package org.shtiroy.module1.hm08.dto;

import java.math.BigDecimal;

public record ReserveRequest(String userId,
                             String operationId,
                             BigDecimal amount) {
}
