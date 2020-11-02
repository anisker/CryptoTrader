package com.lyamra.trade.binance;

public class OrderCanceledException extends Exception {

	private Long orderId;

	private static final long serialVersionUID = 1L;

	public OrderCanceledException(Long orderId) {
		super();
		this.orderId = orderId;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}



}
