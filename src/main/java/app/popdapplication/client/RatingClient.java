package app.popdapplication.client;

import app.popdapplication.client.dto.Rating;
import app.popdapplication.client.dto.RatingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(value = "popd-rating-svc", url = "http://localhost:8084/api/v1")
public interface RatingClient {

    @PostMapping("/ratings")
    ResponseEntity<Rating> upsertRating(@RequestBody RatingRequest requestBody);

    @GetMapping("/ratings/{userId}/{movieId}")
    ResponseEntity<Rating> getRatingByUserAndMovie(@PathVariable("userId") UUID userId, @PathVariable("movieId") UUID movieId);

    @DeleteMapping("/ratings/{userId}/{movieId}")
    void deleteRating(@PathVariable("userId") UUID userId, @PathVariable("movieId") UUID movieId);
}
