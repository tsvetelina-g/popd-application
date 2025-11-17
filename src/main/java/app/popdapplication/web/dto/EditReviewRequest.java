package app.popdapplication.web.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditReviewRequest {

    @Column(nullable = true)
    private String title;

    @Column(nullable = false)
    private String content;
}
