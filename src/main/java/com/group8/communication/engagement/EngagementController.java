package com.group8.communication.engagement;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/engagement-dashboard")
public class EngagementController {
    private final EngagementService service;

    public EngagementController(EngagementService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public EngagementDtos.SummaryResponse summary() {
        return service.summary();
    }
}
