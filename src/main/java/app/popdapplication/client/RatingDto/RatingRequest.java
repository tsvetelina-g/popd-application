package app.popdapplication.client.RatingDto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RatingRequest {

    private UUID userId;

    private UUID movieId;

    private int value;
}
