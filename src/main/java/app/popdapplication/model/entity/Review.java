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
public class Review {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private User author;

    @ManyToOne(optional = false)
    private Movie movie;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false)
    private LocalDateTime editedOn;
}
