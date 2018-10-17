package com.example.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Log4j2
public class ServiceApplication {

	@GetMapping("/hi")
	String greet() throws Exception {
		log.info("receiving #greet()");
		long delay = Math.max(
			(long) (Math.random() * (1000 * 5)), (long) (Math.random() * (1000 * 5)));
		Thread.sleep(delay);
		String msg = "Hello after " + delay + "ms.";
		log.info("message: " + msg);
		return msg;
	}

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}
}
