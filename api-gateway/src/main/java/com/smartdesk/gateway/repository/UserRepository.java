package com.smartdesk.gateway.repository;

import com.smartdesk.gateway.entity.AppUser;
import com.smartdesk.gateway.entity.Department;
import com.smartdesk.gateway.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select u from AppUser u
            where (:role is null or u.roleName = :role)
              and (:dept is null or u.departmentName = :dept)
            order by u.name""")
    List<AppUser> search(Role role, Department dept);
}
