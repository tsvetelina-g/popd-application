package app.popdapplication.service;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.repository.GenreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GenreService {

    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    public void saveGenre(Genre genre) {
        genreRepository.save(genre);
    }

    public List<Genre> findAllById(List<UUID> genresIds) {
        return genreRepository.findAllById(genresIds);
    }
}
