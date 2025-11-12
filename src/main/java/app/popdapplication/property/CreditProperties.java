package app.popdapplication.property;

import app.popdapplication.config.YamlPropertySourceFactory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties
@PropertySource(value = "credits-details.yaml", factory = YamlPropertySourceFactory.class)
public class CreditProperties {

    private List<CreditDetails> credits;

    @Data
    public static class CreditDetails {
        private String movieTitle;
        private String artistName;
        private String role;
    }

}

