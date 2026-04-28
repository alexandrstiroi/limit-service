package org.shtiroy.module1.hm08.controller;

import org.shtiroy.module1.hm08.dto.CancelRequest;
import org.shtiroy.module1.hm08.dto.ConfirmRequest;
import org.shtiroy.module1.hm08.dto.LimitSnapshotResponse;
import org.shtiroy.module1.hm08.dto.OperationResponse;
import org.shtiroy.module1.hm08.dto.ReserveRequest;
import org.shtiroy.module1.hm08.dto.RevertRequest;
import org.shtiroy.module1.hm08.service.LimitService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class LimitController {

    private final LimitService limitService;

    public LimitController(LimitService limitService) {
        this.limitService = limitService;
    }

    @GetMapping("/users/{userId}/limit")
    public LimitSnapshotResponse getLimit(@PathVariable String userId) {
        return limitService.getLimit(userId);
    }

    @PostMapping("/operations/reverse")
    @ResponseStatus(HttpStatus.CREATED)
    public OperationResponse reserve(@RequestBody ReserveRequest request) {
        return limitService.reserve(request.userId(), request.operationId(), request.amount());
    }

    @PostMapping("/operations/confirm")
    public OperationResponse confirm(@RequestBody ConfirmRequest request) {
        return limitService.confirm(request.operationId());
    }

    @PostMapping("/operations/cancel")
    public OperationResponse cancel(@RequestBody CancelRequest request) {
        return limitService.cancel(request.operationId());
    }

    @PostMapping("/operations/revert")
    public OperationResponse revert(@RequestBody RevertRequest request) {
        return limitService.revert(request.operationId());
    }
}
