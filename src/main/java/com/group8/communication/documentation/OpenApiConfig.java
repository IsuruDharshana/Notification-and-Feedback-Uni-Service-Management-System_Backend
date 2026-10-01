package com.group8.communication.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI communicationFeedbackOpenApi() {
        ObjectSchema errorSchema = new ObjectSchema();
        errorSchema.addProperty("code", new StringSchema().example("VALIDATION_FAILED"));
        errorSchema.setRequired(List.of("code"));
        Components components = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                        .description("Group 5 RS256 access token. Paste only the token, without the Bearer prefix. "
                                + "The token subject is the Group 5 user id (for example usr-student-001)."))
                .addSecuritySchemes("serviceKey", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-Service-Key")
                        .description("Shared secret configured as NOTIFICATIONS_SERVICE_KEY. "
                                + "Used only by POST /api/notifications/trigger; it is not a user login token."))
                .addSchemas("ApiError", errorSchema)
                .addResponses("Unauthorized", error("Missing, expired or invalid Group 5 bearer token.", "UNAUTHORIZED"))
                .addResponses("InvalidServiceKey", error("Missing, incorrect or unconfigured X-Service-Key.", "INVALID_SERVICE_KEY"))
                .addResponses("ValidationError", error("Request fields or business validation failed.", "VALIDATION_FAILED"))
                .addResponses("Forbidden", error("The caller does not have the required role, ownership or eligibility.", "FORBIDDEN"))
                .addResponses("NotFound", error("The requested record or eligible recipient was not found. Code depends on the operation.",
                        "NOTIFICATION_NOT_FOUND", "NOTIFICATION_RECIPIENT_NOT_FOUND", "ANNOUNCEMENT_NOT_FOUND",
                        "USER_NOT_FOUND", "FEEDBACK_FORM_NOT_FOUND", "FEEDBACK_ACTIVITY_NOT_FOUND"))
                .addResponses("Conflict", error("Duplicate submission or invalid state transition. Code depends on the operation.",
                        "FEEDBACK_ALREADY_SUBMITTED", "FEEDBACK_FORM_ALREADY_EXISTS", "FEEDBACK_FORM_INACTIVE",
                        "ANNOUNCEMENT_NOT_DRAFT", "ANNOUNCEMENT_NOT_PUBLISHED"))
                .addResponses("ServiceUnavailable", error("A required provider is unavailable or returned invalid data. Code identifies the provider.",
                        "RECIPIENT_DIRECTORY_UNAVAILABLE", "RECIPIENT_DIRECTORY_INVALID_RESPONSE",
                        "GROUP7_ELIGIBILITY_UNAVAILABLE", "GROUP7_INVALID_ELIGIBILITY_RESPONSE",
                        "EVENT_SERVICE_UNAVAILABLE", "EVENT_SERVICE_INVALID_RESPONSE"))
                .addResponses("InternalError", error("Unexpected server error; details are recorded only in server logs.", "INTERNAL_SERVER_ERROR"));

        return new OpenAPI()
                .info(new Info().title("Group 8 Communication and Feedback API").version("1.0.0")
                        .description("In-app notifications, targeted announcements, feedback forms/responses and engagement summary. "
                                + "User endpoints require a Group 5 bearer token. The notification trigger uses X-Service-Key. "
                                + "A created notification is stored in the in-app inbox; this API does not send email or SMS."))
                // Relative origin works on localhost and HTTPS deployments without exposing internal proxy URLs.
                .servers(List.of(new Server().url("/").description("The server serving this documentation")))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private static ApiResponse error(String description, String... codes) {
        MediaType body = new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"));
        if (codes.length == 1) {
            body.example(Map.of("code", codes[0]));
        } else {
            for (String code : codes) {
                body.addExamples(code, new Example().value(Map.of("code", code)));
            }
        }
        return new ApiResponse().description(description).content(new Content().addMediaType("application/json", body));
    }
}
