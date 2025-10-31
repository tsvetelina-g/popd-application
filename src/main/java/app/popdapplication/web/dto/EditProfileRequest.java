package app.popdapplication.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditProfileRequest {

    @Size(min = 3, max = 30)
    private String username;

    @Nullable
    private String firstName;

    @Nullable
    private String lastName;

    @Email
    private String email;

    @URL
    @Nullable
    private String profilePictureUrl;

}
