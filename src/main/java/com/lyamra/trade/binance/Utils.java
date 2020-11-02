package com.lyamra.trade.binance;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.slf4j.Logger;

import com.lyamra.trade.binance.order.BinanceOrder;

public class Utils {
	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Utils.class);
	
	
/**
 * 
 * @param value
 * @param places
 * @return
 */
	public static String roundDouble2String(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(Double.toString(value));
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.toPlainString();
	}
/**
 * 
 * @param createdDate
 * @return
 */
	public static long getOrderAge(Date createdDate) {
		return new Date().getTime() - createdDate.getTime();
	}
/**
 * 
 * @param symbol
 * @param price
 * @return
 */
	public static String getQuantity(String symbol, String price) {
		String curencyAssets = "";
		if (symbol.toUpperCase().contains("ETH")) {
			curencyAssets = Constant.ETH_ASSETS;
		} else if (symbol.toUpperCase().contains("BNB")) {
			curencyAssets = Constant.BNB_ASSETS;
		} else if (symbol.toUpperCase().contains("BTC")) {
			curencyAssets = Constant.BTC_ASSETS;
		}
		BigDecimal priceBd = new BigDecimal(price);
		BigDecimal curencyQuantityBd = new BigDecimal(curencyAssets);

		return (curencyQuantityBd.divide(priceBd, 2, RoundingMode.CEILING)).toString();
	}
/**
 * 
 * @param msg
 * @param binanceOrder
 * @param e
 */
	public static void alert(String msg, BinanceOrder binanceOrder, Exception e) {
		LOGGER.error("ALERT **************************************************************************");
		
		LOGGER.error(msg + ": Symbol" + binanceOrder.getSymbol() + "- id: " + binanceOrder.getOrderId());
	}

/**
 * 
 * @param quantity
 * @return
 */
	public static String correctedQuantiy(String quantity) {
		String[] quatitySpiltter = quantity.split("\\.");
		String quatityBuilder = quatitySpiltter[0];
		if (quatitySpiltter.length == 1) {
			return quatitySpiltter[0];
		} else {
			String fraction = quatitySpiltter[1].substring(0, quatitySpiltter[1].length() - 1);
			if (fraction.equals(""))
				return quatityBuilder;
			else
				return quatityBuilder + "." + fraction;
		}

	}
public static String min(String price, String price2) {
	
	return Double.parseDouble(price)<Double.parseDouble(price2)?price:price2;
}

}