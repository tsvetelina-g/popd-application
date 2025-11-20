package app.popdapplication.service;

import app.popdapplication.model.entity.Genre;
import app.popdapplication.repository.GenreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GenreService {

    private final GenreRepository genreRepository;

    @Autowired
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
