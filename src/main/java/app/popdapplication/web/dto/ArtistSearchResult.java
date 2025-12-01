package app.popdapplication.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtistSearchResult {
    private String name;
    private Integer birthYear;
}

