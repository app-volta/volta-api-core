package com.volta.api.controller;

import com.volta.api.controller.docs.CompanyControllerDocs;
import com.volta.api.dto.company.CompanyRequestDTO;
import com.volta.api.dto.company.CompanyResponseDTO;
import com.volta.api.security.AuthenticatedUser;
import com.volta.api.usecase.CompanyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompanyController implements CompanyControllerDocs {

    private final CompanyUseCase companyUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponseDTO create(@Valid @RequestBody CompanyRequestDTO dto) {
        return companyUseCase.register(dto);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<CompanyResponseDTO> show() {
        List<CompanyResponseDTO> companies = companyUseCase.getCompanies();
        return companies;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public CompanyResponseDTO showMe(@AuthenticationPrincipal AuthenticatedUser author) {
        CompanyResponseDTO company = companyUseCase.getMyCompany(author);
        return company;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CompanyResponseDTO show(@PathVariable UUID id) {
        CompanyResponseDTO company = companyUseCase.getCompanyById(id);
        return company;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public CompanyResponseDTO update(
            @PathVariable UUID id,
            @Valid @RequestBody CompanyRequestDTO dto
    ) {
        CompanyResponseDTO response = companyUseCase.update(id, dto);
        return response;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        companyUseCase.delete(id);
    }
}
