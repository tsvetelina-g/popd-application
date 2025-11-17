package app.popdapplication.client;

import app.popdapplication.client.ReviewDto.ReviewRequest;
import app.popdapplication.client.ReviewDto.ReviewResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(value = "popd-review-svc", url = "http://localhost:8085/api/v1")
public interface ReviewClient {

    @PostMapping("/reviews")
    ResponseEntity<ReviewResponse> upsertReview(@RequestBody ReviewRequest requestBody);

    @GetMapping("/reviews/{userId}/{movieId}")
    ResponseEntity<ReviewResponse> getReviewByUserAndMovie(@PathVariable("userId") UUID userId, @PathVariable("movieId") UUID movieId);

    @DeleteMapping("/reviews/{userId}/{movieId}")
    ResponseEntity<Void> deleteReview(@PathVariable("userId") UUID userId, @PathVariable("movieId") UUID movieId);

    @GetMapping("reviews/{movieId}")
    ResponseEntity<List<ReviewResponse>> getLatestFiveReviewsForAMovie(@PathVariable("movieId") UUID movieId);

    @GetMapping("/reviews/{movieId}/page")
    ResponseEntity<Page<ReviewResponse>> getReviewsForMovie(
            @PathVariable("movieId") UUID movieId,
            @RequestParam("page") int page,
            @RequestParam("size") int size);

}
