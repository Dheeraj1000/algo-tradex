package com.algotradex.repository;

import com.algotradex.model.User;
import com.algotradex.model.enums.UserRole;
import com.algotradex.model.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActive(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.deletedAt IS NULL")
    Page<User> findByRole(UserRole role, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.deletedAt IS NULL")
    long countByStatus(UserStatus status);
}
