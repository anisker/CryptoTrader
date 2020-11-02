package com.lyamra.trade.binance.batch;

import java.io.Closeable;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.domain.account.NewOrder;
import com.lyamra.trade.binance.Constant;
import com.lyamra.trade.binance.TradecUtils;
import com.lyamra.trade.binance.Utils;
import com.lyamra.trade.binance.order.BinanceOrder;
import com.lyamra.trade.binance.order.BinanceOrderRepository;
import com.lyamra.trade.binance.service.BinanceService;

@Component
public class OrderBatchCanceler {

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderBatchCanceler.class);
	private BinanceOrderRepository binanceOrderRepository;
	private TradecUtils tradecUtils;
	private BinanceService binanceService;
	@Autowired
	public OrderBatchCanceler(BinanceOrderRepository binanceOrderRepository, TradecUtils tradecUtils,BinanceService binanceService) {
		super();
		this.binanceOrderRepository = binanceOrderRepository;
		this.tradecUtils = tradecUtils;
		this.binanceService = binanceService;
	}

	@Scheduled(fixedDelay = 10000)
	public void process() {
		List<BinanceOrder> orders = binanceOrderRepository.findAll();
		for (BinanceOrder binanceOrder : orders) {
			if (binanceOrder.getBatchOrderType() == BatchOrderType.NEW
					&& Utils.getOrderAge(binanceOrder.getCreatedDate()) > 240000) {
				if (tradecUtils.getOrderStatus(binanceOrder) == OrderStatus.NEW) {
					try {
						tradecUtils.cancelOrder(binanceOrder);
					} catch (Exception e) {
						// Theoretically can't happen,
						// except if the database crashes, Or order filled while canceling it.
					}
				}
				binanceOrderRepository.delete(binanceOrder);

			} else if (binanceOrder.getBatchOrderType() == BatchOrderType.PARTIALLY_FILLED
					&& Utils.getOrderAge(binanceOrder.getCreatedDate()) > 10000) {
				OrderStatus orderStatus = tradecUtils.getOrderStatus(binanceOrder);
				if (orderStatus == OrderStatus.PARTIALLY_FILLED || orderStatus == OrderStatus.FILLED) {
					LOGGER.info("PARTIALLY FILLED !");
					try {
						tradecUtils.cancelOrder(binanceOrder);
					} catch (Exception e) {
						// If Order filled.
					}
					while (true) {

						try {
							double price = Double.parseDouble(binanceOrder.getPrice());
							String sellPrice = Utils.roundDouble2String(price * Constant.SELL_PRICE, tradecUtils.getTick(binanceOrder.getSymbol()));
							String symbol = binanceOrder.getSymbol().toUpperCase().substring(0, binanceOrder.getSymbol().length()-3);
							AssetBalance balance = tradecUtils.getRestClient().getAccount().getAssetBalance(symbol);
							NewOrder sellOrder = new NewOrder(binanceOrder.getSymbol(), OrderSide.SELL, OrderType.LIMIT,
									TimeInForce.GTC, balance.getFree(), sellPrice);
							tradecUtils.getRestClient().newOrder(sellOrder);
							break;
						} catch (Exception e) {
							if (e.getMessage().equals("Filter failure: MIN_NOTIONAL")) {
								Utils.alert("Filter failure: MIN_NOTIONAL", binanceOrder, e);
								
							}
							e.printStackTrace();
							break;
						}
					}
					LOGGER.info("opening socket "+binanceOrder.getSymbol()+"From Batch");
					Closeable openSocket =  binanceService.coinEngine(binanceOrder.getSymbol());
					binanceService.getOpenWebsockets().put(binanceOrder.getSymbol(),openSocket);
				}
				binanceOrderRepository.delete(binanceOrder);
			}
		}
	}
	
}