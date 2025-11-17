package app.popdapplication.service;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.web.dto.AddArtistRequest;
import app.popdapplication.web.dto.EditArtistRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ArtistService {

    private final ArtistRepository artistRepository;

    @Autowired
    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    public Artist addArtist(AddArtistRequest addArtistRequest) {

        Artist artist = Artist.builder()
                .name(addArtistRequest.getName())
                .birthDate(addArtistRequest.getBirthDate())
                .imageUrl(addArtistRequest.getImageUrl())
                .biography(addArtistRequest.getBiography())
                .build();

        return artistRepository.save(artist);
    }

    public Artist findById(UUID artistId) {
        return artistRepository.findById(artistId).orElseThrow(() -> new RuntimeException("Artist with [%s] id not found".formatted(artistId)));
    }

    public void updateArtistInfo(UUID artistId, EditArtistRequest editArtistRequest) {

        Artist artist = findById(artistId);

        artist.setName(editArtistRequest.getName());
        artist.setBiography(editArtistRequest.getBiography());
        artist.setBirthDate(editArtistRequest.getBirthDate());
        artist.setImageUrl(editArtistRequest.getImageUrl());

        artistRepository.save(artist);
    }

    public List<Artist> searchArtists(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return artistRepository.findAllByOrderByName();
        }
        return artistRepository.findByNameContainingIgnoreCase(searchTerm.trim());
    }
    
    public Artist findByName(String name) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Artist with name [%s] not found".formatted(name)));
    }

    public List<Artist> searchByName(String query) {
        return artistRepository.findByNameContainingIgnoreCase(query);
    }
}
