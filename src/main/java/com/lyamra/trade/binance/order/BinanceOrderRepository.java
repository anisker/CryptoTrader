package com.lyamra.trade.binance.order;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BinanceOrderRepository extends JpaRepository<BinanceOrder, Long> {

	BinanceOrder findByOrderId(Long orderId);

}
