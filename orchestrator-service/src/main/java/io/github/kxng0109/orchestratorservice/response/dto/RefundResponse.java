package io.github.kxng0109.orchestratorservice.response.dto;

import lombok.Builder;

@Builder
public record RefundResponse(
		String status,
		String referenceId,
		String userId
) {
}
