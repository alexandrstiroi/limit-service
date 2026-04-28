package org.shtiroy.module1.hm08.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DefaultLimitResponse(BigDecimal defaultLimit,
                                   LocalDateTime updatedAt) {
}
