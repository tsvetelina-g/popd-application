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
@PropertySource(value = "artists-details.yaml", factory = YamlPropertySourceFactory.class)
public class ArtistProperties {

    private List<ArtistDetails> artists;

    @Data
    public static class ArtistDetails {
        private String name;
        private LocalDate birthDate;
        private String imageUrl;
        private String biography;
    }

}
