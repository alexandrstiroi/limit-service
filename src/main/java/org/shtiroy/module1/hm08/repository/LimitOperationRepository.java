package org.shtiroy.module1.hm08.repository;

import jakarta.persistence.LockModeType;
import org.shtiroy.module1.hm08.entity.LimitOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LimitOperationRepository extends JpaRepository<LimitOperation, UUID> {

    Optional<LimitOperation> findByOperationId(String operationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select operation from LimitOperation operation where operation.operationId = :operationId")
    Optional<LimitOperation> findByOperationIdForUpdate(@Param("operationId") String operationId);
}
