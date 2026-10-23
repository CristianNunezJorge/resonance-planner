package com.resonanceplanner.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.resonanceplanner.agent.chat.dto.AgentMessageRequest;
import com.resonanceplanner.auth.dto.LoginRequest;
import com.resonanceplanner.auth.dto.RegisterRequest;
import com.resonanceplanner.event.dto.EventRequest;
import com.resonanceplanner.inventory.dto.UpdateQuantityRequest;
import com.resonanceplanner.onboarding.dto.OnboardingRequest;
import com.resonanceplanner.planning.PlanSpreadsheet;
import com.resonanceplanner.planning.dto.TaskRequest;
import com.resonanceplanner.roster.dto.AddCharacterRequest;
import com.resonanceplanner.roster.dto.UpdateCharacterRequest;
import com.resonanceplanner.user.dto.UpdateProfileRequest;
import com.resonanceplanner.waveplate.dto.UpdateWaveplateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

// Swagger describes every endpoint with examples, and the examples of the requests work (D53, D136):
// a new field or endpoint without an example fails here
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    // The DTO of every request body: the example that Swagger builds for it must be a valid request
    private static final Map<String, Class<?>> REQUEST_BODIES = Map.ofEntries(
            Map.entry("RegisterRequest", RegisterRequest.class),
            Map.entry("LoginRequest", LoginRequest.class),
            Map.entry("UpdateProfileRequest", UpdateProfileRequest.class),
            Map.entry("OnboardingRequest", OnboardingRequest.class),
            Map.entry("UpdateWaveplateRequest", UpdateWaveplateRequest.class),
            Map.entry("AddCharacterRequest", AddCharacterRequest.class),
            Map.entry("UpdateCharacterRequest", UpdateCharacterRequest.class),
            Map.entry("UpdateQuantityRequest", UpdateQuantityRequest.class),
            Map.entry("TaskRequest", TaskRequest.class),
            Map.entry("EventRequest", EventRequest.class),
            Map.entry("AgentMessageRequest", AgentMessageRequest.class));

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final String SCHEMA_REF = "#/components/schemas/";
    private static final String EXPORT = "GET /api/me/calendar/export";
    // Hidden from Swagger on purpose: they exist only in the tests
    private static final Set<String> TEST_CONTROLLERS = Set.of("ErrorTestController", "SecurityProbeController");

    // The document is longer than the limit that the application puts on request bodies (D110): read it with the
    // default limits, and keep the mapper of the application to turn the examples into requests
    private static final JsonMapper DOCUMENT_READER = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private Validator validator;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    private JsonNode apiDocs;

    @BeforeEach
    void readTheApiDocs() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        apiDocs = DOCUMENT_READER.readTree(body);
    }

    @Test
    void describesTheApiAtTheTop() {
        JsonNode info = apiDocs.path("info");

        assertThat(info.path("title").asString()).isEqualTo("Resonance Planner API");
        assertThat(info.path("version").asString()).isEqualTo("1.0");
        // Token, envelope, errors, limits and the WebSocket that Swagger cannot show
        assertThat(info.path("description").asString())
                .contains("Authorize", "\"success\": true", "VALIDATION_ERROR", "\"fields\"", "Retry-After", "/ws",
                        "05-api.md");
    }

    @Test
    void documentsEveryEndpointOfTheApplication() {
        Map<String, JsonNode> operations = operations();
        List<String> undocumented = new ArrayList<>();
        operations.forEach((name, operation) -> {
            if (operation.path("summary").asString().isBlank() || operation.path("description").asString().isBlank()) {
                undocumented.add(name);
            }
        });

        // An endpoint hidden by mistake would be missing from the document
        assertThat(operations).hasSize(apiEndpointsOfTheControllers());
        assertThat(undocumented).isEmpty();
    }

    @Test
    void answersInJsonExceptForTheExcelFile() {
        List<String> wrongType = new ArrayList<>();
        operations().forEach((name, operation) -> {
            for (Map.Entry<String, JsonNode> response : operation.path("responses").properties()) {
                boolean excelFile = name.equals(EXPORT) && response.getKey().equals("200");
                String expected = excelFile ? PlanSpreadsheet.CONTENT_TYPE : MediaType.APPLICATION_JSON_VALUE;
                for (String mediaType : response.getValue().path("content").propertyNames()) {
                    if (!mediaType.equals(expected)) {
                        wrongType.add(name + " " + response.getKey() + " " + mediaType);
                    }
                }
            }
        });

        assertThat(wrongType).isEmpty();
    }

    @Test
    void givesAnExampleOfTheRightTypeForEveryField() {
        List<String> withoutExample = new ArrayList<>();
        for (Map.Entry<String, JsonNode> schema : apiDocs.path("components").path("schemas").properties()) {
            for (Map.Entry<String, JsonNode> field : schema.getValue().path("properties").properties()) {
                if (!hasExample(field.getValue())) {
                    withoutExample.add(schema.getKey() + "." + field.getKey());
                }
            }
        }

        assertThat(withoutExample).isEmpty();
    }

    @Test
    void describesEveryParameterWithAnExample() {
        List<String> undocumented = new ArrayList<>();
        operations().forEach((name, operation) -> {
            for (JsonNode parameter : operation.path("parameters")) {
                boolean described = !parameter.path("description").asString().isBlank();
                if (!described || !hasExample(parameter) && !hasExample(parameter.path("schema"))) {
                    undocumented.add(name + " " + parameter.path("name").asString());
                }
            }
        });

        assertThat(undocumented).isEmpty();
    }

    @Test
    void theExampleOfEveryRequestBodyPassesTheValidation() {
        List<String> problems = new ArrayList<>();
        operations().forEach((name, operation) -> {
            JsonNode content = operation.path("requestBody").path("content");
            if (content.isMissingNode()) {
                return;
            }
            JsonNode body = content.path(MediaType.APPLICATION_JSON_VALUE);
            String schemaName = refName(body.path("schema"));
            Class<?> dto = REQUEST_BODIES.get(schemaName);
            if (dto == null) {
                problems.add(name + ": the body is not JSON, or no DTO is listed for " + schemaName);
                return;
            }
            Object request = jsonMapper.treeToValue(exampleOf(body.path("schema")), dto);
            for (ConstraintViolation<Object> violation : validator.validate(request)) {
                problems.add(name + ": " + violation.getPropertyPath() + " " + violation.getMessage());
            }
        });

        assertThat(problems).isEmpty();
    }

    /**
     * "Try it out" with the examples as they are: the rules of the services (levels and ranks of the game, Waveplate,
     * play time, dates of an event) accept them too. The agent is left out: it would call the model.
     */
    @Test
    @Transactional
    void theExamplesWorkAsRealRequestsOneAfterTheOther() throws Exception {
        String token = jsonMapper.readTree(send("POST", "/api/auth/register", null, 201).getContentAsString())
                .path("data").path("token").asString();
        send("POST", "/api/me/roster", token, 201);
        // The guided setup replaces the roster: its example has the same character
        send("POST", "/api/me/onboarding", token, 200);
        send("PATCH", "/api/me", token, 200);
        send("PUT", "/api/me/waveplate", token, 200);
        send("PUT", "/api/me/roster/{characterId}", token, 200);
        send("PUT", "/api/me/inventory/{materialId}", token, 200);
        send("POST", "/api/me/tasks", token, 201);
        send("POST", "/api/auth/login", null, 200);

        String adminLogin = "{\"email\":\"" + adminEmail + "\",\"password\":\"" + adminPassword + "\"}";
        String adminToken = jsonMapper.readTree(mockMvc.perform(request(HttpMethod.POST, "/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(adminLogin))
                .andReturn().getResponse().getContentAsString()).path("data").path("token").asString();
        send("POST", "/api/admin/events", adminToken, 201);
    }

    /** Sends the example body of an operation, with the example values of its path parameters. */
    private MockHttpServletResponse send(String method, String path, String token, int expectedStatus)
            throws Exception {
        JsonNode operation = apiDocs.path("paths").path(path).path(method.toLowerCase());
        String uri = path;
        for (JsonNode parameter : operation.path("parameters")) {
            uri = uri.replace("{" + parameter.path("name").asString() + "}", parameter.path("example").asString());
        }
        JsonNode schema = operation.path("requestBody").path("content").path(MediaType.APPLICATION_JSON_VALUE)
                .path("schema");
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(method), uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(exampleOf(schema)));
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        MockHttpServletResponse response = mockMvc.perform(builder).andReturn().getResponse();
        assertThat(response.getStatus())
                .as("%s %s answered %s", method, uri, response.getContentAsString())
                .isEqualTo(expectedStatus);
        return response;
    }

    /** Every operation, named like "POST /api/me/tasks", in the order of the document. */
    private Map<String, JsonNode> operations() {
        Map<String, JsonNode> operations = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> path : apiDocs.path("paths").properties()) {
            for (Map.Entry<String, JsonNode> operation : path.getValue().properties()) {
                if (HTTP_METHODS.contains(operation.getKey())) {
                    operations.put(operation.getKey().toUpperCase() + " " + path.getKey(), operation.getValue());
                }
            }
        }
        return operations;
    }

    /**
     * The pairs of HTTP method and path under /api that the controllers answer. Only the controllers that exist in
     * the tests are left out: a hidden endpoint of the application is counted, so it fails the comparison.
     */
    private int apiEndpointsOfTheControllers() {
        int endpoints = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : handlerMapping.getHandlerMethods().entrySet()) {
            if (TEST_CONTROLLERS.contains(mapping.getValue().getBeanType().getSimpleName())) {
                continue;
            }
            long paths = mapping.getKey().getPatternValues().stream().filter(path -> path.startsWith("/api/")).count();
            endpoints += (int) paths * mapping.getKey().getMethodsCondition().getMethods().size();
        }
        return endpoints;
    }

    /**
     * A field has an example when it carries one of the right type, or when Swagger builds it from another schema
     * (a reference, or a list of references), which this test checks on its own.
     */
    private static boolean hasExample(JsonNode schema) {
        JsonNode example = exampleValue(schema);
        if (example != null) {
            return hasTheTypeOf(schema, example) && isOneOfTheValues(schema, example);
        }
        JsonNode items = schema.path("items");
        return schema.has("$ref") || items.has("$ref") || exampleValue(items) != null;
    }

    // A field that can be null has a list of types, like ["integer", "null"]: the example must match one of them
    private static boolean hasTheTypeOf(JsonNode schema, JsonNode example) {
        JsonNode type = schema.path("type");
        if (type.isMissingNode()) {
            return true;
        }
        if (!type.isArray()) {
            return isOfType(type.asString(), example);
        }
        for (JsonNode oneType : type) {
            if (isOfType(oneType.asString(), example)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOfType(String type, JsonNode example) {
        return switch (type) {
            case "integer" -> example.isIntegralNumber();
            case "number" -> example.isNumber();
            case "boolean" -> example.isBoolean();
            case "string" -> example.isString();
            case "array" -> example.isArray();
            case "object" -> example.isObject();
            case "null" -> example.isNull();
            default -> false;
        };
    }

    // The example of an enum is one of its values
    private static boolean isOneOfTheValues(JsonNode schema, JsonNode example) {
        JsonNode values = schema.path("enum");
        if (!values.isArray() || example.isNull()) {
            return true;
        }
        for (JsonNode value : values) {
            if (value.equals(example)) {
                return true;
            }
        }
        return false;
    }

    /** The example of a schema, as Swagger UI builds it: its own example first, then the one of its parts. */
    private JsonNode exampleOf(JsonNode schema) {
        JsonNode example = exampleValue(schema);
        if (example != null) {
            return example;
        }
        if (schema.has("$ref")) {
            return exampleOf(apiDocs.path("components").path("schemas").path(refName(schema)));
        }
        if (schema.has("items")) {
            ArrayNode list = jsonMapper.createArrayNode();
            list.add(exampleOf(schema.path("items")));
            return list;
        }
        ObjectNode object = jsonMapper.createObjectNode();
        for (Map.Entry<String, JsonNode> field : schema.path("properties").properties()) {
            object.set(field.getKey(), exampleOf(field.getValue()));
        }
        return object;
    }

    // OpenAPI 3.1 allows both "example" and a list of "examples"
    private static JsonNode exampleValue(JsonNode schema) {
        if (schema.has("example")) {
            return schema.get("example");
        }
        JsonNode examples = schema.path("examples");
        return examples.isArray() && !examples.isEmpty() ? examples.get(0) : null;
    }

    private static String refName(JsonNode schema) {
        return schema.path("$ref").asString().replace(SCHEMA_REF, "");
    }
}
