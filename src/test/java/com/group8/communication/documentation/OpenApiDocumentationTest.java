package com.group8.communication.documentation;

import com.group8.communication.announcement.AnnouncementRepository;
import com.group8.communication.feedback.FeedbackFormRepository;
import com.group8.communication.feedback.FeedbackResponseRepository;
import com.group8.communication.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

/** Exercises the real Boot 4/Jackson 3/Tomcat/security/springdoc stack, without a database or live credentials. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "notification.service-key=documentation-test-only-key",
        "debug=false", "logging.level.org.springframework=INFO",
        "spring.security.user.password=documentation-test-only-unused-password",
        "jwt.jwks-url=http://127.0.0.1:1/never-call-jwks",
        "user-directory.base-url=", "user-directory.access-token=",
        "facility-directory.base-url=", "group7-feedback.base-url=", "event-service.base-url="
})
class OpenApiDocumentationTest {
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int port;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @MockitoBean private NotificationRepository notifications;
    @MockitoBean private AnnouncementRepository announcements;
    @MockitoBean private FeedbackFormRepository forms;
    @MockitoBean private FeedbackResponseRepository responses;

    @Test
    void swaggerUiAndItsAssetsArePublicAndUseOurSpecification() throws Exception {
        HttpResponse<String> redirect = get("/swagger-ui.html");
        assertTrue(redirect.statusCode() >= 300 && redirect.statusCode() < 400);
        String location = redirect.headers().firstValue("Location").orElseThrow();
        assertTrue(location.endsWith("/swagger-ui/index.html"), location);

        assertTrue(ok("/swagger-ui/index.html").contains("Swagger UI"));
        assertFalse(ok("/swagger-ui/swagger-ui-bundle.js").isBlank());
        assertTrue(ok("/swagger-ui/swagger-initializer.js").contains("/v3/api-docs/swagger-config"));
        JsonNode config = JSON.readTree(ok("/v3/api-docs/swagger-config"));
        assertEquals("/v3/api-docs", config.path("url").asString(""));
        assertFalse(config.path("persistAuthorization").asBoolean());
        assertFalse(config.toString().contains("petstore"));
        assertEquals("", config.path("validatorUrl").asString(""));
    }

    @Test
    void generatedJsonDocumentsEveryControllerOperationWithoutDanglingReferences() throws Exception {
        JsonNode spec = JSON.readTree(ok("/v3/api-docs"));
        assertTrue(spec.path("openapi").asString("").startsWith("3.0."));
        assertEquals("Group 8 Communication and Feedback API", spec.at("/info/title").asString(""));
        assertEquals("/", spec.at("/servers/0/url").asString(""));

        Set<String> actualRoutes = new HashSet<>();
        mappings.getHandlerMethods().forEach((mapping, method) -> {
            for (String path : mapping.getPatternValues()) {
                if (path.startsWith("/api/")) {
                    mapping.getMethodsCondition().getMethods().forEach(verb -> actualRoutes.add(verb.name() + " " + path));
                }
            }
        });
        Set<String> documentedRoutes = operations(spec);
        assertEquals(13, documentedRoutes.size());
        assertEquals(actualRoutes, documentedRoutes);

        Set<String> operationIds = new HashSet<>();
        for (Map.Entry<String, JsonNode> path : spec.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (!HTTP_METHODS.contains(operation.getKey())) continue;
                JsonNode details = operation.getValue();
                assertFalse(details.path("summary").asString("").isBlank(), path.getKey());
                assertFalse(details.path("operationId").asString("").isBlank());
                assertTrue(operationIds.add(details.path("operationId").asString("")), "Duplicate operationId");
                assertTrue(details.path("responses").has("401"), path.getKey());
                for (JsonNode parameter : details.path("parameters")) {
                    assertNotEquals("auth", parameter.path("name").asString(""));
                    assertNotEquals("X-Service-Key", parameter.path("name").asString(""));
                }
            }
        }
        assertReferencesResolve(spec, spec);
        assertFalse(spec.path("paths").has("/actuator/health"));
    }

    @Test
    void authenticationSchemesDistinguishUsersFromServiceTriggers() throws Exception {
        JsonNode spec = JSON.readTree(ok("/v3/api-docs"));
        assertTrue(spec.at("/security/0").has("bearerAuth"));
        assertEquals("http", spec.at("/components/securitySchemes/bearerAuth/type").asString(""));
        assertEquals("bearer", spec.at("/components/securitySchemes/bearerAuth/scheme").asString(""));
        assertEquals("apiKey", spec.at("/components/securitySchemes/serviceKey/type").asString(""));
        assertEquals("header", spec.at("/components/securitySchemes/serviceKey/in").asString(""));
        assertEquals("X-Service-Key", spec.at("/components/securitySchemes/serviceKey/name").asString(""));

        JsonNode trigger = spec.path("paths").path("/api/notifications/trigger").path("post");
        assertEquals(JSON.readTree("[{\"serviceKey\":[]}]"), trigger.path("security"));
        assertTrue(trigger.path("responses").has("201"));
        assertTrue(trigger.path("responses").has("200"));
        assertEquals("#/components/responses/InvalidServiceKey", trigger.at("/responses/401/$ref").asString(""));
        assertTrue(trigger.path("requestBody").path("required").asBoolean());
    }

    @Test
    void schemasPreserveStringIdsValidationAndDistinctResponseNames() throws Exception {
        JsonNode spec = JSON.readTree(ok("/v3/api-docs"));
        JsonNode schemas = spec.at("/components/schemas");
        JsonNode trigger = schemas.path("NotificationTriggerRequest");
        JsonNode properties = trigger.path("properties");
        assertEquals("string", properties.at("/recipientId/type").asString(""));
        assertEquals(64, properties.at("/recipientId/maxLength").asInt());
        assertEquals(200, properties.at("/idempotencyKey/maxLength").asInt());
        assertEquals(2000, properties.at("/message/maxLength").asInt());
        assertEquals("string", properties.at("/relatedId/type").asString(""));
        assertEquals(128, properties.at("/relatedId/maxLength").asInt());
        assertTrue(properties.at("/relatedId/nullable").asBoolean());
        assertEquals(Set.of("REGISTRATION_CONFIRMED", "REGISTRATION_CANCELLED", "EVENT_CANCELLED",
                "EVENT_UPDATED", "RESERVATION_STATUS", "SERVICE_REQUEST_STATUS"), strings(properties.at("/type/enum")));
        assertEquals(Set.of("recipientId", "type", "message", "relatedType", "sourceService", "idempotencyKey"), strings(trigger.path("required")));

        assertTrue(schemas.at("/Notification/properties").has("recipientId"));
        assertFalse(schemas.at("/Notification/properties").has("audienceType"));
        assertTrue(schemas.at("/Announcement/properties").has("audienceType"));
        assertFalse(schemas.at("/Announcement/properties").has("recipientId"));
        assertTrue(schemas.at("/Announcement/properties/publishedAt/nullable").asBoolean());
        assertTrue(schemas.at("/Announcement/properties/ruleValue/nullable").asBoolean());
        assertTrue(schemas.at("/Notification/properties/relatedId/nullable").asBoolean());
        assertTrue(schemas.at("/FeedbackResponse/properties/comment/nullable").asBoolean());
        assertEquals("string", schemas.at("/FeedbackFormCreateRequest/properties/activityId/type").asString(""));
        assertEquals("string", schemas.at("/FeedbackFormCreateRequest/properties/questionsJson/type").asString(""));
        assertEquals(1, schemas.at("/FeedbackResponseRequest/properties/rating/minimum").asInt());
        assertEquals(5, schemas.at("/FeedbackResponseRequest/properties/rating/maximum").asInt());
        assertTrue(schemas.at("/EngagementSummary/properties/eventParticipationCount/nullable").asBoolean());

        JsonNode response = spec.path("paths").path("/api/notifications/trigger").at("/post/responses/201/content");
        assertTrue(response.toString().contains("#/components/schemas/Notification"));
    }

    @Test
    void yamlSpecificationHasTheSameOperationsAndCanBeReadWithoutLogin() throws Exception {
        HttpResponse<String> response = get("/v3/api-docs.yaml");
        assertEquals(200, response.statusCode(), response.body());
        Map<String, Object> yaml = new Yaml().load(response.body());
        JsonNode spec = JSON.valueToTree(yaml);
        assertEquals(operations(JSON.readTree(ok("/v3/api-docs"))), operations(spec));
        assertReferencesResolve(spec, spec);
    }

    @Test
    void portableContractIsValidYamlAndCoversTheSameOperations() throws Exception {
        try (var input = getClass().getResourceAsStream("/openapi/notification-api.yaml")) {
            assertNotNull(input);
            Map<String, Object> yaml = new Yaml().load(input);
            JsonNode contract = JSON.valueToTree(yaml);
            assertEquals(operations(JSON.readTree(ok("/v3/api-docs"))), operations(contract));
            assertReferencesResolve(contract, contract);
            assertEquals(2000, contract.at("/components/schemas/NotificationTriggerRequest/properties/message/maxLength").asInt());
        }
    }

    @Test
    void publicDocumentationDoesNotExposeProtectedBusinessEndpoints() throws Exception {
        for (String path : new String[]{"/api/notifications", "/api/announcements", "/api/feedback/forms",
                "/api/engagement-dashboard/summary"}) {
            HttpResponse<String> response = get(path);
            assertEquals(401, response.statusCode(), path);
            assertEquals("UNAUTHORIZED", JSON.readTree(response.body()).path("code").asString(""));
        }
        String payload = """
                {"recipientId":"usr-student-001","type":"EVENT_CANCELLED","message":"Test only",
                 "relatedType":"EVENT","relatedId":"event-doc-test","sourceService":"event-service",
                 "idempotencyKey":"documentation-test-only"}
                """;
        for (boolean wrongKey : new boolean[]{false, true}) {
            HttpRequest.Builder request = HttpRequest.newBuilder(uri("/api/notifications/trigger"))
                    .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));
            if (wrongKey) request.header("X-Service-Key", "wrong-test-key");
            HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(401, response.statusCode(), response.body());
            assertEquals("INVALID_SERVICE_KEY", JSON.readTree(response.body()).path("code").asString(""));
        }
        verifyNoInteractions(notifications, announcements, forms, responses);
    }

    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String ok(String path) throws Exception {
        HttpResponse<String> response = get(path);
        assertEquals(200, response.statusCode(), path + ": " + response.body());
        return response.body();
    }

    private static Set<String> strings(JsonNode array) {
        Set<String> values = new HashSet<>();
        array.forEach(value -> values.add(value.asString("")));
        return values;
    }

    private static Set<String> operations(JsonNode spec) {
        Set<String> routes = new HashSet<>();
        spec.path("paths").properties().forEach(path -> path.getValue().properties().forEach(operation -> {
            if (HTTP_METHODS.contains(operation.getKey())) routes.add(operation.getKey().toUpperCase() + " " + path.getKey());
        }));
        return routes;
    }

    private static void assertReferencesResolve(JsonNode root, JsonNode node) {
        if (node.isObject()) {
            if (node.has("$ref")) {
                String ref = node.path("$ref").asString("");
                assertTrue(ref.startsWith("#/"), ref);
                assertFalse(root.at(ref.substring(1)).isMissingNode(), "Unresolved reference: " + ref);
            }
            node.properties().forEach(property -> assertReferencesResolve(root, property.getValue()));
        } else if (node.isArray()) {
            node.forEach(child -> assertReferencesResolve(root, child));
        }
    }
}
