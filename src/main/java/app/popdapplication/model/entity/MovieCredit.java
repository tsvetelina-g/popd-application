package app.popdapplication.model.entity;

import app.popdapplication.model.enums.ArtistRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class MovieCredit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    private Movie movie;

    @ManyToOne(optional = false)
    private Artist artist;

    @Enumerated(EnumType.STRING)
    private ArtistRole roleType;
}
