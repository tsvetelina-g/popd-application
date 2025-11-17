package app.popdapplication.client.RatingDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRatingStatsResponse {

   private Integer ratedMovies;
}
