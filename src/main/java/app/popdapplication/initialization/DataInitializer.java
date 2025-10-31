package app.popdapplication.initialization;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.enums.GenreType;
import app.popdapplication.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final GenreService genreService;

    @Override
    public void run(String... args) throws Exception {

        List<Genre> genres = genreService.findAll();

        if (genres.size() == 0) {
            for (GenreType genreType : GenreType.values()) {
                Genre genre = new Genre();
                genre.setName(genreType.getDisplayName());
                genreService.saveGenre(genre);
            }
        }
    }
}
