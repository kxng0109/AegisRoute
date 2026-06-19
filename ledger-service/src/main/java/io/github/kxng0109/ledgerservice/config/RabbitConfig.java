package io.github.kxng0109.ledgerservice.config;

import io.github.kxng0109.ledgerservice.request.dto.CreditRequest;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration that defines beans required for RabbitMQ messaging.
 *
 * <p>This configuration provides:
 * <ul>
 *   <li>a {@link MessageConverter} that serializes and deserializes messages as JSON,
 *       configured with a trusted package wildcard and a custom type‑id mapping for
 *       {@code CreditRequest} objects;</li>
 *   <li>a {@link SimpleRabbitListenerContainerFactory} that creates listener containers
 *       with tuned concurrency and prefetch settings.</li>
 * </ul>
 * <p>
 * The beans are automatically detected by Spring when the application context is
 * initialized because the class is annotated with {@code @Configuration}.
 */
@Configuration
public class RabbitConfig {
	public final String CREDIT_EXCHANGE_NAME = "credit.exchange";
	public final String CREDIT_ROUTING_KEY = "credit.routing.key";
	public final String REFUND_RESPONSE_EXCHANGE_NAME = "refund.response.exchange";
	public final String REFUND_RESPONSE_ROUTING_KEY = "refund.response.routing.key";

	@Bean
	public MessageConverter jsonMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();

		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		typeMapper.setTrustedPackages("*");

		Map<String, Class<?>> idClassMapping = new HashMap<>();
		idClassMapping.put(
				"io.github.kxng0109.orchestratorservice.service.SagaOrchestratorService$CreditRequest",
				CreditRequest.class
		);
		typeMapper.setIdClassMapping(idClassMapping);

		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}

	/**
	 * Creates a {@link SimpleRabbitListenerContainerFactory} pre‑configured for the
	 * application's RabbitMQ listeners.
	 *
	 * <p>The returned factory is initialized with the supplied {@link ConnectionFactory}
	 * and then tuned with concurrency and prefetch settings that match the expected
	 * load of credit‑related messages.  By injecting the {@code jsonMessageConverter},
	 * all listener containers produced by this factory will automatically convert
	 * inbound messages to and from JSON, applying the type‑id mappings defined in
	 * {@link #jsonMessageConverter()}.</p>
	 *
	 * <p>Typical usage is to let Spring inject this bean into {@code @RabbitListener}
	 * definitions, allowing the framework to create and manage listener containers
	 * without further manual configuration.</p>
	 *
	 * @param configurer           the Spring Boot auto‑configuration helper that
	 *                             applies default settings (e.g., transaction
	 *                             management, acknowledge mode) to the factory;
	 *                             must not be {@code null}
	 * @param connectionFactory    the low‑level RabbitMQ {@link ConnectionFactory}
	 *                             that supplies physical connections; must not be
	 *                             {@code null}
	 * @param jsonMessageConverter the {@link MessageConverter} used to deserialize
	 *                             inbound JSON payloads and serialize outbound
	 *                             messages; must not be {@code null}
	 * @return a fully configured {@link SimpleRabbitListenerContainerFactory}
	 *         ready to be used by {@code @RabbitListener} containers
	 *
	 * @see SimpleRabbitListenerContainerFactoryConfigurer
	 * @see org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry
	 */
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
			SimpleRabbitListenerContainerFactoryConfigurer configurer,
			ConnectionFactory connectionFactory,
			MessageConverter jsonMessageConverter
	) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		configurer.configure(factory, connectionFactory);

		factory.setConcurrentConsumers(20);
		factory.setMaxConcurrentConsumers(50);
		factory.setPrefetchCount(20);
		factory.setMessageConverter(jsonMessageConverter);

		return factory;
	}
}