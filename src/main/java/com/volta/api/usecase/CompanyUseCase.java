package com.volta.api.usecase;

import com.volta.api.dto.company.CompanyRequestDTO;
import com.volta.api.dto.company.CompanyResponseDTO;
import com.volta.api.security.AuthenticatedUser;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {

    CompanyResponseDTO register(CompanyRequestDTO dto);

    List<CompanyResponseDTO> getCompanies();

    CompanyResponseDTO getCompanyById(UUID id);

    CompanyResponseDTO getMyCompany(AuthenticatedUser author);

    CompanyResponseDTO update(UUID id, CompanyRequestDTO dto);

    void delete(UUID id);

}