package app.popdapplication.client.ReviewDto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {

    private UUID userId;

    private UUID movieId;

    private Integer rating;

    private String title;

    private String content;

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;
}
