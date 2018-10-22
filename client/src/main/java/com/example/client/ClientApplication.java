package com.example.client;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class ClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

	@Bean
	WebClient client(HedgeExchangeFilterFunction eff) {
		return WebClient.builder().filter(eff).build();
	}

}

@Log4j2
@Component
class HedgeExchangeFilterFunction
	implements ExchangeFilterFunction {

	private final DiscoveryClient discoveryClient;
	private final LoadBalancerClient loadBalancerClient;
	private final int attempts, maxAttempts;

	@Autowired
	HedgeExchangeFilterFunction(DiscoveryClient dc, LoadBalancerClient lbc) {
		this(dc, lbc, 3);
	}

	HedgeExchangeFilterFunction(
		DiscoveryClient discoveryClient,
		LoadBalancerClient loadBalancerClient,
		int attempts) {
		this.discoveryClient = discoveryClient;
		this.loadBalancerClient = loadBalancerClient;
		this.attempts = attempts;
		this.maxAttempts = attempts * 2;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

		URI originalURI = request.url();
		String serviceId = originalURI.getHost();
		List<ServiceInstance> serviceInstanceList = this.discoveryClient.getInstances(serviceId);
		Assert.state(serviceInstanceList.size() >= this.attempts, "there must be at least " +
			this.attempts + " instances of the service " + serviceId + "!");
		int counter = 0;
		Map<String, Mono<ClientResponse>> ships = new HashMap<>();
		while (ships.size() < this.attempts && (counter++ < this.maxAttempts)) {
			ServiceInstance lb = this.loadBalancerClient.choose(serviceId);
			String asciiString = lb.getUri().toASCIIString();
			ships.computeIfAbsent(asciiString, str -> this.invoke(lb, originalURI, request, next));
		}
		return Flux
			.first(ships.values())
			.singleOrEmpty();
	}

	private Mono<ClientResponse> invoke(ServiceInstance serviceInstance,
																																					URI originalURI,
																																					ClientRequest request,
																																					ExchangeFunction next) {

		URI uri = this.loadBalancerClient.reconstructURI(serviceInstance, originalURI);
		ClientRequest newRequest = ClientRequest
			.create(request.method(), uri)
			.headers(h -> h.addAll(request.headers()))
			.cookies(c -> c.addAll(request.cookies()))
			.attributes(a -> a.putAll(request.attributes()))
			.body(request.body())
			.build();

		return next
			.exchange(newRequest)
			.doOnNext(cr -> log.info("launching " + newRequest.url()));
	}
}

@RestController
class HedgingRestController {

	private final WebClient client;

	HedgingRestController(WebClient client) {
		this.client = client;
	}

	@GetMapping("/hedge")
	Flux<String> greet() {
		return this.client
			.get()
			.uri("http://service/hi")
			.retrieve()
			.bodyToFlux(String.class);



	}

}