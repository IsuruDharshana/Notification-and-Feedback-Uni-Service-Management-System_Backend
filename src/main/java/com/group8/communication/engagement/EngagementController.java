package com.group8.communication.engagement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/engagement-dashboard")
@Tag(name = "Engagement", description = "Aggregate metrics owned by the communication and feedback service.")
public class EngagementController {
    private final EngagementService service;

    public EngagementController(EngagementService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(operationId = "getEngagementSummary", summary = "Get communication and feedback metrics",
            description = "Event participation is currently unavailable and is represented by null plus "
                    + "eventParticipationAvailable=false, rather than a zero count.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Engagement metrics.", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalError")
    })
    public EngagementDtos.SummaryResponse summary() {
        return service.summary();
    }
}
