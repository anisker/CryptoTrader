package com.lyamra.trade.binance;

public class Constant {
	public static final long ONE_MINUTE_IN_MILLIS = 60000;
	public static final double ONE_TIME_HIGH_DUMP = 1.007;
	public static final double ONE_TIME_MEDIUM_DUMP = 1.012;
	public static final double ONE_TIME_LOW_DUMP = 1.020;
	public static final double ONE_TIME_IDLE_DUMP = 1000;// will never match
	public static final double MULTIPLE_TIMES_DUMP = 1.003;
	public static final Integer NUMBER_OF_MIN_BACK_MED_HIGH = 5;
	public static final Integer NUMBER_OF_MIN_BACK_LOW = 30;
	public static final String ETH_ASSETS = "0.3000000";
	public static final String BNB_ASSETS = "10";
	public static final String BTC_ASSETS = "0.02500000";
	public static final int Highest24HPriceBack = 60;
	public static final long NUMBER_OF_MIN_FOR_PUMP = 60;
	public static final double PRICE_24H_CHANGE_PERCENTAGE = 20;
	public static final double SELL_PRICE = 1.008;
	public static final double MASTER_CHECK_RATE = 1.006;
	public static final Object BTC_USDT = "BTCUSDT";
	public static final double MASTER_DEPTH = 1.006;
	public static final double IS_PUMP_IN_1ST_HOUR = 1.02;
	public static final double IS_PUMP_IN_4_HOUR = 1.04;
	public static final long INTERVAL_BETWEEN_MAXTIME_MINTIME_PUMP = ONE_MINUTE_IN_MILLIS*10;
}

