package com.example.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@Log4j2
@RestController
@SpringBootApplication
public class ServiceApplication {

	private long random(int seconds) {
		return new Random().nextInt(seconds) * 1000;
	}

	@GetMapping("/hi")
	String greet() throws InterruptedException {
		long delay = Math.max(random(5), random(5));
		Thread.sleep(delay);
		final String msg = "Hello, afte" +
			"r " + delay + " ms.";
		log.info("returning (" + msg + ")");
		return msg;
	}


	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}
}
