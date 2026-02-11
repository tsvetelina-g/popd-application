package app.popdapplication.web;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
public class AdminControllerApiTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void patchRequestChangeUserStatus_fromAdminUser_shouldReturnRedirectAndInvokeServiceMethod() throws Exception {
        UserDetails mockUserData = adminAuthentication();

        int page = 0;
        int size = 10;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/status", UUID.randomUUID())
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=" + page + "&size=" + size));
        verify(userService).switchStatus(any());
    }

    @Test
    void patchRequestChangeUserStatus_fromNormalUser_shouldReturn404StatusCodeAndViewNotFound() throws Exception {
        UserDetails mockUserData = nonAdminAuthentication();

        int page = 0;
        int size = 10;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/status", UUID.randomUUID())
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));
        verify(userService, never()).switchStatus(any());
    }

    @Test
    void getAdminPage_andUserIsAdmin_thenReturnAdminViewAnd200Ok() throws Exception {
        UserDetails mockUserData = adminAuthentication();

        MockHttpServletRequestBuilder httpRequest = get("/admin")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("admin"));
    }

    @Test
    void getAdminPage_andUserIsNotAdmin_thenReturn403Forbidden() throws Exception {
        UserDetails mockUserData = nonAdminAuthentication();

        MockHttpServletRequestBuilder httpRequest = get("/admin")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getAdminPage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        MockHttpServletRequestBuilder httpRequest = get("/admin");

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void getAllUsers_andUserIsAdmin_thenReturnAdminUsersViewWithUsersAnd200Ok() throws Exception {
        UserDetails mockUserData = adminAuthentication();
        List<User> users = List.of(
                User.builder()
                        .id(UUID.randomUUID())
                        .username("stanimir")
                        .role(UserRole.USER)
                        .createdOn(LocalDateTime.now())
                        .updatedOn(LocalDateTime.now())
                        .build(),
                User.builder()
                        .id(UUID.randomUUID())
                        .username("gosho")
                        .role(UserRole.ADMIN)
                        .createdOn(LocalDateTime.now())
                        .updatedOn(LocalDateTime.now())
                        .build()
        );
        Page<User> usersPage = new PageImpl<>(users);

        when(userService.findAll(0, 10)).thenReturn(usersPage);

        MockHttpServletRequestBuilder httpRequest = get("/admin/users")
                .param("page", "0")
                .param("size", "10")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attributeExists("size"));

        verify(userService).findAll(0, 10);
    }

    @Test
    void getAllUsers_andUserIsAdmin_andNoPageParams_thenUseDefaultValues() throws Exception {
        UserDetails mockUserData = adminAuthentication();
        Page<User> emptyPage = new PageImpl<>(List.of());

        when(userService.findAll(0, 10)).thenReturn(emptyPage);

        MockHttpServletRequestBuilder httpRequest = get("/admin/users")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-users"));

        verify(userService).findAll(0, 10);
    }

    @Test
    void getAllUsers_andUserIsNotAdmin_thenReturn403Forbidden() throws Exception {
        UserDetails mockUserData = nonAdminAuthentication();

        MockHttpServletRequestBuilder httpRequest = get("/admin/users")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(userService, never()).findAll(anyInt(), anyInt());
    }

    @Test
    void patchChangeUserStatus_andUserIsAdmin_andCustomPageParams_thenRedirectWithCorrectParams() throws Exception {
        UserDetails mockUserData = adminAuthentication();
        UUID userId = UUID.randomUUID();
        int page = 5;
        int size = 25;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/status", userId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=" + page + "&size=" + size));

        verify(userService).switchStatus(userId);
    }

    @Test
    void patchChangeUserStatus_andNoCsrfToken_thenReturn302Redirect() throws Exception {
        UserDetails mockUserData = adminAuthentication();

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/status", UUID.randomUUID())
                .param("page", "0")
                .param("size", "10")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(userService, never()).switchStatus(any());
    }

    @Test
    void patchChangeUserRole_andUserIsAdmin_thenSwitchRoleAndRedirectWithParams() throws Exception {
        UserDetails mockUserData = adminAuthentication();
        UUID userId = UUID.randomUUID();
        int page = 0;
        int size = 10;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/role", userId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=" + page + "&size=" + size));

        verify(userService).switchRole(userId);
    }

    @Test
    void patchChangeUserRole_andUserIsAdmin_andCustomPageParams_thenRedirectWithCorrectParams() throws Exception {
        UserDetails mockUserData = adminAuthentication();
        UUID userId = UUID.randomUUID();
        int page = 3;
        int size = 15;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/role", userId)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users?page=" + page + "&size=" + size));

        verify(userService).switchRole(userId);
    }

    @Test
    void patchChangeUserRole_andUserIsNotAdmin_thenReturn403AndDoNotInvokeService() throws Exception {
        UserDetails mockUserData = nonAdminAuthentication();
        int page = 0;
        int size = 10;

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/role", UUID.randomUUID())
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .with(user(mockUserData))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(userService, never()).switchRole(any());
    }

    @Test
    void patchChangeUserRole_andNoCsrfToken_thenReturn302Redirect() throws Exception {
        UserDetails mockUserData = adminAuthentication();

        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/role", UUID.randomUUID())
                .param("page", "0")
                .param("size", "10")
                .with(user(mockUserData));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(userService, never()).switchRole(any());
    }

    @Test
    void patchChangeUserRole_andUserNotAuthenticated_thenRedirectToLogin() throws Exception {
        MockHttpServletRequestBuilder httpRequest = patch("/admin/users/{userId}/role", UUID.randomUUID())
                .param("page", "0")
                .param("size", "10")
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(userService, never()).switchRole(any());
    }

    public static UserDetails adminAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.ADMIN, true);
    }

    public static UserDetails nonAdminAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.USER, true);
    }
}
