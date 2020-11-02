package com.lyamra.trade.binance;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.domain.market.TickerStatistics;
import com.lyamra.trade.binance.coin.Coin;
import com.lyamra.trade.binance.coin.VolumeType;
import com.lyamra.trade.binance.model.TradeReference;
import com.lyamra.trade.binance.order.BinanceOrder;

@Component
public class TradecUtils {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TradecUtils.class);
	private static final String PUBLIC_KEY = "afS5foM2PKRHlXenk2taYqhpV2NWeu6a90SvvRnTo9qJwLdQT8iX7S8Nwjx4teWz";
	//zTMd4wS0QSC2rlkqYr8xZrVuujFKtAJGZWBf0kHMbFwnxiTU6rH4kbIKx1Z8WKyU 3asba
	//afS5foM2PKRHlXenk2taYqhpV2NWeu6a90SvvRnTo9qJwLdQT8iX7S8Nwjx4teWz addad
	private static final String PRIVATE_KEY = "NzEypwS8XdWCC3Eyy8LPRF2YFV7Tdw2EhtJ9GYN9N1nEVoN5ThHIMlgplEi7IG5g";
	//1sq4bZWeAVpsOjzsa4hXmA7FLcUzMI0qQLvsYcgui29ovu660g3ci3xFfFPGpIL0
	//NzEypwS8XdWCC3Eyy8LPRF2YFV7Tdw2EhtJ9GYN9N1nEVoN5ThHIMlgplEi7IG5g
	private static final double MEDIUM_BTC_VOLUME_TRESHOLD = 2000;
	private static final double LOW_BTC_VOLUME_TRESHOLD = 1000;
	private static final double MEDIUM_ETH_VOLUME_TRESHOLD = 5000;
	private static final double LOW_ETH_VOLUME_TRESHOLD = 2000;
	private static final double MEDIUM_BNB_VOLUME_TRESHOLD = 300000;
	private static final double LOW_BNB_VOLUME_TRESHOLD = 200000;
	private static BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(PUBLIC_KEY, PRIVATE_KEY);
	private static BinanceApiRestClient clientRest = factory.newRestClient();
	private static BinanceApiWebSocketClient clientWS = BinanceApiClientFactory.newInstance().newWebSocketClient();
	public static String listenKey = clientRest.startUserDataStream();

	public BinanceApiRestClient getRestClient() {
		return clientRest;
	}

	public BinanceApiWebSocketClient getSocketClient() {
		return clientWS;
	}

	public List<TickerPrice> getSymbolList() {
		
/*		List<TickerPrice> list = new ArrayList<>();
		List<TickerPrice> tikers = clientRest.getAllPrices();
		for (TickerPrice tickerPrice : tikers) {
			if(tickerPrice.getSymbol().equals("XRPBTC")) {
			list.add(tickerPrice);
			}
		}
*/		return clientRest.getAllPrices();
	}

	public Map<String, Coin> getCoinPriceRefs() {
		Map<String, Coin> coinPriceRefs = new HashMap<String, Coin>();
		List<TickerPrice> priceRefs = getSymbolList();
		for (TickerPrice tickerPrice : priceRefs) {

			AggTrade aggTrade = new AggTrade();
			aggTrade.setPrice(tickerPrice.getPrice());
			aggTrade.setTradeTime(0);
			Coin coin = new Coin();
			coin.setAggregate(aggTrade);
			coin.setSymbol(tickerPrice.getSymbol());
			coin.setVolumeType(getCoinVolumeType(tickerPrice.getSymbol()));
			coin.setTick(getSymbolTick(tickerPrice.getSymbol()));
			LOGGER.info("INIT SYMBOL: " + coin.getSymbol() + " " + coin.getVolumeType());
			coinPriceRefs.put(tickerPrice.getSymbol(), coin);

		}
		return coinPriceRefs;
	}

	private VolumeType getCoinVolumeType(String symbol) {
		TickerStatistics stats = clientRest.get24HrPriceStatistics(symbol);
		double volume = Double.parseDouble(stats.getVolume()) * Double.parseDouble(stats.getLastPrice());
		double mediumValume = 0;
		double lowValume = 0;
		if (symbol.toUpperCase().contains("BTC")) {
			mediumValume = MEDIUM_BTC_VOLUME_TRESHOLD;
			lowValume = LOW_BTC_VOLUME_TRESHOLD;
		} else if (symbol.toUpperCase().contains("ETH")) {
			mediumValume = MEDIUM_ETH_VOLUME_TRESHOLD;
			lowValume = LOW_ETH_VOLUME_TRESHOLD;
		} else {
			mediumValume = MEDIUM_BNB_VOLUME_TRESHOLD;
			lowValume = LOW_BNB_VOLUME_TRESHOLD;
		}
		if (volume > mediumValume)
			return VolumeType.HIGH;
		if (volume > lowValume)
			return VolumeType.MEDIUM;
		return VolumeType.LOW;
	}

	public void cancelOrder(BinanceOrder order) {
		clientRest.cancelOrder(new CancelOrderRequest(order.getSymbol(), order.getOrderId()));

	}

	public OrderStatus getOrderStatus(BinanceOrder binanceOrder) {
		Order order = clientRest
				.getOrderStatus(new OrderStatusRequest(binanceOrder.getSymbol(), binanceOrder.getOrderId()));
		return order.getStatus();
	}

	public boolean is24HoursChangeHigh(String symbol) {
		return Double.parseDouble(getRestClient().get24HrPriceStatistics(symbol)
				.getPriceChangePercent()) > Constant.PRICE_24H_CHANGE_PERCENTAGE;
	}

	public boolean isHighest24Price(String symbol) {
		String highest24Price = clientRest.get24HrPriceStatistics(symbol).getHighPrice();
		long endTime = new Date().getTime();
		long startTime = new Date(endTime - (Constant.Highest24HPriceBack * Constant.ONE_MINUTE_IN_MILLIS)).getTime();

		List<AggTrade> aggTrades = clientRest.getAggTrades(symbol, null, null, startTime, endTime);
		List<AggTrade> aggTrades2endHour = clientRest.getAggTrades(symbol, null, null, startTime- (Constant.Highest24HPriceBack * Constant.ONE_MINUTE_IN_MILLIS), endTime- (Constant.Highest24HPriceBack * Constant.ONE_MINUTE_IN_MILLIS));
		aggTrades.addAll(aggTrades2endHour);
		for (AggTrade aggTrade : aggTrades) {
			if (aggTrade.getPrice().equals(highest24Price)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * @param symbol
	 * 
	 * @return index - 1. the price is 0.0... then we need to substruct -2, and
	 *         string starts from 0, we need to add +1. Then -2+1 = -1.
	 */
	private int getSymbolTick(String symbol) {
		String tick = clientRest.getExchangeInfo().getSymbolInfo(symbol).getSymbolFilter(FilterType.PRICE_FILTER)
				.getTickSize();
		return tick.indexOf("1") - 1;
	}

	public AggTrade getAggTradeFromTradeRef(TradeReference tradeReference) {
		AggTrade agg = new AggTrade();
		agg.setPrice(tradeReference.getPrice());
		agg.setQuantity(tradeReference.getQuantity());
		agg.setTradeTime(tradeReference.getTime());
		return agg;
	}

	public double getOneTimeDump(Coin coin) {
		if (coin.getVolumeType() == VolumeType.HIGH)
			return Constant.ONE_TIME_HIGH_DUMP;
		else if (coin.getVolumeType() == VolumeType.MEDIUM)
			return Constant.ONE_TIME_MEDIUM_DUMP;
		else if (coin.getVolumeType() == VolumeType.LOW)
			return Constant.ONE_TIME_LOW_DUMP;
		else
			return Constant.ONE_TIME_IDLE_DUMP;

	}

	public int getTick(String symbol) {

		return getSymbolTick(symbol);
	}

}
