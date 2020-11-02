package com.lyamra.trade.binance.endpoint.restcontroller;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lyamra.trade.binance.TradecUtils;
import com.lyamra.trade.binance.service.BinanceService;

@RestController
public class EndPoint {

	private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BinanceService.class);
	@Autowired
	private TradecUtils tradecUtils;
	private final BinanceService binanceService;

	@Autowired
	public EndPoint(BinanceService binanceService) {
		super();
		this.binanceService = binanceService;
	}

	@GetMapping("/startws")
	public String startws() throws InterruptedException {

		binanceService.createWebsockets();
		return "All websockets were opened successfully ..";

	}

	@PostMapping("/stop/{coin}")
	public String stopCoin(@PathVariable String coin) throws InterruptedException {
		return binanceService.stopCoin(coin)?coin + " stopped!!":"an exception occured !";
	}

	@GetMapping("/test")
	public String test() {

		return "tarek";
		// System.out.println(Double.parseDouble(tradecUtils.getRestClient().get24HrPriceStatistics("TRIGETH").getPriceChangePercent())>-20);
		//binanceService.createEmptyWebsocket();
	}

}