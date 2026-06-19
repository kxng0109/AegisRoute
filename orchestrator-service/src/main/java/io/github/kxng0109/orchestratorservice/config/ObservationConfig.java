package io.github.kxng0109.orchestratorservice.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Micrometer observation support for the Orchestrator Service.
 *
 * <p>Spring Boot does not create an {@link ObservedAspect} automatically; without this
 * aspect, {@code @Observed} annotations placed on service methods would be ignored and no
 * metrics or tracing data would be emitted. Declaring this configuration registers the
 * aspect as a Spring bean bound to the supplied {@link ObservationRegistry}, enabling
 * automatic instrumentation of annotated methods throughout the application context.
 *
 * <p>The produced {@link ObservedAspect} is thread‑safe and can be shared across all
 * request‑handling threads. It does not introduce transactional boundaries or I/O;
 * its only responsibility is to start, stop, and propagate {@link io.micrometer.observation.Observation}
 * instances.
 *
 * @see ObservationRegistry
 * @see io.micrometer.observation.annotation.Observed
 */
@Configuration
public class ObservationConfig {
	@Bean
	public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
		return new ObservedAspect(observationRegistry);
	}
}
