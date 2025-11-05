package app.popdapplication.web.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditMovieRequest {

    @NotBlank
    private String title;

    @Nullable
    private String description;

    @Nullable
    private LocalDate releaseDate;

    @Nullable
    private List<UUID> genresIds = new ArrayList<>();

    @Nullable
    @URL
    private String posterUrl;

    @Nullable
    @URL
    private String backgroundImage;
}
