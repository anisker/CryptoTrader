package com.lyamra.trade.binance.order;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.lyamra.trade.binance.batch.BatchOrderType;

@Entity
public class BinanceOrder {
	@Id
	private Long orderId;
	private String symbol;
	private BatchOrderType batchOrderType;
	private Date createdDate;

	private String price;

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public BatchOrderType getBatchOrderType() {
		return batchOrderType;
	}

	public void setBatchOrderType(BatchOrderType batchOrderType) {
		this.batchOrderType = batchOrderType;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;

	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

}
