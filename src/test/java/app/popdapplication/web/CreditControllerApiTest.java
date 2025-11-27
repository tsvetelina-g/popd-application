package app.popdapplication.web;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.entity.Movie;
import app.popdapplication.model.entity.MovieCredit;
import app.popdapplication.model.enums.ArtistRole;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.ArtistService;
import app.popdapplication.service.MovieCreditService;
import app.popdapplication.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CreditController.class)
public class CreditControllerApiTest {

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private MovieCreditService movieCreditService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAddCreditPage_andUserIsAdmin_thenReturn200WithCreditAddView() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);

        MockHttpServletRequestBuilder httpRequest = get("/credit/{movieId}/add", movieId)
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("credit-add"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("addCreditRequest"))
                .andExpect(model().attributeExists("roles"));

        verify(movieService).findById(movieId);
    }

    @Test
    void getAddCreditPage_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/credit/{movieId}/add", movieId)
                .with(user(regularUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError())
                .andExpect(view().name("not-found"));

        verify(movieService, never()).findById(any());
    }

    @Test
    void getAddCreditPage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/credit/{movieId}/add", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieService, never()).findById(any());
    }

    @Test
    void searchArtists_andUserIsAuthenticated_thenReturnArtistNames() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        List<String> artistNames = List.of("Artist 1", "Artist 2", "Artist 3");

        when(artistService.searchArtistNames("Artist", 50)).thenReturn(artistNames);

        MockHttpServletRequestBuilder httpRequest = get("/credit/artists/search")
                .param("query", "Artist")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(content().json("[\"Artist 1\",\"Artist 2\",\"Artist 3\"]"));

        verify(artistService).searchArtistNames("Artist", 50);
    }

    @Test
    void searchArtists_andEmptyQuery_thenReturnResultsWithEmptyQuery() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        List<String> artistNames = List.of("Artist 1", "Artist 2");

        when(artistService.searchArtistNames("", 50)).thenReturn(artistNames);

        MockHttpServletRequestBuilder httpRequest = get("/credit/artists/search")
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk());

        verify(artistService).searchArtistNames("", 50);
    }

    @Test
    void searchArtists_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        MockHttpServletRequestBuilder httpRequest = get("/credit/artists/search")
                .param("query", "Artist");

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(artistService, never()).searchArtistNames(any(), anyInt());
    }

    @Test
    void postAddCredit_andUserIsAdmin_andValidData_thenRedirectToCreditEdit() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);

        MockHttpServletRequestBuilder httpRequest = post("/credit/{movieId}/add", movieId)
                .param("artistName", "Artist")
                .param("role", "ACTOR")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/credit/" + movieId + "/edit"));

        verify(movieCreditService).saveCredit(any(), eq(movieId));
    }

    @Test
    void postAddCredit_andUserIsAdmin_andBlankArtistName_thenReturnCreditAddViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);

        MockHttpServletRequestBuilder httpRequest = post("/credit/{movieId}/add", movieId)
                .param("artistName", "")
                .param("role", "ACTOR")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("credit-add"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("roles"));

        verify(movieCreditService, never()).saveCredit(any(), any());
    }

    @Test
    void postAddCredit_andUserIsAdmin_andBlankRole_thenReturnCreditAddViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);

        when(movieService.findById(movieId)).thenReturn(movie);

        MockHttpServletRequestBuilder httpRequest = post("/credit/{movieId}/add", movieId)
                .param("artistName", "Artist")
                .param("role", "")
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("credit-add"));

        verify(movieCreditService, never()).saveCredit(any(), any());
    }

    @Test
    void postAddCredit_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/credit/{movieId}/add", movieId)
                .param("artistName", "Artist")
                .param("role", "ACTOR")
                .with(user(regularUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(movieCreditService, never()).saveCredit(any(), any());
    }

    @Test
    void postAddCredit_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = post("/credit/{movieId}/add", movieId)
                .param("artistName", "Artist")
                .param("role", "ACTOR")
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieCreditService, never()).saveCredit(any(), any());
    }

    @Test
    void getEditCreditsPage_andUserIsAuthenticated_thenReturn200WithCreditEditView() throws Exception {
        UserDetails authenticatedUser = regularUserAuthentication();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);
        List<MovieCredit> movieCredits = List.of(createTestMovieCredit(movieId));
        Map<ArtistRole, List<MovieCredit>> creditsByRole = Map.of(ArtistRole.ACTOR, movieCredits);

        when(movieService.findById(movieId)).thenReturn(movie);
        when(movieCreditService.getCreditsByMovie(movie)).thenReturn(movieCredits);
        when(movieCreditService.getCreditsByMovieGroupedByRole(movie)).thenReturn(creditsByRole);

        MockHttpServletRequestBuilder httpRequest = get("/credit/{movieId}/edit", movieId)
                .with(user(authenticatedUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().isOk())
                .andExpect(view().name("credit-edit"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("movieCredits"))
                .andExpect(model().attributeExists("creditsByRole"));

        verify(movieService).findById(movieId);
        verify(movieCreditService).getCreditsByMovie(movie);
        verify(movieCreditService).getCreditsByMovieGroupedByRole(movie);
    }

    @Test
    void getEditCreditsPage_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID movieId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = get("/credit/{movieId}/edit", movieId);

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieService, never()).findById(any());
    }

    @Test
    void deleteCredit_andUserIsAdmin_thenRedirectToCreditEdit() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID creditId = UUID.randomUUID();
        UUID movieId = UUID.randomUUID();
        Movie movie = createTestMovie(movieId);
        MovieCredit movieCredit = MovieCredit.builder()
                .id(creditId)
                .movie(movie)
                .artist(Artist.builder().id(UUID.randomUUID()).name("Artist").build())
                .roleType(ArtistRole.ACTOR)
                .build();

        when(movieCreditService.findCreditById(creditId)).thenReturn(movieCredit);

        MockHttpServletRequestBuilder httpRequest = delete("/credit/{creditId}/delete", creditId)
                .with(user(adminUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/credit/" + movieId + "/edit"));

        verify(movieCreditService).findCreditById(creditId);
        verify(movieCreditService).deleteCredit(movieCredit);
    }

    @Test
    void deleteCredit_andUserIsNotAdmin_thenReturn404NotFound() throws Exception {
        UserDetails regularUser = regularUserAuthentication();
        UUID creditId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/credit/{creditId}/delete", creditId)
                .with(user(regularUser))
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is4xxClientError());

        verify(movieCreditService, never()).deleteCredit(any());
    }

    @Test
    void deleteCredit_andUserIsNotAuthenticated_thenRedirectToLogin() throws Exception {
        UUID creditId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/credit/{creditId}/delete", creditId)
                .with(csrf());

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieCreditService, never()).deleteCredit(any());
    }

    @Test
    void deleteCredit_andNoCsrfToken_thenRedirectToLogin() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID creditId = UUID.randomUUID();

        MockHttpServletRequestBuilder httpRequest = delete("/credit/{creditId}/delete", creditId)
                .with(user(adminUser));

        mockMvc.perform(httpRequest)
                .andExpect(status().is3xxRedirection());

        verify(movieCreditService, never()).deleteCredit(any());
    }

    private static UserDetails adminAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.ADMIN, true);
    }

    private static UserDetails regularUserAuthentication() {
        return new UserData(UUID.randomUUID(), "stanimir", "123123123", UserRole.USER, true);
    }

    private static Movie createTestMovie(UUID movieId) {
        return Movie.builder()
                .id(movieId)
                .title("Title")
                .description("Description")
                .build();
    }

    private static MovieCredit createTestMovieCredit(UUID movieId) {
        return MovieCredit.builder()
                .id(UUID.randomUUID())
                .movie(createTestMovie(movieId))
                .artist(Artist.builder().id(UUID.randomUUID()).name("Artist").build())
                .roleType(ArtistRole.ACTOR)
                .build();
    }
}

