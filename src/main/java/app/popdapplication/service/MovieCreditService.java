package app.popdapplication.service;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.repository.MovieCreditRepository;
import app.popdapplication.web.dto.AddCreditRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MovieCreditService {

    private final MovieCreditRepository movieCreditRepository;
    private final MovieService movieService;
    private final ArtistService artistService;

    @Autowired
    public MovieCreditService(MovieCreditRepository movieCreditRepository, MovieService movieService, ArtistService artistService) {
        this.movieCreditRepository = movieCreditRepository;
        this.movieService = movieService;
        this.artistService = artistService;
    }

    public void saveCredit(AddCreditRequest addCreditRequest, UUID movieId) {

        Movie movie = movieService.findById(movieId);
        Artist artist = artistService.findByName(addCreditRequest.getArtistName());
        ArtistRole artistRole = ArtistRole.valueOf(addCreditRequest.getRole());

        // Check if credit already exists
        if (movieCreditRepository.findByMovieAndArtistAndRoleType(movie, artist, artistRole).isPresent()) {
            throw new RuntimeException("Credit already exists: " + artist.getName() + " as " + artistRole.getDisplayName() + " for movie '" + movie.getTitle() + "'");
        }

        MovieCredit movieCredit = MovieCredit.builder()
                .movie(movie)
                .artist(artist)
                .roleType(artistRole)
                .build();

        movieCreditRepository.save(movieCredit);
    }

    public List<MovieCredit> getCreditsByMovie(Movie movie) {
        return movieCreditRepository.findByMovie(movie);
    }

    public MovieCredit findCreditById(UUID creditId) {
        return movieCreditRepository.findById(creditId).orElseThrow(() -> new RuntimeException("Credit with id [%s] does not exist".formatted(creditId)));
    }

    public void deleteCredit(MovieCredit movieCredit) {
        movieCreditRepository.delete(movieCredit);
    }

    public int findAllCreditsByArtist(Artist artist) {
        return movieCreditRepository.findAllByArtist(artist).size();
    }

    public Map<ArtistRole, List<MovieCredit>> getCreditsByArtistGrouped(Artist artist) {
        List<MovieCredit> credits = movieCreditRepository.findAllByArtist(artist);

        Map<ArtistRole, List<MovieCredit>> creditsByRole = credits.stream()
                .collect(Collectors.groupingBy(
                        MovieCredit::getRoleType,
                        () -> new EnumMap<>(ArtistRole.class),
                        Collectors.toList()
                ));

        Comparator<MovieCredit> byReleaseDateDesc = Comparator.comparing(
                (MovieCredit credit) -> credit.getMovie().getReleaseDate(),
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        creditsByRole.values()
                .forEach(list -> list.sort(byReleaseDateDesc));

        return creditsByRole;
    }
}
