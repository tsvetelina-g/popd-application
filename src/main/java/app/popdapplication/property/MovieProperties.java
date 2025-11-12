package app.popdapplication.property;

import app.popdapplication.config.YamlPropertySourceFactory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.LocalDate;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties
@PropertySource(value = "movies-details.yaml", factory = YamlPropertySourceFactory.class)
public class MovieProperties {

    private List<MovieDetails> movies;

    @Data
    public static class MovieDetails {
        private String title;
        private String description;
        private LocalDate releaseDate;
        private List<String> genres;
        private String posterUrl;
        private String backgroundImage;
    }

}
