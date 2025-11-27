package app.popdapplication.initialization;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.entity.User;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.model.enums.GenreType;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.property.ArtistProperties;
import app.popdapplication.property.CreditProperties;
import app.popdapplication.property.MovieProperties;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.repository.MovieCreditRepository;
import app.popdapplication.repository.MovieRepository;
import app.popdapplication.repository.UserRepository;
import app.popdapplication.service.GenreService;
import app.popdapplication.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final GenreService genreService;
    private final WatchlistService watchlistService;
    private final ArtistRepository artistRepository;
    private final MovieRepository movieRepository;
    private final MovieCreditRepository movieCreditRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ArtistProperties artistProperties;
    private final MovieProperties movieProperties;
    private final CreditProperties creditProperties;

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultAdmin();
        initializeGenres();
        initializeArtists();
        initializeMovies();
        initializeMovieCredits();
    }

    private void initializeDefaultAdmin() {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@popd.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .role(UserRole.ADMIN)
                    .active(true)
                    .createdOn(LocalDateTime.now())
                    .updatedOn(LocalDateTime.now())
                    .build();

            userRepository.save(admin);
            watchlistService.createDefaultWatchlist(admin);
            log.info("Default admin user created - username: admin, password: admin123");
        }
    }

    private void initializeGenres() {
        List<Genre> genres = genreService.findAll();
        if (genres.isEmpty()) {
            for (GenreType genreType : GenreType.values()) {
                Genre genre = new Genre();
                genre.setName(genreType.getDisplayName());
                genreService.saveGenre(genre);
            }
            log.info("Genres initialized successfully");
        }
    }

    private void initializeArtists() {
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
            log.info("Artists initialized successfully");
        }
    }

    private void initializeMovies() {
        if (movieRepository.count() == 0 && movieProperties.getMovies() != null) {
            Map<String, Genre> genreMap = genreService.findAll().stream()
                    .collect(Collectors.toMap(Genre::getName, genre -> genre));

            for (MovieProperties.MovieDetails movieDetails : movieProperties.getMovies()) {
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
            log.info("Movies initialized successfully");
        }
    }

    private void initializeMovieCredits() {
        if (movieCreditRepository.count() == 0 && creditProperties.getCredits() != null) {
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
                        log.warn("Invalid role: {} for {} in {}",
                                creditDetails.getRole(),
                                creditDetails.getArtistName(),
                                creditDetails.getMovieTitle());
                    }
                }
            }
            log.info("Movie credits initialized successfully");
        }
    }
}
