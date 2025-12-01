package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Artist;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.web.dto.AddArtistRequest;
import app.popdapplication.web.dto.ArtistSearchResult;
import app.popdapplication.web.dto.EditArtistRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NotFoundException("Artist with id [%s] not found".formatted(artistId)));

        artist.setName(editArtistRequest.getName());
        artist.setBiography(editArtistRequest.getBiography());
        artist.setBirthDate(editArtistRequest.getBirthDate());
        artist.setImageUrl(editArtistRequest.getImageUrl());

        artistRepository.save(artist);
        log.info("Artist info updated for artist with id: {}", artistId);
    }

    private List<Artist> searchArtists(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return artistRepository.findAllByOrderByName();
        }
        
        String trimmedTerm = searchTerm.trim();
        String[] words = trimmedTerm.split("\\s+");
        
        if (words.length > 1) {
            List<Artist> exactMatch = artistRepository.findByNameContainingIgnoreCase(trimmedTerm);
            
            List<Artist> multiWordResults = artistRepository.findByNameContainingBothWords(words[0], words[1]);
            
            if (words.length > 2) {
                multiWordResults = multiWordResults.stream()
                        .filter(artist -> {
                            String lowerName = artist.getName().toLowerCase();
                            return Arrays.stream(words)
                                    .allMatch(word -> lowerName.contains(word.toLowerCase()));
                        })
                        .toList();
            }
            
            Set<UUID> seenIds = new HashSet<>();
            List<Artist> combined = new ArrayList<>();
            
            for (Artist artist : exactMatch) {
                if (seenIds.add(artist.getId())) {
                    combined.add(artist);
                }
            }
            
            for (Artist artist : multiWordResults) {
                if (seenIds.add(artist.getId())) {
                    combined.add(artist);
                }
            }
            
            return combined;
        }
        
        return artistRepository.findByNameContainingIgnoreCase(trimmedTerm);
    }
    
    public Artist findByName(String name) {
        return artistRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new NotFoundException("Artist with name [%s] not found".formatted(name)));
    }

    private List<Artist> searchByName(String query) {
        return artistRepository.findByNameContainingIgnoreCase(query);
    }

    public List<Artist> searchByNameLimited(String query, int limit) {
        return searchByName(query).stream()
                .limit(limit)
                .toList();
    }

    public List<ArtistSearchResult> searchArtistsWithBirthYear(String query, int limit) {
        List<Artist> artists = searchArtists(query);
        return artists.stream()
                .map(artist -> {
                    Integer birthYear = artist.getBirthDate() != null 
                            ? artist.getBirthDate().getYear() 
                            : null;
                    return new ArtistSearchResult(artist.getName(), birthYear);
                })
                .limit(limit)
                .toList();
    }
}
