package com.lyamra.trade.binance;

import com.binance.api.client.domain.OrderStatus;

public class BinanceServiceException extends Exception {

	private OrderStatus status;
	private static final long serialVersionUID = 1L;

	public BinanceServiceException(OrderStatus status) {
		super();
		this.status = status;
	}

	@Override
	public String getMessage() {

		return "Order was: " + status;
	}
}
