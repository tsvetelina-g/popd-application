package app.popdapplication.service;

import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.repository.UserRepository;
import app.popdapplication.security.UserData;
import app.popdapplication.web.dto.EditProfileRequest;
import app.popdapplication.web.dto.RegisterRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WatchlistService watchlistService;
    private final SessionRegistry sessionRegistry;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, WatchlistService watchlistService, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.watchlistService = watchlistService;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Username not found"));

        return new UserData(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.isActive());
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {

        Optional<User> optionalUser = userRepository.findByUsername(registerRequest.getUsername());

        if (optionalUser.isPresent()) {
            throw new RuntimeException("User with [%s] username already exists".formatted(registerRequest.getUsername()));
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.USER)
                .active(true)
                .createdOn(LocalDateTime.now())
                .updatedOn(LocalDateTime.now())
                .build();

        userRepository.save(user);

        watchlistService.createDefaultWatchlist(user);
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User with [%s] id not found".formatted(userId)));
    }

    @Transactional
    public void updateProfile(UUID id, EditProfileRequest editProfileRequest) {

        User user = findById(id);

        user.setUsername(editProfileRequest.getUsername());
        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setEmail(editProfileRequest.getEmail());
        user.setProfilePicture(editProfileRequest.getProfilePictureUrl());

        userRepository.save(user);
    }

    public Page<User> findAll(PageRequest pageable) {
        return this.userRepository.findAll(pageable);
    }

    @Transactional
    public void switchStatus(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()){
            throw new RuntimeException("User with userId [%s] not found".formatted(userId));
        }

        User user = userOpt.get();

        user.setActive(!user.isActive());

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);

        if (!user.isActive()) {
            forceLogoutUser(user.getUsername());
        }
    }

    private void forceLogoutUser(String username) {
        sessionRegistry.getAllPrincipals().stream()
                .filter(p -> p instanceof UserData ud && ud.getUsername().equals(username))
                .forEach(principal -> {
                    List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (SessionInformation session : sessions) {
                        session.expireNow();
                    }
                });
    }

    @Transactional
    public void switchRole(UUID userId) {

        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()){
            throw new RuntimeException("User with userId [%s] not found".formatted(userId));
        }

        User user = userOpt.get();

        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.ADMIN);
        } else {
            user.setRole(UserRole.USER);
        }

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    public List<User> searchUsers(String query) {
        return userRepository.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, query);
    }

    public Map<UUID, String> getUsernamesByIds(Set<UUID> userIds) {
        Map<UUID, String> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (UUID userId : userIds) {
            if (userId == null || result.containsKey(userId)) {
                continue; // skip nulls and already processed users
            }

            try {
                User user = findById(userId);
                if (user != null) {
                    result.put(userId, user.getUsername());
                }
            } catch (Exception e) {
                log.warn("Could not fetch user with id {}: {}", userId, e.getMessage());
            }
        }

        return result;
    }
}
