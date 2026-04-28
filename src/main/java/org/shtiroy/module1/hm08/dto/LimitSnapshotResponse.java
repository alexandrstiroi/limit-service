package org.shtiroy.module1.hm08.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LimitSnapshotResponse(String userId,
                                   BigDecimal availableAmount,
                                   BigDecimal reservedAmount,
                                   BigDecimal defaultLimit,
                                   LocalDate lastResetDate) {
}
