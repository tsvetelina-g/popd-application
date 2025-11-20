package app.popdapplication.event;

import app.popdapplication.model.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDtoEvent {

    private UUID userId;

    private UUID movieId;

    private ActivityType type;

    private boolean removed;

    private Integer rating;

    private LocalDateTime createdOn;
}
