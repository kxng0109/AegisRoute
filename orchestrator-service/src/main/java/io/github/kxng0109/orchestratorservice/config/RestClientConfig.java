package io.github.kxng0109.orchestratorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring configuration class that defines {@link RestClient} beans for communicating
 * with external services.
 *
 * <p>This configuration creates two distinct {@code RestClient} instances, each
 * pre‑configured with a base URL and a custom {@link JdkClientHttpRequestFactory}
 * that sets an appropriate read timeout. The beans can be injected wherever a
 * {@code RestClient} is required to interact with the corresponding service.</p>
 *
 * <ul>
 *   <li>{@code ledgerClient()} – targets the Ledger service located at
 *   {@code http://localhost:8082/api/v1} with a read timeout of 5 seconds.</li>
 *   <li>{@code providerClient()} – targets the Provider service located at
 *   {@code http://localhost:8081/api/v1} with a read timeout of 12 seconds.</li>
 * </ul>
 *
 * <p>Both clients are built using {@link RestClient#builder()} and share the same
 * request factory implementation, differing only by their timeout configuration
 * and base URL. This separation allows fine‑grained control over the connection
 * characteristics for each external endpoint.</p>
 *
 * <p>The class is annotated with {@link Configuration} so that Spring's
 * component scanning registers the beans automatically during application startup.</p>
 */
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
