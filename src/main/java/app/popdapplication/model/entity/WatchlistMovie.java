package app.popdapplication.model.entity;

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
public class WatchlistMovie {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Watchlist watchlist;

    @ManyToOne(optional = false)
    private Movie movie;

    @Column(name = "added_on", nullable = false)
    private LocalDateTime addedOn;
}
