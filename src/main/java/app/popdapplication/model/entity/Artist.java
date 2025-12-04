package app.popdapplication.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column
    private LocalDate birthDate;

    @Column
    private String imageUrl;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String biography;
}
