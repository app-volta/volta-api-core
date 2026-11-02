package com.volta.api.service;

import com.volta.api.database.entity.Cooperative;
import com.volta.api.database.repository.CooperativeRepository;
import com.volta.api.dto.cooperative.CooperativeRequestDTO;
import com.volta.api.dto.cooperative.CooperativeResponseDTO;
import com.volta.api.exception.ConflictException;
import com.volta.api.exception.InvalidRequestException;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.CooperativeMapper;
import com.volta.api.mapper.ScoringMapper;
import com.volta.api.dto.scoring.CooperativeScoringDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@DisplayName("CooperativeService")
class CooperativeServiceTest {

    private static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Mock
    private CooperativeRepository cooperativeRepository;

    private CooperativeService service;

    @BeforeEach
    void setUp() {
        service = new CooperativeService(cooperativeRepository, new CooperativeMapper(), new ScoringMapper());
    }

    private CooperativeRequestDTO request(String specialties) {
        return new CooperativeRequestDTO(
                "Recicla Mais",
                "11.444.777/0001-61",
                new BigDecimal("-23.561684"),
                new BigDecimal("-46.655981"),
                "contato@reciclamais.com.br",
                specialties);
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("especialidades válidas salvam a cooperativa com o texto normalizado")
        void register_validSpecialties_savesNormalizedSpecialties() {
            when(cooperativeRepository.existsByCnpj("11444777000161")).thenReturn(false);
            when(cooperativeRepository.save(any(Cooperative.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CooperativeResponseDTO response = service.register(request("papel, Plástico, não reciclável"));

            assertThat(response.specialties()).isEqualTo("PAPEL,PLASTICO,NAO_RECICLAVEL");
            ArgumentCaptor<Cooperative> captor = ArgumentCaptor.forClass(Cooperative.class);
            verify(cooperativeRepository).save(captor.capture());
            assertThat(captor.getValue().getCnpj()).isEqualTo("11444777000161");
        }

        @ParameterizedTest(name = "\"{0}\" é rejeitada")
        @ValueSource(strings = {"BORRACHA", "PAPEL,BORRACHA", "papel, tecido"})
        @DisplayName("especialidade desconhecida lança requisição inválida e não consulta nem salva")
        void register_unknownSpecialty_throwsInvalidRequestAndDoesNotSave(String specialties) {
            assertThatThrownBy(() -> service.register(request(specialties)))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("Invalid specialty")
                    .hasMessageContaining("PAPEL")
                    .hasMessageContaining("NAO_RECICLAVEL");

            verifyNoInteractions(cooperativeRepository);
        }

        @Test
        @DisplayName("mensagem aponta a especialidade inválida")
        void register_unknownSpecialty_messageNamesTheInvalidValue() {
            assertThatThrownBy(() -> service.register(request("PAPEL,tecido")))
                    .hasMessageContaining("'TECIDO'");
        }

        @Test
        @DisplayName("CNPJ já cadastrado lança conflito e não salva")
        void register_cnpjAlreadyRegistered_throwsConflictAndDoesNotSave() {
            when(cooperativeRepository.existsByCnpj("11444777000161")).thenReturn(true);

            assertThatThrownBy(() -> service.register(request("PAPEL")))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("CNPJ already registered");

            verify(cooperativeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("especialidade desconhecida lança requisição inválida antes de consultar o banco")
        void update_unknownSpecialty_throwsInvalidRequestWithoutQueryingRepository() {
            assertThatThrownBy(() -> service.update(ID, request("BORRACHA")))
                    .isInstanceOf(InvalidRequestException.class);

            verifyNoInteractions(cooperativeRepository);
        }

        @Test
        @DisplayName("cooperativa inexistente lança not found")
        void update_missingCooperative_throwsNotFound() {
            when(cooperativeRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(ID, request("PAPEL")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cooperative not found");

            verify(cooperativeRepository, never()).save(any());
        }

        @Test
        @DisplayName("CNPJ de outra cooperativa lança conflito")
        void update_cnpjOfAnotherCooperative_throwsConflict() {
            when(cooperativeRepository.findById(ID)).thenReturn(Optional.of(new Cooperative()));
            when(cooperativeRepository.existsByCnpjAndIdNot("11444777000161", ID)).thenReturn(true);

            assertThatThrownBy(() -> service.update(ID, request("PAPEL")))
                    .isInstanceOf(ConflictException.class);

            verify(cooperativeRepository, never()).save(any());
        }

        @Test
        @DisplayName("dados válidos alteram os campos e salvam")
        void update_validData_changesFieldsAndSaves() {
            Cooperative existing = new Cooperative();
            existing.setId(ID);
            when(cooperativeRepository.findById(ID)).thenReturn(Optional.of(existing));

            CooperativeResponseDTO response = service.update(ID, request("vidro,METAL"));

            assertThat(response.specialties()).isEqualTo("VIDRO,METAL");
            assertThat(existing.getName()).isEqualTo("Recicla Mais");
            verify(cooperativeRepository).save(existing);
        }
    }

    private Cooperative cooperative(UUID id, String name) {
        Cooperative cooperative = new Cooperative();
        cooperative.setId(id);
        cooperative.setName(name);
        cooperative.setCnpj("11444777000161");
        cooperative.setLatitude(new BigDecimal("-23.561684"));
        cooperative.setLongitude(new BigDecimal("-46.655981"));
        cooperative.setSpecialties("PAPEL");
        return cooperative;
    }

    @Nested
    @DisplayName("consultas")
    class Queries {

        @Test
        @DisplayName("getCooperatives converte todas as cooperativas")
        void getCooperatives_existingRecords_convertsAll() {
            when(cooperativeRepository.findAll()).thenReturn(List.of(
                    cooperative(UUID.randomUUID(), "First"), cooperative(UUID.randomUUID(), "Second")));

            assertThat(service.getCooperatives()).extracting(CooperativeResponseDTO::name)
                    .containsExactly("First", "Second");
        }

        @Test
        @DisplayName("getCooperativeById existente devolve a cooperativa convertida")
        void getCooperativeById_existing_returnsConvertedCooperative() {
            Cooperative cooperative = cooperative(ID, "First");
            when(cooperativeRepository.findById(ID)).thenReturn(Optional.of(cooperative));

            CooperativeResponseDTO response = service.getCooperativeById(ID);

            assertThat(response.id()).isEqualTo(ID);
            assertThat(response.specialties()).isEqualTo("PAPEL");
        }

        @Test
        @DisplayName("getCooperativeById inexistente lança not found")
        void getCooperativeById_missing_throwsNotFound() {
            when(cooperativeRepository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCooperativeById(ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cooperative not found");
        }
    }

    @Nested
    @DisplayName("getScoringData")
    class GetScoringData {

        private Cooperative cooperative(String specialties, BigDecimal averageRating) {
            Cooperative cooperative = new Cooperative();
            cooperative.setId(ID);
            cooperative.setName("Recicla Sul");
            cooperative.setCnpj("11444777000161");
            cooperative.setLatitude(new BigDecimal("-23.561684"));
            cooperative.setLongitude(new BigDecimal("-46.655981"));
            cooperative.setAverageRating(averageRating);
            cooperative.setSpecialties(specialties);
            return cooperative;
        }

        @Test
        @DisplayName("sem cooperativas devolve lista vazia")
        void getScoringData_noCooperatives_returnsEmptyList() {
            when(cooperativeRepository.findAll()).thenReturn(List.of());

            assertThat(service.getScoringData()).isEmpty();
        }

        @Test
        @DisplayName("devolve especialidades como lista normalizada e sem repetição")
        void getScoringData_returnsNormalizedSpecialtyList() {
            when(cooperativeRepository.findAll())
                    .thenReturn(List.of(cooperative("Papel,, plástico ,PERIGOSO,papel", new BigDecimal("4.50"))));

            List<CooperativeScoringDataDTO> result = service.getScoringData();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(ID);
            assertThat(result.get(0).name()).isEqualTo("Recicla Sul");
            assertThat(result.get(0).latitude()).isEqualByComparingTo("-23.561684");
            assertThat(result.get(0).longitude()).isEqualByComparingTo("-46.655981");
            assertThat(result.get(0).averageRating()).isEqualByComparingTo("4.50");
            assertThat(result.get(0).specialties()).containsExactly("PAPEL", "PLASTICO", "PERIGOSO");
        }

        @Test
        @DisplayName("especialidades nulas viram lista vazia e a avaliação segue como veio")
        void getScoringData_nullSpecialties_returnsEmptyListAndKeepsRating() {
            when(cooperativeRepository.findAll()).thenReturn(List.of(cooperative(null, null)));

            List<CooperativeScoringDataDTO> result = service.getScoringData();

            assertThat(result.get(0).specialties()).isEmpty();
            assertThat(result.get(0).averageRating()).isNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("inexistente lança not found e não apaga")
        void delete_missing_throwsNotFoundAndDoesNotDelete() {
            when(cooperativeRepository.existsById(ID)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(ID)).isInstanceOf(ResourceNotFoundException.class);

            verify(cooperativeRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("existente remove pelo id")
        void delete_existing_removesById() {
            when(cooperativeRepository.existsById(ID)).thenReturn(true);

            service.delete(ID);

            verify(cooperativeRepository).deleteById(ID);
        }
    }
}
