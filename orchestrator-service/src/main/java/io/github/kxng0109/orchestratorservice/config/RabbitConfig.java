package io.github.kxng0109.orchestratorservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that declares the RabbitMQ infrastructure needed by the
 * application.
 *
 * <p>This configuration defines two logical messaging channels:</p>
 *
 * <ul>
 *   <li>A <b>credit</b> channel consisting of a queue, a topic exchange and a
 *   binding that uses the {@code credit.routing.key} routing key.</li>
 *   <li>A <b>refund response</b> channel consisting of a separate queue, a topic
 *   exchange and a binding that uses the
 *   {@code refund.response.routing.key} routing key.</li>
 * </ul>
 *
 * <p>Each channel is isolated by its own exchange and queue names, allowing
 * producers and consumers to target the appropriate workflow without interference.</p>
 *
 * <p>In addition, a {@link MessageConverter}
 * bean is provided that serializes messages to JSON using Jackson. This converter is
 * automatically applied to all {@link org.springframework.amqp.rabbit.core.RabbitTemplate}
 * instances in the Spring context.</p>
 * <p>
 * The class is annotated with {@link Configuration},
 * so Spring automatically registers all {@code @Bean} definitions during startup.
 */
@Configuration
public class RabbitConfig {
	public final String CREDIT_QUEUE_NAME = "credit.queue";
	public final String CREDIT_EXCHANGE_NAME = "credit.exchange";
	public final String CREDIT_ROUTING_KEY = "credit.routing.key";
	public final String REFUND_RESPONSE_QUEUE_NAME = "refund.response.queue";
	public final String REFUND_RESPONSE_EXCHANGE_NAME = "refund.response.exchange";
	public final String REFUND_RESPONSE_ROUTING_KEY = "refund.response.routing.key";

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
	public MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter();
	}
}
