package app.popdapplication.service;

import app.popdapplication.exception.NotFoundException;
import app.popdapplication.model.entity.Artist;
import app.popdapplication.repository.ArtistRepository;
import app.popdapplication.web.dto.AddArtistRequest;
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
    void whenSearchArtistNames_andQueryIsProvided_thenReturnLimitedListOfArtistNames() {
        List<Artist> artists = List.of(
                Artist.builder().name("Actor 1").build(),
                Artist.builder().name("Actor 2").build(),
                Artist.builder().name("Actor 3").build()
        );
        when(artistRepository.findByNameContainingIgnoreCase("Actor")).thenReturn(artists);

        List<String> result = artistService.searchArtistNames("Actor", 2);

        assertEquals(2, result.size());
        assertEquals("Actor 1", result.get(0));
        assertEquals("Actor 2", result.get(1));
    }

    @Test
    void whenSearchArtistNames_andQueryIsNull_thenReturnAllArtistNamesUpToLimit() {
        List<Artist> allArtists = List.of(
                Artist.builder().name("Actor 1").build(),
                Artist.builder().name("Actor 2").build(),
                Artist.builder().name("Actor 3").build()
        );
        when(artistRepository.findAllByOrderByName()).thenReturn(allArtists);

        List<String> result = artistService.searchArtistNames(null, 2);

        assertEquals(2, result.size());
    }

    @Test
    void whenSearchArtistNames_andQueryIsEmptyString_thenReturnAllArtistNamesUpToLimit() {
        List<Artist> allArtists = List.of(
                Artist.builder().name("Actor 1").build(),
                Artist.builder().name("Actor 2").build()
        );
        when(artistRepository.findAllByOrderByName()).thenReturn(allArtists);

        List<String> result = artistService.searchArtistNames("", 5);

        assertEquals(2, result.size());
    }

    @Test
    void whenSearchArtistNames_andQueryIsWhitespaceOnly_thenTreatAsEmptyAndReturnAllArtistNames() {
        List<Artist> allArtists = List.of(
                Artist.builder().name("Actor 1").build()
        );
        when(artistRepository.findAllByOrderByName()).thenReturn(allArtists);

        List<String> result = artistService.searchArtistNames("   ", 10);

        assertEquals(1, result.size());
        assertEquals("Actor 1", result.get(0));
    }

    @Test
    void whenSearchArtistNames_andNoArtistsMatchQuery_thenReturnEmptyList() {
        when(artistRepository.findByNameContainingIgnoreCase("NoMatch")).thenReturn(Collections.emptyList());

        List<String> result = artistService.searchArtistNames("NoMatch", 10);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
