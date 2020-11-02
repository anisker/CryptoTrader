package com.lyamra.trade.binance;

public class BuyNotPlacedException extends Exception {

	private Exception e;

	public BuyNotPlacedException(Exception e) {
		super();
		this.e = e;
	}
	
	@Override
	public synchronized Throwable getCause() {
		// TODO Auto-generated method stub
		return e;
	}
	
	
}
