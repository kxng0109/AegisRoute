package io.github.kxng0109.orchestratorservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} that declares all RabbitMQ topology components required
 * by the orchestrator service.
 *
 * <p>The configuration creates three logical groups of queues, exchanges and bindings,
 * each representing a distinct messaging concern:</p>
 *
 * <ul>
 *   <li><strong>Credit</strong> – handles requests for credit processing.</li>
 *   <li><strong>Refund response</strong> – receives asynchronous responses from the
 *   refund subsystem.</li>
 *   <li><strong>Timeout refund response</strong> – captures refund responses that have
 *   exceeded a configured processing window.</li>
 * </ul>
 *
 * <p>All queues are created with default durability (non‑exclusive, non‑auto‑delete) so
 * that they survive broker restarts. Each queue is bound to a {@link TopicExchange}
 * using a dedicated routing key, allowing fine‑grained routing based on topic patterns.</p>
 *
 * <p>In addition to the topology beans, a JSON {@link MessageConverter} and a
 * {@link SimpleRabbitListenerContainerFactory} are provided. The factory is tuned
 * for high‑throughput consumption (20–50 concurrent consumers, prefetch of 20) and
 * registers the JSON converter so that listener methods receive deserialized POJOs.</p>
 *
 * <p>This configuration is deliberately side‑effect free: bean creation does not
 * perform any network I/O; topology declaration is delegated to Spring AMQP at
 * application startup. The returned beans are thread‑safe and may be shared across
 * the application context.</p>
 *
 * @see Queue
 * @see TopicExchange
 * @see Binding
 */
@Configuration
public class RabbitConfig {
	public final String CREDIT_QUEUE_NAME = "credit.queue";
	public final String CREDIT_EXCHANGE_NAME = "credit.exchange";
	public final String CREDIT_ROUTING_KEY = "credit.routing.key";
	public final String REFUND_RESPONSE_QUEUE_NAME = "refund.response.queue";
	public final String REFUND_RESPONSE_EXCHANGE_NAME = "refund.response.exchange";
	public final String REFUND_RESPONSE_ROUTING_KEY = "refund.response.routing.key";
	public final String TIMEOUT_REFUND_RESPONSE_QUEUE_NAME = "timeout.refund.response.queue";
	public final String TIMEOUT_REFUND_RESPONSE_EXCHANGE_NAME = "timeout.refund.response.exchange";
	public final String TIMEOUT_REFUND_RESPONSE_ROUTING_KEY = "timeout.refund.response.routing.key";

	@Bean
	public Queue creditQueue() {
		return new Queue(CREDIT_QUEUE_NAME);
	}

	@Bean
	public TopicExchange creditExchange() {
		return new TopicExchange(CREDIT_EXCHANGE_NAME);
	}

	@Bean
	public Binding creditBinding(Queue creditQueue, TopicExchange creditExchange) {
		return BindingBuilder.bind(creditQueue).to(creditExchange).with(CREDIT_ROUTING_KEY);
	}

	@Bean
	public Queue refundResponseQueue() {
		return new Queue(REFUND_RESPONSE_QUEUE_NAME);
	}

	@Bean
	public TopicExchange refundResponseExchange() {
		return new TopicExchange(REFUND_RESPONSE_EXCHANGE_NAME);
	}

	@Bean
	public Binding refundResponseBinding(Queue refundResponseQueue, TopicExchange refundResponseExchange) {
		return BindingBuilder.bind(refundResponseQueue).to(refundResponseExchange).with(REFUND_RESPONSE_ROUTING_KEY);
	}

	@Bean
	public Queue timeoutRefundResponseQueue() {
		return new Queue(TIMEOUT_REFUND_RESPONSE_QUEUE_NAME);
	}

	@Bean
	public TopicExchange timeoutRefundResponseExchange() {
		return new TopicExchange(TIMEOUT_REFUND_RESPONSE_EXCHANGE_NAME);
	}

	@Bean
	public Binding timeoutRefundResponseBinding(
			Queue timeoutRefundResponseQueue,
			TopicExchange timeoutRefundResponseExchange
	) {
		return BindingBuilder.bind(timeoutRefundResponseQueue)
		                     .to(timeoutRefundResponseExchange)
		                     .with(TIMEOUT_REFUND_RESPONSE_ROUTING_KEY);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}

	/**
	 * Creates a {@link SimpleRabbitListenerContainerFactory} pre‑configured for the
	 * application’s RabbitMQ listeners.
	 *
	 * <p>The factory is first configured using the supplied {@code configurer},
	 * which applies the default Spring Boot Rabbit settings (e.g., automatic
	 * declaration of {@link ConnectionFactory} properties). Afterward, explicit
	 * concurrency and prefetch values are set to optimise throughput for the
	 * orchestrator’s expected load.</p>
	 *
	 * <ul>
	 *   <li>Sets the minimum number of concurrent consumers to {@code 20}.</li>
	 *   <li>Allows the pool to grow up to {@code 50} concurrent consumers under load.</li>
	 *   <li>Configures a prefetch count of {@code 20} messages per consumer to
	 *       balance latency and network utilization.</li>
	 *   <li>Registers the provided {@code jsonMessageConverter} so that listener
	 *       methods receive payloads deserialized from JSON.</li>
	 * </ul>
	 *
	 * <p>The returned factory is thread‑safe and can be shared across multiple
	 * {@link org.springframework.amqp.rabbit.annotation.RabbitListener} definitions.
	 * No transactional boundaries are introduced by this method; any transaction
	 * management must be applied at the listener level.</p>
	 *
	 * @param configurer           the {@link SimpleRabbitListenerContainerFactoryConfigurer}
	 *                             that supplies the baseline RabbitMQ configuration; must be non‑null.
	 * @param connectionFactory    the {@link ConnectionFactory} used to establish AMQP
	 *                             connections; must be non‑null.
	 * @param jsonMessageConverter the {@link MessageConverter} responsible for
	 *                             converting inbound JSON messages; must be non‑null.
	 * @return a fully configured {@link SimpleRabbitListenerContainerFactory} ready
	 * for injection into the Spring context.
	 * @see SimpleRabbitListenerContainerFactoryConfigurer
	 * @see ConnectionFactory
	 * @see MessageConverter
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
