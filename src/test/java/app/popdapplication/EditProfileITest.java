package app.popdapplication;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.repository.UserRepository;
import app.popdapplication.service.UserService;
import app.popdapplication.web.dto.EditProfileRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class EditProfileITest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Validator validator;

    @Test
    void editProfile_withValidData_shouldUpdateUserInDatabase() {
        User user = User.builder()
                .username("tsvetelina")
                .email("old@gmail.com")
                .password("123123123")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .profilePicture(null)
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        userRepository.save(user);

        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("stanimir")
                .email("new@gmail.com")
                .firstName("Stanimir")
                .lastName("Andonov")
                .profilePictureUrl("www.image.com")
                .build();

        userService.updateProfile(user.getId(), editRequest);

        User updatedUser = userService.findById(user.getId());

        assertEquals("stanimir", updatedUser.getUsername());
        assertEquals("new@gmail.com", updatedUser.getEmail());
        assertEquals("Stanimir", updatedUser.getFirstName());
        assertEquals("Andonov", updatedUser.getLastName());
        assertEquals("www.image.com", updatedUser.getProfilePicture());
        assertNotNull(updatedUser.getUpdatedOn());
    }

    @Test
    void editProfile_withUsernameTooShort_shouldHaveValidationError() {
        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("ts")
                .email("ts@gmail.com")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);

        assertFalse(violations.isEmpty(), "Should have validation errors for short username");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void editProfile_withUsernameTooLong_shouldHaveValidationError() {
        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tsvetelina".repeat(5))
                .email("ts@gmail.com")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);

        assertFalse(violations.isEmpty(), "Should have validation errors for long username");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void editProfile_withInvalidEmail_shouldHaveValidationError() {
        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tsvetelina")
                .email("invalid-email")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);

        assertFalse(violations.isEmpty(), "Should have validation errors for invalid email");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void editProfile_withInvalidProfilePictureUrl_shouldHaveValidationError() {
        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .profilePictureUrl("not-a-valid-url")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);

        assertFalse(violations.isEmpty(), "Should have validation errors for invalid URL");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("profilePictureUrl")));
    }

    @Test
    void editProfile_withNonExistentUser_shouldThrowException() {
        UUID nonExistentId = UUID.randomUUID();

        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .firstName("Tsvetelina")
                .lastName("Georgieva")
                .build();

        assertThrows(NotFoundException.class, () -> userService.updateProfile(nonExistentId, editRequest));
    }

    @Test
    void editProfile_withNullOptionalFields_shouldUpdateSuccessfully() {
        User user = User.builder()
                .username("gosho")
                .email("g@gmail.com")
                .password("123123123")
                .firstName("Gosho")
                .lastName("Georgiev")
                .profilePicture("www.image.com")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        userRepository.save(user);

        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tosho")
                .email("t@gmail.com")
                .firstName(null)
                .lastName(null)
                .profilePictureUrl(null)
                .build();

        userService.updateProfile(user.getId(), editRequest);

        User updatedUser = userService.findById(user.getId());

        assertEquals("tosho", updatedUser.getUsername());
        assertEquals("t@gmail.com", updatedUser.getEmail());
        assertNull(updatedUser.getFirstName());
        assertNull(updatedUser.getLastName());
        assertNull(updatedUser.getProfilePicture());
    }

    @Test
    void editProfile_withOnlyRequiredFields_shouldUpdateSuccessfully() {
        User user = User.builder()
                .username("pesho")
                .email("p@gmail.com")
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        userRepository.save(user);

        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("stanimir")
                .email("st@gmail.com")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);
        assertTrue(violations.isEmpty(), "Should have no validation errors with only required fields");

        userService.updateProfile(user.getId(), editRequest);

        User updatedUser = userService.findById(user.getId());

        assertEquals("stanimir", updatedUser.getUsername());
        assertEquals("st@gmail.com", updatedUser.getEmail());
    }

    @Test
    void editProfile_withValidUrl_shouldUpdateProfilePicture() {
        User user = User.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .password("123123123")
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();
        userRepository.save(user);

        EditProfileRequest editRequest = EditProfileRequest.builder()
                .username("tsvetelina")
                .email("ts@gmail.com")
                .profilePictureUrl("https://www.image.com")
                .build();

        Set<ConstraintViolation<EditProfileRequest>> violations = validator.validate(editRequest);
        assertTrue(violations.isEmpty(), "Should have no validation errors with valid URL");

        userService.updateProfile(user.getId(), editRequest);

        User updatedUser = userService.findById(user.getId());

        assertEquals("https://www.image.com", updatedUser.getProfilePicture());
    }
}

