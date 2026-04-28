package org.shtiroy.module1.hm08.controller;

import org.shtiroy.module1.hm08.dto.DefaultLimitRequest;
import org.shtiroy.module1.hm08.dto.DefaultLimitResponse;
import org.shtiroy.module1.hm08.service.LimitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class AdminLimitController {

    private final LimitService limitService;

    public AdminLimitController(LimitService limitService) {
        this.limitService = limitService;
    }

    @GetMapping("/default-limit")
    public DefaultLimitResponse getDefaultLimit() {
        return limitService.getDefaultLimit();
    }

    @PutMapping("/default-limit")
    public DefaultLimitResponse updateDefaultLimit(@RequestBody DefaultLimitRequest request) {
        return limitService.updateDefaultLimit(request.amount());
    }
}
