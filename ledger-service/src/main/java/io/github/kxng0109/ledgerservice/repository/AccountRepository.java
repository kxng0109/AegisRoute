package io.github.kxng0109.ledgerservice.repository;

import io.github.kxng0109.ledgerservice.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.QueryHint;

import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.userId = :userId")
    /*@QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "0") // NOWAIT - fail immediately if locked
    })*/
    Optional<Account> findByUserIdForUpdate(@Param("userId") String userId);

    Optional<Account> findByUserId(String userId);
}
