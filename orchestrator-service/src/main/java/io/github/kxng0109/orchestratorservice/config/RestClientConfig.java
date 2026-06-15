package io.github.kxng0109.orchestratorservice.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring configuration that creates {@link RestClient} beans for communication with
 * external services used by the orchestrator.
 *
 * <p>Two distinct {@code RestClient} instances are defined:</p>
 *
 * <ul>
 *   <li>{@code ledgerClient} – connects to the Ledger service located at
 *   {@code http://localhost:8082/api/v1}. It uses a read timeout of 5 seconds.</li>
 *   <li>{@code providerClient} – connects to the Provider service located at
 *   {@code http://localhost:8081/api/v1}. It uses a read timeout of 12 seconds.</li>
 * </ul>
 *
 * <p>Both clients share the same {@link ObservationRegistry} so that Micrometer
 * observations (metrics, tracing, etc.) are automatically applied to outgoing HTTP
 * requests. The {@link JdkClientHttpRequestFactory} is configured with an
 * appropriate {@link Duration} read timeout for each client.</p>
 *
 * <p>The beans are registered in the Spring application context and can be injected
 * wherever a {@code RestClient} is required.</p>
 */
@Configuration
public class RestClientConfig {

	@Bean
	public RestClient ledgerClient(ObservationRegistry observationRegistry) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
		factory.setReadTimeout(Duration.ofSeconds(5));

		return RestClient.builder()
		                 .baseUrl("http://localhost:8082/api/v1")
		                 .requestFactory(factory)
		                 .observationRegistry(observationRegistry)
		                 .build();
	}

	@Bean
	public RestClient providerClient(ObservationRegistry observationRegistry) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
		factory.setReadTimeout(Duration.ofSeconds(12));

		return RestClient.builder()
		                 .baseUrl("http://localhost:8081/api/v1")
		                 .requestFactory(factory)
		                 .observationRegistry(observationRegistry)
		                 .build();
	}
}
