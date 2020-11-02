package com.lyamra.trade.binance;

public class OrderPartiallyFilledException extends Exception {

	private static final long serialVersionUID = 1L;
	private Long orderId;

	public OrderPartiallyFilledException(Long orderId) {
		super();
		this.orderId = orderId;
	}

	@Override
	public String getMessage() {

		return "Orderid :" + orderId + " was partially fielled";
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
	
}
