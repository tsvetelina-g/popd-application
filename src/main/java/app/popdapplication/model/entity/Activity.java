package app.popdapplication.model.entity;

import app.popdapplication.model.enums.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(nullable = false)
    private boolean removed;

    @Column(nullable = true)
    private Integer rating;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;
}
