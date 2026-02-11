package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Artist;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.web.dto.AddArtistRequest;
import app.popdapplication.web.dto.ArtistSearchResult;
import app.popdapplication.web.dto.EditArtistRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ArtistServiceUTest {

    @Mock
    private ArtistRepository artistRepository;

    @InjectMocks
    private ArtistService artistService;

    @Test
    void whenAddArtist_andRequestHasAllFields_thenBuildArtistAndSaveToRepositoryAndReturnSavedArtist() {
        AddArtistRequest request = AddArtistRequest.builder()
                .name("Actor")
                .birthDate(LocalDate.of(1934, 8, 25))
                .imageUrl("www.image.com")
                .biography("Biography")
                .build();
        Artist savedArtist = Artist.builder()
                .id(UUID.randomUUID())
                .name("Actor")
                .birthDate(LocalDate.of(1934, 8, 25))
                .imageUrl("www.image.com")
                .biography("Biography")
                .build();

        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);

        Artist result = artistService.addArtist(request);

        assertNotNull(result);
        assertEquals("Actor", result.getName());
        assertEquals(LocalDate.of(1934, 8, 25), result.getBirthDate());
        assertEquals("www.image.com", result.getImageUrl());
        assertEquals("Biography", result.getBiography());
        verify(artistRepository).save(any(Artist.class));
    }

    @Test
    void whenAddArtist_andRequestHasOnlyRequiredNameField_thenSaveArtistWithNullOptionalFields() {
        AddArtistRequest request = AddArtistRequest.builder()
                .name("Actor")
                .birthDate(null)
                .imageUrl(null)
                .biography(null)
                .build();
        Artist savedArtist = Artist.builder()
                .id(UUID.randomUUID())
                .name("Actor")
                .build();

        when(artistRepository.save(any(Artist.class))).thenReturn(savedArtist);

        Artist result = artistService.addArtist(request);

        assertNotNull(result);
        assertEquals("Actor", result.getName());

        ArgumentCaptor<Artist> artistCaptor = ArgumentCaptor.forClass(Artist.class);
        verify(artistRepository).save(artistCaptor.capture());
        Artist capturedArtist = artistCaptor.getValue();
        assertNull(capturedArtist.getBirthDate());
        assertNull(capturedArtist.getImageUrl());
        assertNull(capturedArtist.getBiography());
    }

    @Test
    void whenFindById_andArtistExistsInRepository_thenReturnArtist() {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("Artist")
                .build();
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));

        Artist result = artistService.findById(artistId);

        assertNotNull(result);
        assertEquals(artistId, result.getId());
        assertEquals("Artist", result.getName());
    }

    @Test
    void whenFindById_andArtistDoesNotExistInRepository_thenThrowsException() {
        UUID artistId = UUID.randomUUID();
        when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> artistService.findById(artistId)
        );

        assertTrue(exception.getMessage().contains(artistId.toString()));
    }

    @Test
    void whenUpdateArtistInfo_andArtistExists_thenUpdateAllFieldsAndSaveToRepository() {
        UUID artistId = UUID.randomUUID();
        Artist existingArtist = Artist.builder()
                .id(artistId)
                .name("Old Name")
                .biography("Old biography")
                .birthDate(LocalDate.of(1980, 1, 1))
                .imageUrl("www.image.com")
                .build();
        EditArtistRequest request = EditArtistRequest.builder()
                .name("New Name")
                .biography("Biography")
                .birthDate(LocalDate.of(1985, 5, 15))
                .imageUrl("www.new-image.com")
                .build();

        when(artistRepository.findById(artistId)).thenReturn(Optional.of(existingArtist));

        artistService.updateArtistInfo(artistId, request);

        assertEquals("New Name", existingArtist.getName());
        assertEquals("Biography", existingArtist.getBiography());
        assertEquals(LocalDate.of(1985, 5, 15), existingArtist.getBirthDate());
        assertEquals("www.new-image.com", existingArtist.getImageUrl());
        verify(artistRepository).save(existingArtist);
    }

    @Test
    void whenUpdateArtistInfo_andArtistDoesNotExist_thenThrowNotFoundExceptionAndDoNotSave() {
        UUID artistId = UUID.randomUUID();
        EditArtistRequest request = EditArtistRequest.builder()
                .name("Artist")
                .build();

        when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> artistService.updateArtistInfo(artistId, request));
        verify(artistRepository, never()).save(any());
    }

    @Test
    void whenFindByName_andArtistWithExactNameExists_thenReturnArtist() {
        String artistName = "Artist";
        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name(artistName)
                .build();
        when(artistRepository.findByNameIgnoreCase(artistName)).thenReturn(Optional.of(artist));

        Artist result = artistService.findByName(artistName);

        assertNotNull(result);
        assertEquals(artistName, result.getName());
    }

    @Test
    void whenFindByName_andArtistDoesNotExist_thenThrowsException() {
        String artistName = "Artist";
        when(artistRepository.findByNameIgnoreCase(artistName)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> artistService.findByName(artistName)
        );

        assertTrue(exception.getMessage().contains(artistName));
    }

    @Test
    void whenSearchByNameLimited_andMultipleArtistsMatch_thenReturnLimitedListOfArtists() {
        List<Artist> artists = List.of(
                Artist.builder().name("Actor 1").build(),
                Artist.builder().name("Actor 2").build(),
                Artist.builder().name("Actor 3").build(),
                Artist.builder().name("Actor 4").build()
        );
        when(artistRepository.findByNameContainingIgnoreCase("Actor")).thenReturn(artists);

        List<Artist> result = artistService.searchByNameLimited("Actor", 2);

        assertEquals(2, result.size());
        assertEquals("Actor 1", result.get(0).getName());
        assertEquals("Actor 2", result.get(1).getName());
    }

    @Test
    void whenSearchByNameLimited_andFewerArtistsThanLimit_thenReturnAllMatchingArtists() {
        List<Artist> artists = List.of(
                Artist.builder().name("Actor Actor").build()
        );
        when(artistRepository.findByNameContainingIgnoreCase("Actor")).thenReturn(artists);

        List<Artist> result = artistService.searchByNameLimited("Actor", 10);

        assertEquals(1, result.size());
    }

    @Test
    void whenSearchByNameLimited_andNoArtistsMatch_thenReturnEmptyList() {
        when(artistRepository.findByNameContainingIgnoreCase("Actor")).thenReturn(Collections.emptyList());

        List<Artist> result = artistService.searchByNameLimited("Actor", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void whenSearchArtistsWithBirthYear_withEmptyQuery_thenReturnAllArtists() {
        Artist artist1 = Artist.builder().id(UUID.randomUUID()).name("Artist A").birthDate(LocalDate.of(1980, 1, 1)).build();
        Artist artist2 = Artist.builder().id(UUID.randomUUID()).name("Artist B").build();
        when(artistRepository.findAllByOrderByName()).thenReturn(List.of(artist1, artist2));

        List<ArtistSearchResult> result = artistService.searchArtistsWithBirthYear("", 10);

        assertEquals(2, result.size());
        assertEquals("Artist A", result.get(0).getName());
        assertEquals(1980, result.get(0).getBirthYear());
        assertEquals("Artist B", result.get(1).getName());
        assertNull(result.get(1).getBirthYear());
    }

    @Test
    void whenSearchArtistsWithBirthYear_withMultiWordQuery_thenReturnCombinedResults() {
        Artist artist1 = Artist.builder().id(UUID.randomUUID()).name("John Smith").birthDate(LocalDate.of(1980, 1, 1)).build();
        Artist artist2 = Artist.builder().id(UUID.randomUUID()).name("Smith John").birthDate(LocalDate.of(1990, 5, 10)).build();
        when(artistRepository.findByNameContainingIgnoreCase("John Smith")).thenReturn(List.of(artist1));
        when(artistRepository.findByNameContainingBothWords("John", "Smith")).thenReturn(List.of(artist1, artist2));

        List<ArtistSearchResult> result = artistService.searchArtistsWithBirthYear("John Smith", 10);

        assertEquals(2, result.size());
        verify(artistRepository).findByNameContainingIgnoreCase("John Smith");
        verify(artistRepository).findByNameContainingBothWords("John", "Smith");
    }

    @Test
    void whenSearchArtistsWithBirthYear_withThreeWordQuery_thenFilterResults() {
        Artist artist1 = Artist.builder().id(UUID.randomUUID()).name("John Michael Smith").birthDate(LocalDate.of(1980, 1, 1)).build();
        Artist artist2 = Artist.builder().id(UUID.randomUUID()).name("John Smith").birthDate(LocalDate.of(1990, 5, 10)).build();
        when(artistRepository.findByNameContainingIgnoreCase("John Michael Smith")).thenReturn(List.of(artist1));
        when(artistRepository.findByNameContainingBothWords("John", "Michael")).thenReturn(List.of(artist1, artist2));

        List<ArtistSearchResult> result = artistService.searchArtistsWithBirthYear("John Michael Smith", 10);

        assertEquals(1, result.size());
        assertEquals("John Michael Smith", result.get(0).getName());
    }

    @Test
    void whenSearchArtistsWithBirthYear_withLimit_thenReturnLimitedResults() {
        Artist artist1 = Artist.builder().id(UUID.randomUUID()).name("Artist 1").birthDate(LocalDate.of(1980, 1, 1)).build();
        Artist artist2 = Artist.builder().id(UUID.randomUUID()).name("Artist 2").birthDate(LocalDate.of(1990, 5, 10)).build();
        Artist artist3 = Artist.builder().id(UUID.randomUUID()).name("Artist 3").build();
        when(artistRepository.findByNameContainingIgnoreCase("Artist")).thenReturn(List.of(artist1, artist2, artist3));

        List<ArtistSearchResult> result = artistService.searchArtistsWithBirthYear("Artist", 2);

        assertEquals(2, result.size());
    }
}
