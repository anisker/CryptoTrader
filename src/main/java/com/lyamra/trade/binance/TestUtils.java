package com.lyamra.trade.binance;



import com.binance.api.client.domain.event.AggTradeEvent;

/**
 * 
 * @author tareksahalia
 *
 */
public class TestUtils {
	
/**
 * 
 * @param response
 * 
 * One Time test if the client socket queue the responses or not.
 * TEST RESULTS: yes it does. 
 */
	public static void slowDownSocket(AggTradeEvent response) {
		try {
			
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
