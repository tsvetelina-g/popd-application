package app.popdapplication.service;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GenreServiceUTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

    @Test
    void whenFindAll_thenReturnAllGenres() {
        List<Genre> genres = List.of(
                Genre.builder().id(UUID.randomUUID()).name("Action").build(),
                Genre.builder().id(UUID.randomUUID()).name("Comedy").build()
        );
        when(genreRepository.findAll()).thenReturn(genres);

        List<Genre> result = genreService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Action", result.get(0).getName());
        assertEquals("Comedy", result.get(1).getName());
        verify(genreRepository).findAll();
    }

    @Test
    void whenFindAll_andNoGenresExist_thenReturnEmptyList() {
        when(genreRepository.findAll()).thenReturn(Collections.emptyList());

        List<Genre> result = genreService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(genreRepository).findAll();
    }

    @Test
    void whenSaveGenre_thenCallRepositorySave() {
        Genre genre = Genre.builder()
                .id(UUID.randomUUID())
                .name("Drama")
                .build();

        genreService.saveGenre(genre);

        verify(genreRepository).save(genre);
    }

    @Test
    void whenFindAllById_andGenresExist_thenReturnGenres() {
        UUID genreId1 = UUID.randomUUID();
        UUID genreId2 = UUID.randomUUID();
        List<UUID> genreIds = List.of(genreId1, genreId2);
        List<Genre> genres = List.of(
                Genre.builder().id(genreId1).name("Action").build(),
                Genre.builder().id(genreId2).name("Comedy").build()
        );
        when(genreRepository.findAllById(genreIds)).thenReturn(genres);

        List<Genre> result = genreService.findAllById(genreIds);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(genreId1, result.get(0).getId());
        assertEquals(genreId2, result.get(1).getId());
        verify(genreRepository).findAllById(genreIds);
    }

    @Test
    void whenFindAllById_andEmptyList_thenReturnEmptyList() {
        List<UUID> emptyList = Collections.emptyList();
        when(genreRepository.findAllById(emptyList)).thenReturn(Collections.emptyList());

        List<Genre> result = genreService.findAllById(emptyList);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(genreRepository).findAllById(emptyList);
    }

    @Test
    void whenFindById_andGenreExists_thenReturnOptionalWithGenre() {
        UUID genreId = UUID.randomUUID();
        Genre genre = Genre.builder()
                .id(genreId)
                .name("Thriller")
                .build();
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));

        Optional<Genre> result = genreService.findById(genreId);

        assertTrue(result.isPresent());
        assertEquals(genreId, result.get().getId());
        assertEquals("Thriller", result.get().getName());
        verify(genreRepository).findById(genreId);
    }

    @Test
    void whenFindById_andGenreDoesNotExist_thenReturnEmptyOptional() {
        UUID genreId = UUID.randomUUID();
        when(genreRepository.findById(genreId)).thenReturn(Optional.empty());

        Optional<Genre> result = genreService.findById(genreId);

        assertTrue(result.isEmpty());
        verify(genreRepository).findById(genreId);
    }
}

