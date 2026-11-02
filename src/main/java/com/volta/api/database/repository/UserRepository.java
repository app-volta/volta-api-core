package com.volta.api.database.repository;

import com.volta.api.database.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByEmail(String email);

    List<Users> findByCompanyIdAndRoleTypeIn(UUID companyId, List<String> roleTypes);
}