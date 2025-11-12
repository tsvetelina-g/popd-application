package app.popdapplication.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCreditRequest {

    @NotBlank(message = "Artist name is required")
    private String artistName;

    @NotBlank(message = "Role is required")
    private String role;

}
