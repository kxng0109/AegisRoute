package io.github.kxng0109.orchestratorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

	@Bean
	public RestClient ledgerClient(){
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
		factory.setReadTimeout(Duration.ofSeconds(5));

		return RestClient.builder()
		                 .baseUrl("http://localhost:8082/api/v1")
		                 .requestFactory(factory)
		                 .build();
	}

	@Bean
	public RestClient providerClient() {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
		factory.setReadTimeout(Duration.ofSeconds(12));

		return RestClient.builder()
		                 .baseUrl("http://localhost:8081/api/v1")
		                 .requestFactory(factory)
		                 .build();
	}
}
