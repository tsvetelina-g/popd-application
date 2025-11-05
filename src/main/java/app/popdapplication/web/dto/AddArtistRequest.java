package app.popdapplication.web.dto;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddArtistRequest {

    @NotBlank
    private String name;

    @Nullable
    private LocalDate birthDate;

    @Nullable
    private String imageUrl;

    @Nullable
    private String biography;
}
