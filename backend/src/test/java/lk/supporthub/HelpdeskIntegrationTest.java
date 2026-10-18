package lk.supporthub;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}","spring.datasource.username=${TEST_DB_USER:sa}","spring.datasource.password=${TEST_DB_PASSWORD:}"})
@AutoConfigureMockMvc
class HelpdeskIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AccountRepository accounts;
    @Autowired TicketRepository tickets;@Autowired CommentRepository comments;@Autowired ActivityRepository activities;
    @Autowired PasswordEncoder encoder;
    Account requester,other,agent,admin;
    @BeforeEach void setup() {
        comments.deleteAll();activities.deleteAll();tickets.deleteAll();accounts.deleteAll();
        requester=account("user@test.test",Role.REQUESTER);other=account("other@test.test",Role.REQUESTER);
        agent=account("agent@test.test",Role.AGENT);admin=account("admin@test.test",Role.ADMIN);
    }
    Account account(String email,Role role) {var a=new Account();a.name=email.split("@")[0];a.email=email;a.role=role;a.passwordHash=encoder.encode("StrongPassword!26");return accounts.save(a);}
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode create()throws Exception {
        String response=mvc.perform(post("/api/tickets").with(user(requester.email)).with(csrf()).contentType("application/json")
            .content(body(Map.of("title","VPN unavailable","description","Connection fails after sign in","category","NETWORK","priority","HIGH"))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }
    String update(long version,String status,Long assignee)throws Exception {
        var map=new HashMap<String,Object>();map.put("version",version);map.put("status",status);map.put("priority","HIGH");map.put("assigneeId",assignee);return body(map);
    }
    @Test void authenticationCsrfAndLogout()throws Exception {
        mvc.perform(get("/api/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("email",requester.email,"password","StrongPassword!26")))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",requester.email,"password","incorrect")))).andExpect(status().isUnauthorized());
        var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",requester.email,"password","StrongPassword!26"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("REQUESTER")).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());assertTrue(session.isInvalid());
    }
    @Test void registrationCannotElevatePrivilegesAndValidates()throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name","New User","email","new@test.test","password","StrongPassword!26","role","ADMIN")))).andExpect(status().isBadRequest());
        String input=body(Map.of("name","New User","email","NEW@test.test","password","StrongPassword!26"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(input)).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("REQUESTER"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(input)).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("name"," ","email","bad","password","short")))).andExpect(status().isBadRequest());
        assertTrue(encoder.matches("StrongPassword!26",accounts.findByEmail("new@test.test").orElseThrow().passwordHash));
    }
    @Test void requesterIsolationIncludesSearchDashboardAndComments()throws Exception {
        long id=create().get("id").asLong();
        mvc.perform(get("/api/tickets").with(user(other.email))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/dashboard").with(user(other.email))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/tickets/"+id).with(user(other.email))).andExpect(status().isNotFound());
        mvc.perform(post("/api/tickets/"+id+"/comments").with(user(other.email)).with(csrf()).contentType("application/json").content("{\"body\":\"Not mine\"}")).andExpect(status().isNotFound());
        mvc.perform(put("/api/tickets/"+id).with(user(requester.email)).with(csrf()).contentType("application/json").content(update(0,"RESOLVED",null))).andExpect(status().isForbidden());
        mvc.perform(get("/api/tickets?q=VPN&priority=HIGH").with(user(requester.email))).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/tickets?q=%25").with(user(agent.email))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/tickets?page=-1").with(user(agent.email))).andExpect(status().isBadRequest());
    }
    @Test void assignmentLifecycleAndOptimisticConflict()throws Exception {
        long id=create().get("id").asLong();String path="/api/tickets/"+id;
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"IN_PROGRESS",null))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"IN_PROGRESS",requester.id))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"CLOSED",agent.id))).andExpect(status().isBadRequest());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"IN_PROGRESS",agent.id))).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"RESOLVED",agent.id))).andExpect(status().isConflict());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(1,"RESOLVED",agent.id))).andExpect(status().isOk());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(2,"CLOSED",agent.id))).andExpect(status().isOk());
        mvc.perform(post(path+"/comments").with(user(requester.email)).with(csrf()).contentType("application/json").content("{\"body\":\"Thanks\"}")).andExpect(status().isConflict());
        mvc.perform(put(path).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(3,"OPEN",agent.id))).andExpect(status().isOk());
        mvc.perform(post(path+"/comments").with(user(requester.email)).with(csrf()).contentType("application/json").content("{\"body\":\"Still seeing this issue\"}")).andExpect(status().isCreated());
        mvc.perform(get(path).with(user(requester.email))).andExpect(jsonPath("$.comments[0].body").value("Still seeing this issue")).andExpect(jsonPath("$.activities.length()").value(7));
    }
    @Test void teamAdministrationAndLivePermissionRevocation()throws Exception {
        mvc.perform(get("/api/users").with(user(agent.email))).andExpect(status().isForbidden());
        mvc.perform(get("/api/agents").with(user(requester.email))).andExpect(status().isForbidden());
        mvc.perform(put("/api/users/"+admin.id).with(user(admin.email)).with(csrf()).contentType("application/json").content("{\"role\":\"REQUESTER\",\"enabled\":false}")).andExpect(status().isBadRequest());
        long id=create().get("id").asLong();
        mvc.perform(put("/api/tickets/"+id).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(0,"IN_PROGRESS",agent.id))).andExpect(status().isOk());
        mvc.perform(put("/api/users/"+agent.id).with(user(admin.email)).with(csrf()).contentType("application/json").content("{\"role\":\"AGENT\",\"enabled\":false}")).andExpect(status().isConflict());
        mvc.perform(put("/api/tickets/"+id).with(user(agent.email)).with(csrf()).contentType("application/json").content(update(1,"RESOLVED",null))).andExpect(status().isOk());
        mvc.perform(put("/api/users/"+agent.id).with(user(admin.email)).with(csrf()).contentType("application/json").content("{\"role\":\"AGENT\",\"enabled\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/tickets").with(user(agent.email))).andExpect(status().isUnauthorized());
    }
    @Test void malformedAndEmptyTicketsAreRejected()throws Exception {
        mvc.perform(post("/api/tickets").with(user(requester.email)).with(csrf()).contentType("application/json").content("{\"title\":\" \",\"description\":\"\",\"category\":\"GENERAL\",\"priority\":\"HIGH\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets?status=UNKNOWN").with(user(requester.email))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/tickets/99999").with(user(agent.email))).andExpect(status().isNotFound());
    }
}
