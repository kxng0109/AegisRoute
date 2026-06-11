package io.github.kxng0109.ledgerservice.repository;

import io.github.kxng0109.ledgerservice.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, UUID> {
	boolean existsByReferenceId(String referenceId);
}
