package io.github.kxng0109.ledgerservice.repository;

import io.github.kxng0109.ledgerservice.entity.TransactionLog;
import io.github.kxng0109.ledgerservice.enums.OperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, UUID> {
	boolean existsByReferenceId(String referenceId);

	Optional<TransactionLog> findByReferenceId(String referenceId);

	Optional<TransactionLog> findByReferenceIdAndOperationType(String referenceId, OperationType operationType);
}
