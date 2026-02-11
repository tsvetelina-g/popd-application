package app.popdapplication;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.entity.Watchlist;
import app.popdapplication.model.enums.WatchlistType;
import app.popdapplication.repository.UserRepository;
import app.popdapplication.service.UserService;
import app.popdapplication.service.WatchlistService;
import app.popdapplication.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class RegisterUserITest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WatchlistService watchlistService;

    @Test
    void registerUser_withValidData_shouldCreateUserAndRedirectToLogin() throws Exception {
        String username = "tsvetelina";
        String email = "ts@gmail.com";
        String password = "123123123";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);

        userService.register(registerRequest);

        Optional<User> createdUser = userRepository.findByUsername(username);

        assertTrue(createdUser.isPresent(), "User should be created in database");
        assertEquals(username, createdUser.get().getUsername());
        assertEquals(email, createdUser.get().getEmail());
        assertTrue(createdUser.get().isActive(), "New user should be active");
        assertNotNull(createdUser.get().getPassword(), "Password should be encoded and saved");
        assertNotEquals(password, createdUser.get().getPassword(), "Password should be encoded, not plain text");

        Watchlist defaultWatchlist = watchlistService.findByUser(createdUser.get());

        assertNotNull(defaultWatchlist, "Default watchlist should be created for new user");
        assertEquals("Default", defaultWatchlist.getName(), "Watchlist name should be 'Default'");
        assertEquals(WatchlistType.DEFAULT, defaultWatchlist.getWatchlistType(), "Watchlist type should be DEFAULT");
        assertEquals(createdUser.get().getId(), defaultWatchlist.getUser().getId(), "Watchlist should belong to the created user");
        assertNotNull(defaultWatchlist.getCreatedOn(), "Watchlist should have createdOn timestamp");
    }
}
