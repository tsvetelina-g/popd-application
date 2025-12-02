package app.popdapplication.web;

import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.RatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
public class RatingControllerApiTest {

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postAddRating_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();
        int rating = 8;

        MockHttpServletRequestBuilder httpRequest = post("/rating/{movieId}/add", movieId)
                .param("rating", String.valueOf(rating))
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movieId));

        verify(ratingService).upsertRating(eq(userId), eq(movieId), eq(rating));
    }

    @Test
    void postAddRating_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/rating/{movieId}/add", movieId)
                .param("rating", "8")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(ratingService, never()).upsertRating(any(), any(), anyInt());
    }

    @Test
    void postAddRating_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/rating/{movieId}/add", movieId)
                .param("rating", "8")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(ratingService, never()).upsertRating(any(), any(), anyInt());
    }

    @Test
    void deleteRating_andUserIsAuthenticated_thenRedirectToMoviePage() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        UUID userId = ((UserData) authenticatedUser).getUserId();

        MockHttpServletRequestBuilder httpRequest = delete("/rating/{movieId}/delete", movieId)
                .with(user(authenticatedUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movieId));

        verify(ratingService).deleteRating(eq(userId), eq(movieId));
    }

    @Test
    void deleteRating_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/rating/{movieId}/delete", movieId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(ratingService, never()).deleteRating(any(), any());
    }

    @Test
    void deleteRating_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/rating/{movieId}/delete", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(ratingService, never()).deleteRating(any(), any());
    }

    private static UserDetails regularUserAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.USER, true);
    }
}

