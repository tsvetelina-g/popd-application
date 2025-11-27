package app.popdapplication.web;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.service.ArtistService;
import app.popdapplication.service.MovieCreditService;
import app.popdapplication.web.dto.AddArtistRequest;
import app.popdapplication.web.dto.EditArtistRequest;
import app.popdapplication.web.dto.dtoMappers.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/artist")
public class ArtistController {

    private final ArtistService artistService;
    private final MovieCreditService movieCreditService;

    public ArtistController(ArtistService artistService, MovieCreditService movieCreditService) {
        this.artistService = artistService;
        this.movieCreditService = movieCreditService;
    }

    @GetMapping("/add")
    public ModelAndView getAddArtistPage() {
        ModelAndView modelAndView = new ModelAndView("artist-add");

        modelAndView.addObject("addArtistRequest", new AddArtistRequest());

        return modelAndView;
    }

    @PostMapping("/add")
    public ModelAndView addArtist(@Valid AddArtistRequest addArtistRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ModelAndView("artist-add");
        }

        Artist artist = artistService.addArtist(addArtistRequest);

        return new ModelAndView("redirect:/artist/" + artist.getId());
    }

    @GetMapping("/{artistId}")
    public ModelAndView getArtistPage(@PathVariable UUID artistId) {
        ModelAndView modelAndView = new ModelAndView("artist");
        Artist artist = artistService.findById(artistId);
        int movieCreditsCount = movieCreditService.findAllCreditsByArtist(artist);
        Map<ArtistRole, List<MovieCredit>> creditsByRole = movieCreditService.getCreditsByArtistGrouped(artist);

        modelAndView.addObject("artist", artist);
        modelAndView.addObject("movieCreditsCount", movieCreditsCount);
        modelAndView.addObject("creditsByRole", creditsByRole);

        return modelAndView;
    }

    @GetMapping("/{artistId}/edit")
    public ModelAndView getEditArtistPage(@PathVariable UUID artistId) {
        ModelAndView modelAndView = new ModelAndView("artist-edit");
        Artist artist = artistService.findById(artistId);
        EditArtistRequest editArtistRequest = DtoMapper.fromArtist(artist);

        modelAndView.addObject("artist", artist);
        modelAndView.addObject("editArtistRequest", editArtistRequest);

        return modelAndView;
    }

    @PutMapping("/{artistId}/edit")
    public ModelAndView editArtist(@Valid EditArtistRequest editArtistRequest, BindingResult bindingResult, @PathVariable UUID artistId) {
        Artist artist = artistService.findById(artistId);
        if (bindingResult.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("artist-edit");
            modelAndView.addObject("artist", artist);
            return modelAndView;
        }

        artistService.updateArtistInfo(artistId, editArtistRequest);

        return new ModelAndView("redirect:/artist/" + artistId);
    }
}
