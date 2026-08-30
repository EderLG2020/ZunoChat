package com.example.backend.module.streak.application;

import com.example.backend.common.enums.ConversationStatus;
import com.example.backend.common.enums.ConversationType;
import com.example.backend.common.exception.AppException;
import com.example.backend.module.messagemanagement.domain.ConversationModel;
import com.example.backend.module.messagemanagement.persistence.ConversationRepository;
import com.example.backend.module.streak.domain.StreakModel;
import com.example.backend.module.streak.domain.StreakRequestStatus;
import com.example.backend.module.streak.domain.StreakStatus;
import com.example.backend.module.streak.dto.StreakResponse;
import com.example.backend.module.streak.persistence.StreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    private static final Long USER_1 = 10L; // conversación: user1Id=10, user2Id=20
    private static final Long USER_2 = 20L;
    private static final Long CONV_ID = 100L;

    @Mock private StreakRepository streakRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private StreakEventPublisher streakEventPublisher;

    private StreakService streakService;

    @BeforeEach
    void setUp() {
        streakService = new StreakService(streakRepository, conversationRepository, streakEventPublisher);
    }

    private ConversationModel directConversation() {
        return ConversationModel.builder()
                .id(CONV_ID)
                .type(ConversationType.DIRECT)
                .user1Id(USER_1)
                .user2Id(USER_2)
                .status(ConversationStatus.OFFLINE)
                .build();
    }

    @Test
    void requestActivation_creaSolicitudPendiente_cuandoNoHayStreakPrevia() {
        when(conversationRepository.findById(CONV_ID)).thenReturn(Optional.of(directConversation()));
        when(streakRepository.findByConversationId(CONV_ID)).thenReturn(Optional.empty());
        when(streakRepository.save(any(StreakModel.class))).thenAnswer(inv -> inv.getArgument(0));

        StreakResponse response = streakService.requestActivation(USER_1, CONV_ID);

        assertThat(response.enabled()).isFalse();
        assertThat(response.requestStatus()).isEqualTo(StreakRequestStatus.PENDING);
        assertThat(response.requestedByUserId()).isEqualTo(USER_1);

        ArgumentCaptor<StreakModel> captor = ArgumentCaptor.forClass(StreakModel.class);
        verify(streakRepository).save(captor.capture());
        assertThat(captor.getValue().getUserAId()).isEqualTo(USER_1); // min(10,20)
        assertThat(captor.getValue().getUserBId()).isEqualTo(USER_2); // max(10,20)
        verify(streakEventPublisher).publish(eq("REQUEST_SENT"), any(StreakModel.class));
    }

    @Test
    void requestActivation_aceptaAutomaticamente_cuandoElOtroYaHabiaPedido() {
        StreakModel pendingFromOther = StreakModel.builder()
                .conversationId(CONV_ID).userAId(USER_1).userBId(USER_2)
                .enabled(false)
                .requestStatus(StreakRequestStatus.PENDING)
                .requestedByUserId(USER_2) // el otro usuario ya la pidió
                .status(StreakStatus.INACTIVE)
                .build();

        when(conversationRepository.findById(CONV_ID)).thenReturn(Optional.of(directConversation()));
        when(streakRepository.findByConversationId(CONV_ID)).thenReturn(Optional.of(pendingFromOther));
        when(streakRepository.save(any(StreakModel.class))).thenAnswer(inv -> inv.getArgument(0));

        StreakResponse response = streakService.requestActivation(USER_1, CONV_ID);

        assertThat(response.enabled()).isTrue();
        assertThat(response.requestStatus()).isEqualTo(StreakRequestStatus.ACCEPTED);
        verify(streakEventPublisher).publish(eq("REQUEST_ACCEPTED"), any(StreakModel.class));
    }

    @Test
    void disable_apagaInmediatamente_sinNecesitarConfirmacionDelOtro() {
        StreakModel active = StreakModel.builder()
                .conversationId(CONV_ID).userAId(USER_1).userBId(USER_2)
                .enabled(true).currentCount(7).longestCount(7)
                .requestStatus(StreakRequestStatus.ACCEPTED)
                .status(StreakStatus.ACTIVE)
                .build();

        when(conversationRepository.findById(CONV_ID)).thenReturn(Optional.of(directConversation()));
        when(streakRepository.findByConversationId(CONV_ID)).thenReturn(Optional.of(active));
        when(streakRepository.save(any(StreakModel.class))).thenAnswer(inv -> inv.getArgument(0));

        StreakResponse response = streakService.disable(USER_2, CONV_ID);

        assertThat(response.enabled()).isFalse();
        assertThat(response.status()).isEqualTo(StreakStatus.INACTIVE);
        assertThat(response.requestStatus()).isEqualTo(StreakRequestStatus.NONE);
        verify(streakEventPublisher).publish(eq("DISABLED"), any(StreakModel.class));
    }

    @Test
    void respondToActivation_rechazaResponderLaPropiaSolicitud() {
        StreakModel pendingFromMe = StreakModel.builder()
                .conversationId(CONV_ID).userAId(USER_1).userBId(USER_2)
                .enabled(false)
                .requestStatus(StreakRequestStatus.PENDING)
                .requestedByUserId(USER_1)
                .status(StreakStatus.INACTIVE)
                .build();

        when(conversationRepository.findById(CONV_ID)).thenReturn(Optional.of(directConversation()));
        when(streakRepository.findByConversationId(CONV_ID)).thenReturn(Optional.of(pendingFromMe));

        assertThatThrownBy(() -> streakService.respondToActivation(USER_1, CONV_ID, true))
                .isInstanceOf(AppException.class);

        verify(streakEventPublisher, never()).publish(anyString(), any(StreakModel.class));
    }

    @Test
    void recordInteraction_noHaceNada_siLaRachaNoEstaHabilitada() {
        StreakModel disabled = StreakModel.builder()
                .conversationId(CONV_ID).userAId(USER_1).userBId(USER_2)
                .enabled(false)
                .status(StreakStatus.INACTIVE)
                .build();

        when(streakRepository.findByConversationIdForUpdate(CONV_ID)).thenReturn(Optional.of(disabled));

        streakService.recordInteraction(CONV_ID, USER_1, USER_2);

        verify(streakRepository, never()).save(any());
        verify(streakEventPublisher, never()).publish(anyString(), any(StreakModel.class));
    }

    @Test
    void recordInteraction_noHaceNada_siElMensajeEsDeUnGrupo() {
        streakService.recordInteraction(CONV_ID, USER_1, null); // receiverId null → GROUP

        verifyNoInteractions(streakRepository);
        verifyNoInteractions(streakEventPublisher);
    }
}
