package com.lyamra.trade.binance.coin;

import com.binance.api.client.domain.market.AggTrade;

public class Coin {
	private String symbol;
	private AggTrade aggregate;
	private VolumeType volumeType;
	private int tick;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public AggTrade getAggregate() {
		return aggregate;
	}

	public void setAggregate(AggTrade aggregate) {
		this.aggregate = aggregate;
	}

	public VolumeType getVolumeType() {
		return volumeType;
	}

	public void setVolumeType(VolumeType volumeType) {
		this.volumeType = volumeType;
	}

	public int getTick() {
		return tick;
	}

	public void setTick(int tick) {
		this.tick = tick;
	}

}
