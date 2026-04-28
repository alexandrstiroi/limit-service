package org.shtiroy.module1.hm08.repository;

import jakarta.persistence.LockModeType;
import org.shtiroy.module1.hm08.entity.UserLimitAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserLimitAccountRepository extends JpaRepository<UserLimitAccount, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserLimitAccount account where account.userId = :userId")
    Optional<UserLimitAccount> findByUserIdForUpdate(@Param("userId") String userId);

    @Modifying
    @Query(value = """
            update user_limit_account set
                available_amount = greatest(cast(:defaultLimit as nueric(19,2)) - reserved_amount, 0),
                    last_reset_date = :businessDate,
                    updated_at = :updatedAt
            where last_reset_date is null or last_reset_date < :businessDate        
            """, nativeQuery = true)
    int resetAllToDefault(
            @Param("defaultLimit") BigDecimal defaultLimit,
            @Param("businessDate") LocalDate businessDate,
            @Param("updatedAt") LocalDateTime updatedAt);
}
