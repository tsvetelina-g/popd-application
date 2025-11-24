package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Artist;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.web.dto.AddArtistRequest;
import app.popdapplication.web.dto.EditArtistRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ArtistService {

    private final ArtistRepository artistRepository;

    @Autowired
    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @CacheEvict(value = "artists", allEntries = true)
    public Artist addArtist(AddArtistRequest addArtistRequest) {

        Artist artist = Artist.builder()
                .name(addArtistRequest.getName())
                .birthDate(addArtistRequest.getBirthDate())
                .imageUrl(addArtistRequest.getImageUrl())
                .biography(addArtistRequest.getBiography())
                .build();

        Artist savedArtist = artistRepository.save(artist);
        log.info("Artist added successfully with id: {} and name: {}", savedArtist.getId(), savedArtist.getName());
        return savedArtist;
    }

    @Cacheable(value = "artists", key = "#artistId")
    public Artist findById(UUID artistId) {
        return artistRepository.findById(artistId).orElseThrow(() -> new NotFoundException("Artist with id [%s] not found".formatted(artistId)));
    }

    @CacheEvict(value = "artists", key = "#artistId")
    public void updateArtistInfo(UUID artistId, EditArtistRequest editArtistRequest) {

        Artist artist = findById(artistId);

        artist.setName(editArtistRequest.getName());
        artist.setBiography(editArtistRequest.getBiography());
        artist.setBirthDate(editArtistRequest.getBirthDate());
        artist.setImageUrl(editArtistRequest.getImageUrl());

        artistRepository.save(artist);
        log.info("Artist info updated for artist with id: {}", artistId);
    }

    public List<Artist> searchArtists(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return artistRepository.findAllByOrderByName();
        }
        return artistRepository.findByNameContainingIgnoreCase(searchTerm.trim());
    }
    
    public Artist findByName(String name) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("Artist with name [%s] not found".formatted(name)));
    }

    public List<Artist> searchByName(String query) {
        return artistRepository.findByNameContainingIgnoreCase(query);
    }
}
