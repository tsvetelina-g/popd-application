package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.exception.AlreadyExistsException;
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
        User user = userRepository.findByUsername(username).orElseThrow(() -> new NotFoundException("User with username [%s] not found".formatted(username)));
        return new UserData(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.isActive());
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new AlreadyExistsException(
                    "User with username [%s] already exists".formatted(registerRequest.getUsername())
            );
        }

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new AlreadyExistsException(
                    "User with email [%s] already exists".formatted(registerRequest.getEmail())
            );
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
        log.info("User registered successfully with username: {}", registerRequest.getUsername());
    }

    public User findById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id [%s] not found".formatted(userId)));
    }

    @Transactional
    public void updateProfile(UUID id, EditProfileRequest editProfileRequest) {
        User user = findById(id);

        user.setUsername(editProfileRequest.getUsername());
        user.setFirstName(editProfileRequest.getFirstName());
        user.setLastName(editProfileRequest.getLastName());
        user.setEmail(editProfileRequest.getEmail());
        user.setProfilePicture(editProfileRequest.getProfilePictureUrl());
        user.setUpdatedOn(LocalDateTime.now());

        userRepository.save(user);
        log.info("Profile updated for user with id: {}", id);
    }

    public Page<User> findAll(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size < 1 || size > 50) {
            size = 10;
        }

        PageRequest pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void switchStatus(UUID userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new NotFoundException("User with id [%s] not found".formatted(userId));
        }

        User user = userOpt.get();

        user.setActive(!user.isActive());

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);

        if (!user.isActive()) {
            forceLogoutUser(user.getUsername());
            log.info("User status switched to inactive and forced logout for user with id: {}", userId);
        } else {
            log.info("User status switched to active for user with id: {}", userId);
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

        if (userOpt.isEmpty()) {
            throw new NotFoundException("User with id [%s] not found".formatted(userId));
        }

        User user = userOpt.get();

        if (user.getRole() == UserRole.USER) {
            user.setRole(UserRole.ADMIN);
            log.info("User role switched to ADMIN for user with id: {}", userId);
        } else {
            user.setRole(UserRole.USER);
            log.info("User role switched to USER for user with id: {}", userId);
        }

        user.setUpdatedOn(LocalDateTime.now());
        userRepository.save(user);
    }

    private List<User> searchUsers(String query) {
        return userRepository.findAllByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(query, query, query);
    }

    public List<User> searchUsersLimited(String query, int limit) {
        return searchUsers(query).stream()
                .limit(limit)
                .toList();
    }

    public Map<UUID, String> getUsernamesByIds(Set<UUID> userIds) {
        Map<UUID, String> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (UUID userId : userIds) {
            if (userId == null) {
                continue;
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
