package app.popdapplication.web;

import app.popdapplication.model.entity.Artist;
import app.popdapplication.model.enums.UserRole;
import app.popdapplication.security.UserData;
import app.popdapplication.service.ArtistService;
import app.popdapplication.service.MovieCreditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArtistController.class)
public class ArtistControllerApiTest {

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private MovieCreditService movieCreditService;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAddArtistPage_andUserIsAdmin_thenReturnAddView() throws Exception {
        UserDetails adminUser = adminAuthentication();

        mockMvc.perform(get("/artist/add")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-add"))
                .andExpect(model().attributeExists("addArtistRequest"));
    }

    @Test
    void postAddArtist_andUserIsAdmin_andValidData_thenRedirectToArtistPage() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID artistId = UUID.randomUUID();
        Artist createdArtist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .build();

        when(artistService.addArtist(any())).thenReturn(createdArtist);

        mockMvc.perform(post("/artist/add")
                        .param("name", "John Doe")
                        .param("biography", "A great actor")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artist/" + artistId));

        verify(artistService).addArtist(any());
    }

    @Test
    void postAddArtist_andUserIsAdmin_andInvalidData_thenReturnAddViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();

        mockMvc.perform(post("/artist/add")
                        .param("name", "") // Invalid: blank name
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-add"));

        verify(artistService, never()).addArtist(any());
    }

    @Test
    void getArtistPage_thenReturnArtistView() throws Exception {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();

        when(artistService.findById(artistId)).thenReturn(artist);
        when(movieCreditService.findAllCreditsByArtist(artist)).thenReturn(10);
        when(movieCreditService.getCreditsByArtistGrouped(artist)).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/artist/{artistId}", artistId))
                .andExpect(status().isOk())
                .andExpect(view().name("artist"))
                .andExpect(model().attributeExists("artist", "movieCreditsCount", "creditsByRole"));

        verify(artistService).findById(artistId);
        verify(movieCreditService).findAllCreditsByArtist(artist);
        verify(movieCreditService).getCreditsByArtistGrouped(artist);
    }

    @Test
    void getEditArtistPage_andUserIsAdmin_thenReturnEditView() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .build();

        when(artistService.findById(artistId)).thenReturn(artist);

        mockMvc.perform(get("/artist/{artistId}/edit", artistId)
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-edit"))
                .andExpect(model().attributeExists("artist", "editArtistRequest"));

        verify(artistService).findById(artistId);
    }

    @Test
    void putEditArtist_andUserIsAdmin_andValidData_thenRedirectToArtistPage() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .build();

        when(artistService.findById(artistId)).thenReturn(artist);

        mockMvc.perform(put("/artist/{artistId}/edit", artistId)
                        .param("name", "Jane Doe")
                        .param("biography", "Updated biography")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/artist/" + artistId));

        verify(artistService).findById(artistId);
        verify(artistService).updateArtistInfo(eq(artistId), any());
    }

    @Test
    void putEditArtist_andUserIsAdmin_andInvalidData_thenReturnEditViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .build();

        when(artistService.findById(artistId)).thenReturn(artist);

        mockMvc.perform(put("/artist/{artistId}/edit", artistId)
                        .param("name", "") // Invalid: blank name
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-edit"))
                .andExpect(model().attributeExists("artist"));

        verify(artistService).findById(artistId);
        verify(artistService, never()).updateArtistInfo(any(), any());
    }

    @Test
    void putEditArtist_andUserIsAdmin_andInvalidImageUrl_thenReturnEditViewWithErrors() throws Exception {
        UserDetails adminUser = adminAuthentication();
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder()
                .id(artistId)
                .name("John Doe")
                .build();

        when(artistService.findById(artistId)).thenReturn(artist);

        mockMvc.perform(put("/artist/{artistId}/edit", artistId)
                        .param("name", "John Doe")
                        .param("imageUrl", "not-a-valid-url") // Invalid URL
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("artist-edit"))
                .andExpect(model().attributeExists("artist"));

        verify(artistService).findById(artistId);
        verify(artistService, never()).updateArtistInfo(any(), any());
    }

    private static UserDetails adminAuthentication() {
        return new UserData(UUID.randomUUID(), "tsvetelina", "123123123", UserRole.ADMIN, true);
    }
}

