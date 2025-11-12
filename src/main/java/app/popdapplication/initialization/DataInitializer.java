package app.popdapplication.initialization;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.model.enums.GenreType;
import app.popdapplication.property.ArtistProperties;
import app.popdapplication.property.CreditProperties;
import app.popdapplication.property.MovieProperties;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.repository.MovieCreditRepository;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final GenreService genreService;
    private final ArtistRepository artistRepository;
    private final MovieRepository movieRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final ArtistProperties artistProperties;
    private final MovieProperties movieProperties;
    private final CreditProperties creditProperties;

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

        // Load artists if database is empty
        if (artistRepository.count() == 0 && artistProperties.getArtists() != null) {
            for (ArtistProperties.ArtistDetails artistDetails : artistProperties.getArtists()) {
                Artist artist = Artist.builder()
                        .name(artistDetails.getName())
                        .birthDate(artistDetails.getBirthDate())
                        .imageUrl(artistDetails.getImageUrl())
                        .biography(artistDetails.getBiography())
                        .build();
                artistRepository.save(artist);
            }
        }

        // Load movies if database is empty
        if (movieRepository.count() == 0 && movieProperties.getMovies() != null) {
            // Create a map of genre names to Genre entities for quick lookup
            Map<String, Genre> genreMap = genreService.findAll().stream()
                    .collect(Collectors.toMap(Genre::getName, genre -> genre));

            for (MovieProperties.MovieDetails movieDetails : movieProperties.getMovies()) {
                // Map genre names to Genre entities
                List<Genre> movieGenres = movieDetails.getGenres() != null ?
                        movieDetails.getGenres().stream()
                                .map(genreMap::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()) : new ArrayList<>();

                Movie movie = Movie.builder()
                        .title(movieDetails.getTitle())
                        .description(movieDetails.getDescription())
                        .releaseDate(movieDetails.getReleaseDate())
                        .genres(movieGenres)
                        .posterUrl(movieDetails.getPosterUrl())
                        .backgroundImage(movieDetails.getBackgroundImage())
                        .build();
                movieRepository.save(movie);
            }
        }

        // Load credits if database is empty
        if (movieCreditRepository.count() == 0 && creditProperties.getCredits() != null) {
            // Create maps for quick lookup
            Map<String, Movie> movieMap = movieRepository.findAll().stream()
                    .collect(Collectors.toMap(Movie::getTitle, movie -> movie));
            Map<String, Artist> artistMap = artistRepository.findAll().stream()
                    .collect(Collectors.toMap(Artist::getName, artist -> artist));

            for (CreditProperties.CreditDetails creditDetails : creditProperties.getCredits()) {
                Movie movie = movieMap.get(creditDetails.getMovieTitle());
                Artist artist = artistMap.get(creditDetails.getArtistName());

                if (movie != null && artist != null) {
                    try {
                        ArtistRole roleType = ArtistRole.valueOf(creditDetails.getRole());
                        MovieCredit movieCredit = MovieCredit.builder()
                                .movie(movie)
                                .artist(artist)
                                .roleType(roleType)
                                .build();
                        movieCreditRepository.save(movieCredit);
                    } catch (IllegalArgumentException e) {
                        // Skip if role doesn't match any enum value
                        System.err.println("Invalid role: " + creditDetails.getRole() + " for " + creditDetails.getArtistName() + " in " + creditDetails.getMovieTitle());
                    }
                }
            }
        }

    }
}
