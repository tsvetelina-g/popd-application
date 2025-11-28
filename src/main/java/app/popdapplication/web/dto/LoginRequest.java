package app.popdapplication.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @Size(min = 3, max = 30)
    @NotBlank
    private String username;

    @Size(min = 8, max = 64)
    @NotBlank
    private String password;
}
