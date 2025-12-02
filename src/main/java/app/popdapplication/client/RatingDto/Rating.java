package app.popdapplication.client.RatingDto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class Rating {

    private int rating;

    private UUID userId;

    private UUID movieId;

    private LocalDateTime createdOn;

    private LocalDateTime updatedOn;
}
