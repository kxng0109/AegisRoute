package io.github.kxng0109.orchestratorservice.repository;

import io.github.kxng0109.orchestratorservice.entity.TransferSagaStates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferSagaStatesRepository extends JpaRepository<TransferSagaStates, UUID> {
	Optional<TransferSagaStates> findByLedgerReferenceIdAndUserId(String ledgerReferenceId, String userId);
}
