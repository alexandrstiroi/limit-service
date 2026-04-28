package org.shtiroy.module1.hm08.dto;

import org.shtiroy.module1.hm08.entity.OperationStatus;
import org.shtiroy.module1.hm08.entity.OperationType;

import java.math.BigDecimal;

public record OperationResponse(String operationId,
                                String userId,
                                BigDecimal amount,
                                OperationType type,
                                OperationStatus status,
                                LimitSnapshotResponse limit) {
}
