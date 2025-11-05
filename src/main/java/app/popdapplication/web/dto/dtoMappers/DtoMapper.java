package app.popdapplication.web.dto.dtoMappers;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Genre;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.User;
import app.popdapplication.web.dto.EditArtistRequest;
import app.popdapplication.web.dto.EditMovieRequest;
import app.popdapplication.web.dto.EditProfileRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static EditProfileRequest fromUser(User user) {
        return EditProfileRequest.builder()
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePicture())
                .build();
    }

    public static EditMovieRequest fromMovie(Movie movie) {

        return EditMovieRequest.builder()
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genresIds(movie.getGenres()
                                .stream()
                                .map(Genre::getId)
                                .toList()
                )
                .posterUrl(movie.getPosterUrl())
                .backgroundImage(movie.getBackgroundImage())
                .releaseDate(movie.getReleaseDate())
                .build();
    }

    public static EditArtistRequest fromArtist(Artist artist) {

        return EditArtistRequest.builder()
                .name(artist.getName())
                .biography(artist.getBiography())
                .birthDate(artist.getBirthDate())
                .imageUrl(artist.getImageUrl())
                .build();
    }

}
