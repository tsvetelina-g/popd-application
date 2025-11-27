package app.popdapplication.web;

import app.popdapplication.service.ActivityService;
import app.popdapplication.service.MovieService;
import app.popdapplication.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
public class IndexControllerApiTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getIndexEndpoint_shouldReturn200OkAndIndexView() throws Exception {

        MockHttpServletRequestBuilder httpRequest = get("/");

        mockMvc.perform(httpRequest)
                .andExpect(view().name("index"))
                .andExpect(status().isOk());
    }

    @Test
    void postRegister_shouldReturn302RedirectAndRedirectToLoginAndInvokeRegisterServiceMethod() throws Exception {

        MockHttpServletRequestBuilder httpRequest = post("/register")
                .formField("username", "tsvetelina")
                .formField("email", "ts@gmail.com")
                .formField("password", "123123123")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        verify(userService).register(any());
    }

    @Test
    void postRegisterWithInvalidFormData_shouldReturn200OkAndShowRegisterViewAndRegisterServiceMethodIsNeverInvoked() throws Exception {

        MockHttpServletRequestBuilder httpRequest = post("/register")
                .formField("username", "t")
                .formField("email", "t")
                .formField("password", "123")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
        verifyNoInteractions(userService);
    }


}
