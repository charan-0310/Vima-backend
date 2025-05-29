package com.vimainsurance.vimaadmin.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vimainsurance.vimaadmin.entity.AdminUser;

@Repository
public interface IAdminUserRepository extends JpaRepository<AdminUser, UUID>{
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);

}
