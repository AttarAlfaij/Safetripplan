package com.safetrip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.frontend-url=http://localhost:5173")
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRootRedirectsToReactApp() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/"));
    }

    @Test
    void unauthenticatedPlanTripRedirectsHome() throws Exception {
        mockMvc.perform(get("/plan-trip"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser
    void authenticatedPlanTripRedirectsToReactApp() throws Exception {
        mockMvc.perform(get("/plan-trip"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/plan-trip"));
    }

    @Test
    void unauthenticatedApiReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void logoutRedirectsHome() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void faviconIsServedWithoutServerError() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isOk());
    }

    @Test
    void oauthLoginStartsWithGithubRedirect() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        startsWith("https://github.com/login/oauth/authorize")));
    }
}
