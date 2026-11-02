package com.volta.api.security;

import com.volta.api.config.SecurityConfiguration;
import com.volta.api.handler.RestAccessDeniedHandler;
import com.volta.api.handler.RestAuthenticationEntryPoint;
import com.volta.api.security.filter.JwtAuthenticationFilter;
import com.volta.api.security.jwt.TokenProvider;
import com.volta.api.usecase.AreaUseCase;
import com.volta.api.usecase.AuthUseCase;
import com.volta.api.usecase.CollectionUseCase;
import com.volta.api.usecase.CompanyUseCase;
import com.volta.api.usecase.CooperativeUseCase;
import com.volta.api.usecase.EsgUseCase;
import com.volta.api.usecase.GoalUseCase;
import com.volta.api.usecase.IncidentUseCase;
import com.volta.api.usecase.NotificationUseCase;
import com.volta.api.usecase.PasswordResetUseCase;
import com.volta.api.usecase.ReviewUseCase;
import com.volta.api.usecase.SummaryUseCase;
import com.volta.api.usecase.UserUseCase;
import com.volta.api.usecase.WasteTypeUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(properties = {"swagger.username=test-user", "swagger.password=test-password"})
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@DisplayName("Autorização por perfil")
class RoleAuthorizationTest {

    private static final List<String> ALL_ROLES = List.of("ADMIN", "MANAGER", "EMPLOYEE", "OPERATOR");
    private static final Set<String> ADMIN = Set.of("ADMIN");
    private static final Set<String> ADMIN_MANAGER = Set.of("ADMIN", "MANAGER");
    private static final Set<String> ADMIN_MANAGER_EMPLOYEE = Set.of("ADMIN", "MANAGER", "EMPLOYEE");
    private static final Set<String> MANAGER_EMPLOYEE = Set.of("MANAGER", "EMPLOYEE");
    private static final Set<String> ANY_ROLE = Set.copyOf(ALL_ROLES);

    private static final String ID = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenProvider tokenProvider;

    @MockitoBean
    private AreaUseCase areaUseCase;

    @MockitoBean
    private AuthUseCase authUseCase;

    @MockitoBean
    private PasswordResetUseCase passwordResetUseCase;

    @MockitoBean
    private GoalUseCase goalUseCase;

    @MockitoBean
    private CollectionUseCase collectionUseCase;

    @MockitoBean
    private CompanyUseCase companyUseCase;

    @MockitoBean
    private CooperativeUseCase cooperativeUseCase;

    @MockitoBean
    private EsgUseCase esgUseCase;

    @MockitoBean
    private IncidentUseCase incidentUseCase;

    @MockitoBean
    private NotificationUseCase notificationUseCase;

    @MockitoBean
    private ReviewUseCase reviewUseCase;

    @MockitoBean
    private SummaryUseCase summaryUseCase;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private WasteTypeUseCase wasteTypeUseCase;

    record Endpoint(HttpMethod method, String path, String body, Set<String> allowedRoles) {

        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    private static Endpoint endpoint(HttpMethod method, String path, Set<String> roles) {
        return new Endpoint(method, path, null, roles);
    }

    private static Endpoint endpoint(HttpMethod method, String path, String body, Set<String> roles) {
        return new Endpoint(method, path, body, roles);
    }

    static List<Endpoint> endpoints() {
        return List.of(
                endpoint(HttpMethod.POST, "/areas", """
                        {"companyId":"%s","sectorName":"Warehouse"}""".formatted(ID), ADMIN),
                endpoint(HttpMethod.GET, "/areas", ADMIN),
                endpoint(HttpMethod.GET, "/areas/" + ID, ADMIN),
                endpoint(HttpMethod.GET, "/areas/mine", ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.PUT, "/areas/" + ID, """
                        {"sectorName":"Kitchen"}""", ADMIN),
                endpoint(HttpMethod.DELETE, "/areas/" + ID, ADMIN),

                endpoint(HttpMethod.POST, "/collections", """
                        {"incidentId":"%s","cooperativeId":"%s","collectionType":"Recyclable"}""".formatted(ID, ID), ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/collections", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/collections/" + ID, ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/collections/" + ID + "/completion-time", ADMIN_MANAGER),
                endpoint(HttpMethod.PATCH, "/collections/" + ID + "/schedule", """
                        {"scheduledAt":"2099-01-01T10:00:00"}""", ADMIN_MANAGER),
                endpoint(HttpMethod.PATCH, "/collections/" + ID + "/status", """
                        {"status":"IN_PROGRESS"}""", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/collections/" + ID + "/history", ADMIN_MANAGER),

                endpoint(HttpMethod.POST, "/companies", """
                        {"name":"Volta Industry","cnpj":"11.222.333/0001-81","address":"Paulista Avenue, 1000"}""", ADMIN),
                endpoint(HttpMethod.GET, "/companies", ADMIN),
                endpoint(HttpMethod.GET, "/companies/" + ID, ADMIN),
                endpoint(HttpMethod.PUT, "/companies/" + ID, """
                        {"name":"Volta Industry","cnpj":"11.222.333/0001-81","address":"Paulista Avenue, 1000"}""", ADMIN),
                endpoint(HttpMethod.DELETE, "/companies/" + ID, ADMIN),

                endpoint(HttpMethod.POST, "/cooperatives", """
                        {"name":"Recycle More","cnpj":"11.444.777/0001-61","latitude":-23.561684,"longitude":-46.655981,"email":"contato@reciclamais.com.br","specialties":"PAPEL,PLASTICO"}""", ADMIN),
                endpoint(HttpMethod.GET, "/cooperatives", ADMIN),
                endpoint(HttpMethod.GET, "/cooperatives/" + ID, ADMIN),
                endpoint(HttpMethod.PUT, "/cooperatives/" + ID, """
                        {"name":"Recycle More","cnpj":"11.444.777/0001-61","latitude":-23.561684,"longitude":-46.655981,"email":"contato@reciclamais.com.br","specialties":"PAPEL,PLASTICO"}""", ADMIN),
                endpoint(HttpMethod.DELETE, "/cooperatives/" + ID, ADMIN),

                endpoint(HttpMethod.POST, "/esg/recycling-percentage", """
                        {"totalWasteKg":100,"totalRecycledKg":50}""", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/esg/score", ADMIN_MANAGER),
                endpoint(HttpMethod.POST, "/esg/metrics", """
                        {"period":"2026-09","totalWasteKg":100,"totalRecycledKg":50}""", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/esg/metrics", ADMIN_MANAGER_EMPLOYEE),

                endpoint(HttpMethod.GET, "/summary", ADMIN_MANAGER_EMPLOYEE),

                endpoint(HttpMethod.GET, "/goals", ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.PUT, "/goals", """
                        {"monthlyKg":1200.00}""", ADMIN_MANAGER),

                endpoint(HttpMethod.GET, "/notifications", ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.PATCH, "/notifications/" + ID + "/read", ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.PATCH, "/notifications/read-all", ADMIN_MANAGER_EMPLOYEE),

                endpoint(HttpMethod.POST, "/incidents", """
                        {"areaId":"%s","wasteTypeIds":["%s"],"employeeDescription":"Oil leak","priority":"LOW"}""".formatted(ID, ID), MANAGER_EMPLOYEE),
                endpoint(HttpMethod.GET, "/incidents", ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.GET, "/incidents/" + ID, ADMIN_MANAGER_EMPLOYEE),
                endpoint(HttpMethod.PATCH, "/incidents/" + ID + "/reject", """
                        {"reason":"Photo does not show the waste"}""", ADMIN_MANAGER),
                endpoint(HttpMethod.PATCH, "/incidents/" + ID + "/close", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/incidents/" + ID + "/ai-report", ADMIN_MANAGER_EMPLOYEE),

                endpoint(HttpMethod.POST, "/collections/" + ID + "/review", """
                        {"stars":5,"comment":"Great"}""", ADMIN_MANAGER),
                endpoint(HttpMethod.GET, "/cooperatives/" + ID + "/reviews", ADMIN_MANAGER),

                endpoint(HttpMethod.POST, "/users", """
                        {"name":"Ana","email":"ana@volta.com","password":"password123","companyId":"%s"}""".formatted(ID), ADMIN),
                endpoint(HttpMethod.GET, "/users", ADMIN),
                endpoint(HttpMethod.GET, "/users/" + ID, ADMIN),
                endpoint(HttpMethod.GET, "/users/me", ANY_ROLE),
                endpoint(HttpMethod.PUT, "/users/me", """
                        {"name":"Ana"}""", ANY_ROLE),
                endpoint(HttpMethod.PATCH, "/users/" + ID + "/role", """
                        {"role":"MANAGER"}""", ADMIN),

                endpoint(HttpMethod.POST, "/waste-types", """
                        {"category":"PLASTICO","defaultRiskLevel":"LOW"}""", ADMIN),
                endpoint(HttpMethod.GET, "/waste-types", ANY_ROLE),
                endpoint(HttpMethod.GET, "/waste-types/" + ID, ANY_ROLE),
                endpoint(HttpMethod.PUT, "/waste-types/" + ID, """
                        {"category":"PLASTICO","defaultRiskLevel":"LOW"}""", ADMIN),
                endpoint(HttpMethod.DELETE, "/waste-types/" + ID, ADMIN)
        );
    }

    static Stream<Arguments> endpointsWithRoles() {
        return endpoints().stream()
                .flatMap(endpoint -> ALL_ROLES.stream().map(role -> Arguments.of(endpoint, role)));
    }

    static Stream<Arguments> endpointsOnly() {
        return endpoints().stream().map(Arguments::of);
    }

    private MockHttpServletRequestBuilder requestFor(Endpoint endpoint) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(endpoint.method(), endpoint.path());
        if (endpoint.body() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        return builder;
    }

    private UsernamePasswordAuthenticationToken authenticationOf(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(
                UUID.randomUUID(), "user@volta.com", null, UUID.randomUUID(), role);
        return new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(role)));
    }

    @ParameterizedTest(name = "{0} como {1}")
    @MethodSource("endpointsWithRoles")
    @DisplayName("cada perfil acessa somente os endpoints permitidos")
    void request_withRole_isAllowedOrForbiddenAccordingToMatrix(Endpoint endpoint, String role) throws Exception {
        var request = requestFor(endpoint).with(authentication(authenticationOf(role)));

        if (endpoint.allowedRoles().contains(role)) {
            mockMvc.perform(request).andExpect(status().is2xxSuccessful());
        } else {
            mockMvc.perform(request).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest(name = "{0} sem token")
    @MethodSource("endpointsOnly")
    @DisplayName("sem autenticação todo endpoint protegido devolve 401")
    void request_withoutToken_returns401(Endpoint endpoint) throws Exception {
        mockMvc.perform(requestFor(endpoint)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("401 é devolvido no formato problem+json")
    void request_withoutToken_returnsProblemJson() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid or missing credentials"))
                .andExpect(jsonPath("$.instance").value("/users/me"));
    }

    @Test
    @DisplayName("token inválido devolve 401")
    void request_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("403 é devolvido no formato problem+json")
    void request_withForbiddenRole_returnsProblemJson() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/companies").with(authentication(authenticationOf("EMPLOYEE"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.instance").value("/companies"));
    }

    @Test
    @DisplayName("login é público")
    void login_withoutToken_isPublic() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@volta.com","password":"password123"}"""))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login com corpo inválido devolve 400 com os erros por campo")
    void login_withInvalidBody_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Email is required"))
                .andExpect(jsonPath("$.errors.password").value("Password is required"));
    }

    @Test
    @DisplayName("filtro de incidentes com status inválido devolve 400")
    void incidents_withInvalidStatusFilter_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/incidents")
                        .param("status", "XYZ")
                        .with(authentication(authenticationOf("MANAGER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("filtro de incidentes com prioridade inválida devolve 400")
    void incidents_withInvalidPriorityFilter_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/incidents")
                        .param("priority", "URGENT")
                        .with(authentication(authenticationOf("MANAGER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("filtro de incidentes com valores válidos é aceito")
    void incidents_withValidFilters_isAccepted() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/incidents")
                        .param("status", "PENDING")
                        .param("priority", "HIGH")
                        .with(authentication(authenticationOf("MANAGER"))))
                .andExpect(status().isOk());
    }
}
