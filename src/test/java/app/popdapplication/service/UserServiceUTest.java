package app.popdapplication.service;

import app.popdapplication.exception.AlreadyExistsException;
import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.repository.UserRepository;
import app.popdapplication.security.UserData;
import app.popdapplication.web.dto.EditProfileRequest;
import app.popdapplication.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private WatchlistService watchlistService;
    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private UserService userService;

    @Test
    void whenEditUserDetails_andRepositoryReturnsOptionalEmpty_thenThrowsException() {

        UUID userId = UUID.randomUUID();
        EditProfileRequest dto = null;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateProfile(userId, dto));
    }

    @Test
    void whenEditUserDetails_andRepositoryReturnsValidUserFromDatabase_thenUpdateAndSaveUserDetails() {

        UUID userId = UUID.randomUUID();
        EditProfileRequest dto = EditProfileRequest.builder()
                .username("GoshoG")
                .email("gosho@gmail.com")
                .firstName("Gosho")
                .lastName("Georgiev")
                .profilePictureUrl("www.picture.com")
                .build();
        User userFromDatabase = User.builder()
                .id(userId)
                .username("StanB")
                .firstName("Stanimir")
                .lastName("Ivanov")
                .email("stan@gmail.com")
                .profilePicture(null)
                .build();
        when(userRepository.findById(any())).thenReturn(Optional.of(userFromDatabase));

        userService.updateProfile(userId, dto);

        assertEquals("GoshoG", userFromDatabase.getUsername());
        assertEquals("gosho@gmail.com", userFromDatabase.getEmail());
        assertEquals("Gosho", userFromDatabase.getFirstName());
        assertEquals("Georgiev", userFromDatabase.getLastName());
        assertNotNull(userFromDatabase.getProfilePicture());
        verify(userRepository).save(userFromDatabase);
    }

    @Test
    void whenSwitchRole_andRepositoryReturnsAdmin_thenUserIsUpdatedWithRoleUserAndUpdatedOnNow_andPersistedInDatabase() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.switchRole(userId);

        assertEquals(UserRole.USER ,user.getRole());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchRole_andRepositoryReturnsUser_thenUserIsUpdatedWithRoleAdminAndUpdatedOnNow_andPersistedInDatabase() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .role(UserRole.USER)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.switchRole(userId);

        assertEquals(UserRole.ADMIN ,user.getRole());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchRole_andRepositoryReturnsOptionalEmpty_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.switchRole(userId));
    }

    @Test
    void whenLoadUserByUsername_andUserExists_thenReturnUserData() {
        String username = "tsvetelina";
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .build();
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals("123123123", result.getPassword());
        assertTrue(result instanceof UserData);
    }

    @Test
    void whenLoadUserByUsername_andUserNotFound_thenThrowsException() {
        String username = "tsvetelina";
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.loadUserByUsername(username));
    }

    @Test
    void whenRegister_andValidData_thenSaveUserAndCreateWatchlist() {
        RegisterRequest request = RegisterRequest.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .password("123123123")
                .build();
        when(userRepository.findByUsername("tsvetelina")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ts@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123123123")).thenReturn("encodedPassword");

        userService.register(request);

        verify(userRepository).save(any(User.class));
        verify(watchlistService).createDefaultWatchlist(any(User.class));
        verify(passwordEncoder).encode("123123123");
    }

    @Test
    void whenRegister_andUsernameAlreadyExists_thenThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .password("123123123")
                .build();
        when(userRepository.findByUsername("tsvetelina")).thenReturn(Optional.of(new User()));

        assertThrows(AlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
        verify(watchlistService, never()).createDefaultWatchlist(any());
    }

    @Test
    void whenRegister_andEmailAlreadyExists_thenThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("existing@example.com")
                .password("password123")
                .build();
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(AlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenFindById_andUserExists_thenReturnUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("tsvetelina").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.findById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getId());
    }

    @Test
    void whenFindById_andUserNotFound_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.findById(userId));
    }

    @Test
    void whenFindAll_withValidPageAndSize_thenReturnPageOfUsers() {
        List<User> users = List.of(
                User.builder().id(UUID.randomUUID()).username("user1").build(),
                User.builder().id(UUID.randomUUID()).username("user2").build()
        );
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);

        Page<User> result = userService.findAll(0, 10);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
    }

    @Test
    void whenFindAll_withNegativePage_thenDefaultToZero() {
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        userService.findAll(-5, 10);

        verify(userRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    void whenFindAll_withInvalidSize_thenDefaultToTen() {
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(PageRequest.class))).thenReturn(emptyPage);

        userService.findAll(0, 0);

        verify(userRepository).findAll(PageRequest.of(0, 10));
    }

    @Test
    void whenSwitchStatus_andUserIsActive_thenMakeStatusInactiveAndForceLogout() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("tsvetelina")
                .active(true)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());

        userService.switchStatus(userId);

        assertFalse(user.isActive());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchStatus_andUserIsInactive_thenMakeStatusActive() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("tsvetelina")
                .active(false)
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.switchStatus(userId);

        assertTrue(user.isActive());
        assertThat(user.getUpdatedOn()).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS));
        verify(userRepository).save(user);
    }

    @Test
    void whenSwitchStatus_andUserNotFound_thenThrowsException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.switchStatus(userId));
    }

    @Test
    void whenSearchUsersLimited_thenReturnLimitedResults() {
        List<User> users = List.of(
                User.builder().username("user1").build(),
                User.builder().username("user2").build(),
                User.builder().username("user3").build()
        );
        when(userRepository.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                "user", "user", "user")).thenReturn(users);

        List<User> result = userService.searchUsersLimited("user", 2);

        assertEquals(2, result.size());
    }

    @Test
    void whenSearchUsersLimited_andNoResults_thenReturnEmptyList() {
        when(userRepository.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                "xyz", "xyz", "xyz")).thenReturn(List.of());

        List<User> result = userService.searchUsersLimited("xyz", 10);

        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetUsernamesByIds_withValidIds_thenReturnMap() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        User user1 = User.builder().id(userId1).username("user1").build();
        User user2 = User.builder().id(userId2).username("user2").build();

        when(userRepository.findById(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findById(userId2)).thenReturn(Optional.of(user2));

        Set<UUID> userIds = Set.of(userId1, userId2);
        Map<UUID, String> result = userService.getUsernamesByIds(userIds);

        assertEquals(2, result.size());
        assertEquals("user1", result.get(userId1));
        assertEquals("user2", result.get(userId2));
    }

    @Test
    void whenGetUsernamesByIds_withEmptySet_thenReturnEmptyMap() {
        Map<UUID, String> result = userService.getUsernamesByIds(Set.of());

        assertTrue(result.isEmpty());
    }

    @Test
    void whenGetUsernamesByIds_andUserNotFound_thenSkipAndContinue() {
        UUID validUserId = UUID.randomUUID();
        UUID invalidUserId = UUID.randomUUID();
        User user = User.builder().id(validUserId).username("tsvetelina").build();

        when(userRepository.findById(validUserId)).thenReturn(Optional.of(user));
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        Set<UUID> userIds = Set.of(validUserId, invalidUserId);
        Map<UUID, String> result = userService.getUsernamesByIds(userIds);

        assertEquals(1, result.size());
        assertEquals("tsvetelina", result.get(validUserId));
    }
}
