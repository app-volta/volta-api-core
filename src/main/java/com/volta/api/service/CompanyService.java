package com.volta.api.service;

import com.volta.api.database.entity.Company;
import com.volta.api.database.repository.CompanyRepository;
import com.volta.api.dto.company.CompanyRequestDTO;
import com.volta.api.dto.company.CompanyResponseDTO;
import com.volta.api.exception.ConflictException;
import com.volta.api.exception.InvalidRequestException;
import com.volta.api.exception.ResourceNotFoundException;
import com.volta.api.mapper.CompanyMapper;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.CompanyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService implements CompanyUseCase {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    public CompanyResponseDTO register(CompanyRequestDTO dto) {
        validateCoordinates(dto);

        if (companyRepository.existsByCnpj(dto.cnpj())) {
            throw new ConflictException("CNPJ already registered");
        }
        Company company = companyMapper.toEntity(dto);
        Company companySaved = companyRepository.save(company);
        return companyMapper.toResponse(companySaved);
    }

    public List<CompanyResponseDTO> getCompanies() {
        List<CompanyResponseDTO> companies = new ArrayList<>();

        for (Company company : companyRepository.findAll()) {
            companies.add(companyMapper.toResponse(company));
        }

        return companies;
    }

    public CompanyResponseDTO getCompanyById(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company"));
        return companyMapper.toResponse(company);
    }

    public CompanyResponseDTO getMyCompany(AuthenticatedUser author) {
        if (author.companyId() == null) {
            throw new ResourceNotFoundException("Company");
        }
        return getCompanyById(author.companyId());
    }

    @Transactional
    public CompanyResponseDTO update(UUID id, CompanyRequestDTO dto) {
        validateCoordinates(dto);

        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company"));

        if (companyRepository.existsByCnpjAndIdNot(dto.cnpj(), id)) {
            throw new ConflictException("CNPJ already registered");
        }

        companyMapper.updateEntity(company, dto);
        companyRepository.save(company);

        return companyMapper.toResponse(company);
    }

    public void delete(UUID id) {
        if (!companyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Company");
        }
        companyRepository.deleteById(id);
    }

    private void validateCoordinates(CompanyRequestDTO dto) {
        if ((dto.latitude() == null) != (dto.longitude() == null)) {
            throw new InvalidRequestException("Latitude and longitude must be informed together");
        }
    }
}
