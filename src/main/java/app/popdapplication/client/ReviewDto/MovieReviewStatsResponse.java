package app.popdapplication.client.ReviewDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieReviewStatsResponse {

    private Integer totalReviews;
}
