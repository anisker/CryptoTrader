package com.lyamra.trade.binance.service;

import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.event.AggTradeEvent;
import com.binance.api.client.domain.market.AggTrade;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.exception.BinanceApiException;
import com.lyamra.trade.binance.BinanceServiceException;
import com.lyamra.trade.binance.BuyNotPlacedException;
import com.lyamra.trade.binance.Constant;
import com.lyamra.trade.binance.OrderCanceledException;
import com.lyamra.trade.binance.OrderPartiallyFilledException;
import com.lyamra.trade.binance.TradecUtils;
import com.lyamra.trade.binance.Utils;
import com.lyamra.trade.binance.batch.BatchOrderType;
import com.lyamra.trade.binance.coin.Coin;
import com.lyamra.trade.binance.coin.VolumeType;
import com.lyamra.trade.binance.model.TradeReference;
import com.lyamra.trade.binance.order.BinanceOrder;
import com.lyamra.trade.binance.order.BinanceOrderRepository;

@Service
public class BinanceService {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BinanceService.class);
	private static BinanceApiWebSocketClient clientws;
	private static BinanceApiRestClient clientRest;
	private Map<String, Coin> coinPriceRefs;
	private Map<String, Closeable> OpenWebsockets = new HashMap<>();
	@Autowired
	private BinanceOrderRepository binanceOrderRepository;
	@Autowired
	private TradecUtils tradecUtils;
	
	public void createWebsockets() {
		clientws = tradecUtils.getSocketClient();
		clientRest = tradecUtils.getRestClient();
		List<TickerPrice> symbolList = tradecUtils.getSymbolList();
		coinPriceRefs = tradecUtils.getCoinPriceRefs();
		
		for (TickerPrice tickerPrice : symbolList) {
			String socketName = tickerPrice.getSymbol();
			if(coinPriceRefs.get(tickerPrice.getSymbol()).getVolumeType() != VolumeType.IDLE) {
			Closeable openSocket = coinEngine(socketName);
			LOGGER.info(socketName + ": Started ...");
			OpenWebsockets.put(socketName, openSocket);
		
		}
	
	}
	}
	public void createEmptyWebsocket() {
		List<TickerPrice> symbol = tradecUtils.getSymbolList();
		clientws = tradecUtils.getSocketClient();
		for (TickerPrice tickerPrice : symbol) {
			
			Closeable openSocket = emptyEngine(tickerPrice.getSymbol());
			LOGGER.info(tickerPrice.getSymbol() + ": Started ...");
			
		}
	}
	public Closeable emptyEngine(String socketName) {

		return clientws.onAggTradeEvent(socketName.toLowerCase(), new BinanceApiCallback<AggTradeEvent>() {

			@Override
			public void onResponse(AggTradeEvent response) {
				LOGGER.info(response.getSymbol()+" "+response.getPrice());
				
			}
		});
	}

	public Closeable coinEngine(String socketName) {

		return clientws.onAggTradeEvent(socketName.toLowerCase(), new BinanceApiCallback<AggTradeEvent>() {
			private TradeReference tradeReference = null;
			private Coin coin = coinPriceRefs.get(socketName);

			@Override
			public void onResponse(final AggTradeEvent response) {
				
				double dumpDepth = (tradeReference == null)
						? dumpDepth(response.getSymbol(), coinPriceRefs.get(response.getSymbol()).getAggregate().getPrice(),
								response.getPrice())
						: tradeReference.getDepth();
					
				/**
				 * If the dump is in bitcoin shutdown 
				 */
				if(response.getSymbol().equals("BTCUSDT")&& dumpDepth>Constant.MASTER_CHECK_RATE ) {
					LOGGER.error("Master Check detected !********");
					System.exit(0);
				}

				
				if (dumpDepth < 1.0015) {
					tradeReference = null;
				} else if (dumpDepth > tradecUtils.getOneTimeDump(coin)) {
				
					long endTime = new Date().getTime();
					Integer backLook = (coin.getVolumeType()==VolumeType.LOW)?Constant.NUMBER_OF_MIN_BACK_LOW:Constant.NUMBER_OF_MIN_BACK_MED_HIGH;
					long startTime = new Date(endTime - (backLook * Constant.ONE_MINUTE_IN_MILLIS)).getTime();

					List<AggTrade> aggTrades = clientRest.getAggTrades(response.getSymbol(), null, null, startTime,
							endTime);

					ArrayDeque<AggTrade> aggTradesQueue = new ArrayDeque<>(aggTrades);
					AggTrade ref = tradeReference == null ? coinPriceRefs.get(response.getSymbol()).getAggregate()
							: tradecUtils.getAggTradeFromTradeRef(tradeReference);
					String LPrice = tradeReference == null?response.getPrice(): coinPriceRefs.get(response.getSymbol()).getAggregate().getPrice();
					
					if (isValidDump(aggTradesQueue, ref, LPrice) && ! afterPump(response.getSymbol()) && isValid24HPriceChange()) {
					
						System.out.println("\n");
						
						LOGGER.info(response.getSymbol());

						if (tradeReference != null) {
							LOGGER.info("ZAT ZAT ZAT");
						}
						tradeReference = null;
						LOGGER.info("Reference Price: " + ref.getPrice() + " | Previous Price: "
								+ coinPriceRefs.get(response.getSymbol()).getAggregate().getPrice() + " | Last Price: "
								+ response.getPrice());
						
						String buyPrice = Utils.min(response.getPrice(),coinPriceRefs.get(response.getSymbol()).getAggregate().getPrice());
						
						boolean isPartiallyFilled = false;
						
						try {
							OpenWebsockets.get(socketName).close();
							
							
							if(tradecUtils.isHighest24Price(response.getSymbol())) {
								LOGGER.info("Cycle Cancelled Because of 24h price HIGH");
							}else if(tradecUtils.is24HoursChangeHigh(response.getSymbol())) {
								LOGGER.info("Cycle Cancelled Because of 24h Change HIGH");
							}else {
							NewOrderResponse orderBuyPlaced = placeBuyOrder(response.getSymbol(),buyPrice);
							LOGGER.info("Order buy Placed: " + orderBuyPlaced.getOrderId());
							Order orderBought = isOrderBought(orderBuyPlaced);
							LOGGER.info("Order Bougth: " + orderBought.getOrderId());
							NewOrderResponse orderSellPlaced = placeSellOder(orderBought);
							LOGGER.info("Order Sell Placed: " + orderSellPlaced.getOrderId());
							Order orderSold = isOrderSold(orderSellPlaced);
							LOGGER.info("Order Sold: " + orderSold.getOrderId());
							}
						} catch (OrderPartiallyFilledException e) {
							isPartiallyFilled = true;
							LOGGER.info(e.getOrderId() + ": was partially filled, and will get treated via Batch");
						} catch (OrderCanceledException e) {
							LOGGER.info(e.getOrderId() + ": was Cancelled");
						} catch (Exception e) {

							e.printStackTrace();
						} finally {
							// Re-Open the socket anyway !
							if(! isPartiallyFilled)
							OpenWebsockets.put(socketName, coinEngine(socketName));

						}

					}
					tradeReference = null;
				} else if (tradeReference != null) {
					double depth = dumpDepth(response.getSymbol(), tradeReference.getPrice(), response.getPrice());

					tradeReference
							.setDepth(depth);
					
				} else if (dumpDepth > Constant.MULTIPLE_TIMES_DUMP) {
					tradeReference = new TradeReference();
					String symbol = response.getSymbol();
					tradeReference.setDepth(dumpDepth);
					tradeReference.setSymbol(symbol);
					tradeReference.setTime(coinPriceRefs.get(symbol).getAggregate().getTradeTime());
					tradeReference.setQuantity(coinPriceRefs.get(symbol).getAggregate().getQuantity());
					tradeReference.setPrice(coinPriceRefs.get(symbol).getAggregate().getPrice());
				}
				coin.setAggregate(response);
				coinPriceRefs.put(response.getSymbol(), coin);
			}

	

			@Override
			public void onFailure(final Throwable cause) {
				System.err.println("Web socket failed :" + socketName);
				cause.printStackTrace();
				tradeReference = null;
			}

		});
	}

	/**
	 * 
	 * 
	 * 
	 */

	/**
	 * 
	 * @param response
	 * @return
	 * @throws BuyNotPlacedException
	 */
	private NewOrderResponse placeBuyOrder(String symbol, String buyPrice) throws BuyNotPlacedException {
		LOGGER.info("Placing buy order by the price "+symbol+" by the price "+buyPrice);
		String trigger = buyPrice;
		String limit = Utils.roundDouble2String((Double.parseDouble(trigger) * 1.0002), coinPriceRefs.get(symbol).getTick());
		String quantity = Utils.getQuantity(symbol, buyPrice);
		while (true) {
			try {
				NewOrder order;
				order = (new NewOrder(symbol, OrderSide.BUY, OrderType.TAKE_PROFIT_LIMIT, TimeInForce.GTC,
						quantity, limit)).stopPrice(trigger);
				NewOrderResponse orderBuyPlaced = tradecUtils.getRestClient().newOrder(order);

				
				/**
				 * Prepare the new order to be saved into DB, and get treated by the batch job
				 * later
				 */
				BinanceOrder binanceOrder = new BinanceOrder();
				binanceOrder.setOrderId(orderBuyPlaced.getOrderId());
				binanceOrder.setSymbol(orderBuyPlaced.getSymbol());
				binanceOrder.setBatchOrderType(BatchOrderType.NEW);
				binanceOrder.setCreatedDate(new Date());
				binanceOrderRepository.save(binanceOrder);

				return orderBuyPlaced;

			} catch (Exception e) {
				if (e.getMessage().equals("Order would trigger immediately.")) {
					limit = trigger;
					trigger = Utils.roundDouble2String((Double.parseDouble(trigger) * 0.9999), coinPriceRefs.get(symbol).getTick());

				} else if (e.getMessage().equals("Filter failure: LOT_SIZE")) {
					quantity = Utils.correctedQuantiy(quantity);
				}else {
					throw new BuyNotPlacedException(e);
				}
			}
		}

	}

	/**
	 * 
	 * @param orderPlaced
	 * @return
	 * @throws OrderPartiallyFilledException
	 * @throws OrderCanceledException
	 * @throws BinanceServiceException
	 */
	private Order isOrderBought(NewOrderResponse orderPlaced)
			throws OrderPartiallyFilledException, OrderCanceledException, BinanceServiceException {
		LOGGER.info("Checking if order is bougth");
		while (true) {

			try {
				Order orderStatus = clientRest
						.getOrderStatus(new OrderStatusRequest(orderPlaced.getSymbol(), orderPlaced.getOrderId()));
				if (orderStatus.getStatus() == OrderStatus.FILLED) {
					return orderStatus;
				} else if (orderStatus.getStatus() == OrderStatus.PARTIALLY_FILLED) {
					BinanceOrder binanceOrder = new BinanceOrder();
					binanceOrder.setOrderId(orderStatus.getOrderId());
					binanceOrder.setSymbol(orderStatus.getSymbol());
					binanceOrder.setBatchOrderType(BatchOrderType.PARTIALLY_FILLED);
					binanceOrder.setCreatedDate(new Date());
					binanceOrder.setPrice(orderStatus.getPrice());
					binanceOrderRepository.save(binanceOrder);
					throw new OrderPartiallyFilledException(orderStatus.getOrderId());
				} else if (orderStatus.getStatus() == OrderStatus.CANCELED) {
					throw new OrderCanceledException(orderStatus.getOrderId());
				} else if (orderStatus.getStatus() == OrderStatus.REJECTED
						|| orderStatus.getStatus() == OrderStatus.EXPIRED) {
					throw new BinanceServiceException(orderStatus.getStatus());
				}
			} catch (BinanceApiException e) {
				// If there is any Binance Api Exception we catch it then we keep checking if
				// the order was bought.
				e.printStackTrace();
			} catch (Exception e) {
				throw e;
			}
			

		}

	}

	/**
	 * 
	 * @param orderBought
	 * @return
	 */
	private NewOrderResponse placeSellOder(Order orderBought) {
		LOGGER.info("Placing sell order");
		while (true) {
			try {
				double price = Double.parseDouble(orderBought.getPrice());
				String sellPrice = Utils.roundDouble2String(price * Constant.SELL_PRICE, coinPriceRefs.get(orderBought.getSymbol()).getTick());
				NewOrder order = new NewOrder(orderBought.getSymbol(), OrderSide.SELL, OrderType.LIMIT, TimeInForce.GTC,
						orderBought.getExecutedQty(), sellPrice);
				return tradecUtils.getRestClient().newOrder(order);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				LOGGER.info("RE-Placing sell order");
			}
		}
	}

	/**
	 * 
	 * @param orderSellPlaced
	 * @return
	 * @throws OrderPartiallyFilledException
	 * @throws OrderCanceledException
	 * @throws BinanceServiceException
	 */
	private Order isOrderSold(NewOrderResponse orderSellPlaced) {
		LOGGER.info("Checking if order sold");
		while (true) {
			try {
				Order orderStatus = clientRest.getOrderStatus(
						new OrderStatusRequest(orderSellPlaced.getSymbol(), orderSellPlaced.getOrderId()));
				if (orderStatus.getStatus() == OrderStatus.FILLED) {
					return orderStatus;
				}
				// TODO: if order cancelled okhrej
			} catch (Exception e) {
				e.printStackTrace();
				LOGGER.info("RE-Checking if order sold");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

	}

	/**
	 * 
	 * @param key
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	private double dumpDepth(String key, String oldValue, String newValue) {

		try {

			BigDecimal oldPrice = new BigDecimal(oldValue);
			BigDecimal newPrice = new BigDecimal(newValue);

			return (oldPrice.divide(newPrice, 3, RoundingMode.CEILING).doubleValue());

		} catch (Exception e) {
			System.out.println("still initialisation");
			return 0;
		}

	}

	/**
	 * 
	 * @param aggTradesQueue
	 * @param previousAgg
	 * @param lastPrice
	 * @return
	 */
	private boolean isValidDump(ArrayDeque<AggTrade> aggTradesQueue, AggTrade previousAgg, String lastPrice) {

		boolean startCalculate = false;
		Iterator<AggTrade> it = aggTradesQueue.descendingIterator();
		while (it.hasNext()) {
			AggTrade fromTableAgg = it.next();

			if (!startCalculate && fromTableAgg.getPrice().equals(previousAgg.getPrice())
					&& fromTableAgg.getTradeTime() == previousAgg.getTradeTime() && it.hasNext()) {
				startCalculate = true;
			}
			if (startCalculate) {

				BigDecimal fromTable = new BigDecimal(fromTableAgg.getPrice());
				BigDecimal lPrice = new BigDecimal(lastPrice);
				if (fromTable.divide(lPrice, 3, RoundingMode.CEILING).doubleValue() < 1.006) {
					return false;
				}
			}

		}

		return startCalculate;
	}

	public boolean afterPump(String symbol) {
		
		long endTime = new Date().getTime();
		long startTime = new Date(endTime - (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS)).getTime();

		List<AggTrade> tradesHour1 = tradecUtils.getRestClient().getAggTrades(symbol, null, null, startTime,
				endTime);
		List<AggTrade> tradesHour2 = tradecUtils.getRestClient().getAggTrades(symbol, null, null, startTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS),
				endTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS));
		List<AggTrade> tradesHour3 = tradecUtils.getRestClient().getAggTrades(symbol, null, null, startTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS*2),
				endTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS*2));
		List<AggTrade> tradesHour4 = tradecUtils.getRestClient().getAggTrades(symbol, null, null, startTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS*3),
				endTime- (Constant.NUMBER_OF_MIN_FOR_PUMP * Constant.ONE_MINUTE_IN_MILLIS*3));
		double max=Double.parseDouble(tradesHour1.get(0).getPrice());
		double min=Double.parseDouble(tradesHour1.get(0).getPrice());
		int maxPostion = 0;
		int minPostion = 0;
		long maxTime= 0;
		long minTime =0;
		
		for (int i = 0; i < tradesHour1.size(); i++) {
			double price = Double.parseDouble(tradesHour1.get(i).getPrice()); 
				if(price > max) {
					max = price;
					maxPostion=i;
					maxTime = tradesHour1.get(i).getTradeTime();
				}else if(price < min) {
					min = price;
					minPostion = i;
					minTime = tradesHour1.get(i).getTradeTime();
				}
		}

		LOGGER.info(symbol+" 1ST HOURS MIN: "+String.valueOf(min)+"|"+String.valueOf(minTime)+"- MAX: "+String.valueOf(max)+"|"+String.valueOf(maxTime));
		boolean isPump1stHour = (minPostion<maxPostion) && (max/min>Constant.IS_PUMP_IN_1ST_HOUR)&& (maxTime-minTime>Constant.INTERVAL_BETWEEN_MAXTIME_MINTIME_PUMP);
		if(isPump1stHour) {
			LOGGER.info(symbol+": Dump detected after Pump in 1st Hour. Operation Canclled !  MIN"+min+"|"+minTime+"- MAX"+max+"|"+maxTime);
			return true;
		}
		
		tradesHour4.addAll(tradesHour3);
		tradesHour4.addAll(tradesHour2);
		tradesHour4.addAll(tradesHour1);
		
		max=Double.parseDouble(tradesHour4.get(0).getPrice());
		min=Double.parseDouble(tradesHour4.get(0).getPrice());
		maxPostion = 0;
		minPostion = 0;
		
		for (int i = 0; i < tradesHour4.size(); i++) {
			double price = Double.parseDouble(tradesHour4.get(i).getPrice()); 
			
			 
				if(price > max) {
					max = price;
					maxPostion=i;
					 maxTime= tradesHour4.get(i).getTradeTime();
				}else if(price < min) {
					min = price;
					minPostion = i;
					minTime = tradesHour4.get(i).getTradeTime();
				}
		}

		LOGGER.info(symbol+" 4 HOURS MIN: "+String.valueOf(min)+"|"+String.valueOf(minTime)+"- MAX: "+String.valueOf(max)+"|"+String.valueOf(maxTime));
		boolean isPump4Hour = (minPostion<maxPostion) && (max/min>Constant.IS_PUMP_IN_4_HOUR)&&(maxTime-minTime>Constant.INTERVAL_BETWEEN_MAXTIME_MINTIME_PUMP);
		if(isPump4Hour) {
			LOGGER.info(symbol+": Dump detected after Pump in 4 Hours. Operation Canclled !  MIN"+min+"|"+minTime+"- MAX"+max+"|"+maxTime);
			return true;

		}
		
		
		return false;
	}

	public Map<String, Closeable> getOpenWebsockets() {
		return OpenWebsockets;
	}
	private boolean isValid24HPriceChange() {
		
		return Double.parseDouble(clientRest.get24HrPriceStatistics("GVTBTC").getPriceChangePercent())<Constant.PRICE_24H_CHANGE_PERCENTAGE;
	}
	public boolean stopCoin(String coin) {
		String socketName = coin.toUpperCase(); 
		try {
			OpenWebsockets.get(socketName).close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

}
