package com.lyamra.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackageClasses = {TradeCurrencyApplication.class})
@EnableScheduling
public class TradeCurrencyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeCurrencyApplication.class, args);
	}
}
