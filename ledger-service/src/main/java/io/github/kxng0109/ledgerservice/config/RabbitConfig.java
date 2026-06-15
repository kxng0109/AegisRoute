package io.github.kxng0109.ledgerservice.config;

import io.github.kxng0109.ledgerservice.request.dto.CreditRequest;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring configuration that defines beans used for RabbitMQ message handling.
 * <p>
 * The primary purpose of this configuration is to provide a {@link MessageConverter}
 * capable of converting objects to and from JSON when communicating with RabbitMQ.
 * The converter is configured with a {@link DefaultJacksonJavaTypeMapper} that
 * trusts all packages and establishes an explicit type mapping for the
 * {@code CreditRequest} class. This ensures that inbound messages containing a
 * type identifier for {@code CreditRequest} are correctly deserialized.
 */
@Configuration
public class RabbitConfig {

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
}