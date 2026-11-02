package com.volta.api.mapper;

import com.volta.api.database.entity.Cooperative;
import com.volta.api.dto.cooperative.CooperativeRequestDTO;
import com.volta.api.dto.cooperative.CooperativeResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class CooperativeMapper {

    public Cooperative toEntity(CooperativeRequestDTO dto) {
        Cooperative cooperative = new Cooperative();

        cooperative.setName(dto.name());
        cooperative.setCnpj(dto.cnpj());
        cooperative.setLatitude(dto.latitude());
        cooperative.setLongitude(dto.longitude());
        cooperative.setEmail(dto.email());
        cooperative.setSpecialties(dto.specialties());

        return cooperative;
    }

    public CooperativeResponseDTO toResponse(Cooperative cooperative) {
        return new CooperativeResponseDTO(
                cooperative.getId(),
                cooperative.getName(),
                cooperative.getCnpj(),
                cooperative.getLatitude(),
                cooperative.getLongitude(),
                cooperative.getEmail(),
                cooperative.getAverageRating(),
                cooperative.getSpecialties()
        );
    }

    public void updateEntity(Cooperative cooperative, CooperativeRequestDTO dto) {
        cooperative.setName(dto.name());
        cooperative.setCnpj(dto.cnpj());
        cooperative.setLatitude(dto.latitude());
        cooperative.setLongitude(dto.longitude());
        cooperative.setEmail(dto.email());
        cooperative.setSpecialties(dto.specialties());
    }
}
