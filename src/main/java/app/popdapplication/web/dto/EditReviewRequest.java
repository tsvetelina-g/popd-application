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
public class EditReviewRequest {

    private String title;

    @NotBlank(message = "Content cannot be empty or contain only whitespace")
    private String content;
}
