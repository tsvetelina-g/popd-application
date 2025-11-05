package app.popdapplication.web.dto;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditArtistRequest {

    @NotBlank
    private String name;

    @Nullable
    private LocalDate birthDate;

    @Nullable
    @URL
    private String imageUrl;

    @Nullable
    private String biography;
}
