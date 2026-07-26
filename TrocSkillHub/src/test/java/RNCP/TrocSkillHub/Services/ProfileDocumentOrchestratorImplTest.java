package RNCP.TrocSkillHub.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;
import RNCP.TrocSkillHub.DTOs.ProfileDocumentResponseDTO;
import RNCP.TrocSkillHub.Mappers.ProfileDocumentMapper;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Services.ImplServices.ProfileDocumentOrchestratorImpl;

@ExtendWith(MockitoExtension.class)
class ProfileDocumentOrchestratorImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ProfileDocumentMapper profileDocumentMapper;

    @Mock
    private DocumentCompositionService documentCompositionService;

    @Mock
    private EmailService emailService;

    @Mock
    private Authentication authentication;

    private ProfileDocumentOrchestratorImpl orchestrator;

    private User user;
    private ProfileDocumentDTO documentDTO;
    private byte[] pdfBytes;

    @BeforeEach
    void setUp() {
        orchestrator = new ProfileDocumentOrchestratorImpl(
                userService,
                profileDocumentMapper,
                documentCompositionService,
                emailService);

        user = new User();
        user.setId(1L);
        user.setFirstName("Jean");
        user.setLastName("Dupont");
        user.setEmail("jean.dupont@test.fr");

        documentDTO = new ProfileDocumentDTO(
                "Jean",
                "Dupont",
                "jean.dupont@test.fr",
                "Paris",
                "France",
                "Développeur",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                false,
                false,
                false);

        pdfBytes = "%PDF-1.4 mock".getBytes();
    }

    @Test
    @DisplayName("exportCurrentUserProfile: maps user, composes PDF, sends email, returns SENT")
    void exportCurrentUserProfile_success_returnsSent() {
        when(authentication.getName()).thenReturn("jean.dupont@test.fr");
        when(userService.getUserByEmail("jean.dupont@test.fr")).thenReturn(Optional.of(user));
        when(profileDocumentMapper.toDocumentDTO(user)).thenReturn(documentDTO);
        when(profileDocumentMapper.buildFileName(documentDTO)).thenReturn("profil-jean-dupont.pdf");
        when(documentCompositionService.composePdf(documentDTO)).thenReturn(pdfBytes);

        ProfileDocumentResponseDTO response = orchestrator.exportCurrentUserProfile(authentication);

        assertThat(response.status()).isEqualTo("SENT");
        assertThat(response.recipient()).isEqualTo("jean.dupont@test.fr");
        assertThat(response.documentName()).isEqualTo("profil-jean-dupont.pdf");
        assertThat(response.message()).isEqualTo("Le CV PDF a été envoyé par email");

        verify(documentCompositionService).composePdf(documentDTO);
        verify(emailService).sendProfileDocumentPdf(
                eq("jean.dupont@test.fr"),
                eq(pdfBytes),
                eq("profil-jean-dupont.pdf"));
    }

    @Test
    @DisplayName("exportCurrentUserProfile: null authentication throws SecurityException")
    void exportCurrentUserProfile_nullAuthentication_throwsSecurityException() {
        assertThatThrownBy(() -> orchestrator.exportCurrentUserProfile(null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Non authentifié");

        verify(userService, never()).getUserByEmail(any());
        verify(documentCompositionService, never()).composePdf(any());
        verify(emailService, never()).sendProfileDocumentPdf(any(), any(), any());
    }

    @Test
    @DisplayName("exportCurrentUserProfile: blank principal throws SecurityException")
    void exportCurrentUserProfile_blankPrincipal_throwsSecurityException() {
        when(authentication.getName()).thenReturn("   ");

        assertThatThrownBy(() -> orchestrator.exportCurrentUserProfile(authentication))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Non authentifié");

        verify(documentCompositionService, never()).composePdf(any());
        verify(emailService, never()).sendProfileDocumentPdf(any(), any(), any());
    }

    @Test
    @DisplayName("exportCurrentUserProfile: unknown user throws RuntimeException")
    void exportCurrentUserProfile_userNotFound_throwsRuntimeException() {
        when(authentication.getName()).thenReturn("unknown@test.fr");
        when(userService.getUserByEmail("unknown@test.fr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.exportCurrentUserProfile(authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Utilisateur non trouvé");

        verify(documentCompositionService, never()).composePdf(any());
        verify(emailService, never()).sendProfileDocumentPdf(any(), any(), any());
    }

    @Test
    @DisplayName("exportCurrentUserProfile: composition failure does not send email")
    void exportCurrentUserProfile_compositionFails_doesNotSendEmail() {
        when(authentication.getName()).thenReturn("jean.dupont@test.fr");
        when(userService.getUserByEmail("jean.dupont@test.fr")).thenReturn(Optional.of(user));
        when(profileDocumentMapper.toDocumentDTO(user)).thenReturn(documentDTO);
        when(profileDocumentMapper.buildFileName(documentDTO)).thenReturn("profil-jean-dupont.pdf");
        when(documentCompositionService.composePdf(documentDTO))
                .thenThrow(new RuntimeException("PDF engine down"));

        assertThatThrownBy(() -> orchestrator.exportCurrentUserProfile(authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Échec de la composition du document PDF");

        verify(emailService, never()).sendProfileDocumentPdf(any(), any(), any());
    }

    @Test
    @DisplayName("exportCurrentUserProfile: email failure wraps RuntimeException")
    void exportCurrentUserProfile_emailFails_throwsRuntimeException() {
        when(authentication.getName()).thenReturn("jean.dupont@test.fr");
        when(userService.getUserByEmail("jean.dupont@test.fr")).thenReturn(Optional.of(user));
        when(profileDocumentMapper.toDocumentDTO(user)).thenReturn(documentDTO);
        when(profileDocumentMapper.buildFileName(documentDTO)).thenReturn("profil-jean-dupont.pdf");
        when(documentCompositionService.composePdf(documentDTO)).thenReturn(pdfBytes);
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(emailService)
                .sendProfileDocumentPdf(
                        eq("jean.dupont@test.fr"),
                        eq(pdfBytes),
                        eq("profil-jean-dupont.pdf"));

        assertThatThrownBy(() -> orchestrator.exportCurrentUserProfile(authentication))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Échec de l'envoi de l'email avec le document PDF");
    }
}
