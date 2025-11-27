package app.popdapplication.web.dto.dtoMappers;

import app.popdapplication.model.entity.User;
import app.popdapplication.web.dto.EditProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class DtoMapperUTest {

    @Test
    void whenMapFromUser_andUserIsNotNull_thenMapsToEditProfileRequest() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("StanB")
                .firstName("Stanimir")
                .lastName("Ivanov")
                .email("stan@gmail.com")
                .profilePicture(null)
                .build();

        EditProfileRequest dto = DtoMapper.fromUser(user);

        assertEquals("StanB", dto.getUsername());
        assertEquals("Stanimir", dto.getFirstName());
        assertEquals("Ivanov", dto.getLastName());
        assertEquals("stan@gmail.com", dto.getEmail());
        assertNull(dto.getProfilePictureUrl());
    }
}

