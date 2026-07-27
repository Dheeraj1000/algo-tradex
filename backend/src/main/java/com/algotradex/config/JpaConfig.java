package com.algotradex.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.springframework.context.annotation.Configuration;

/**
 * Configures JPA to handle PostgreSQL enum types correctly.
 * Enums are stored as VARCHAR and mapped via @Enumerated(EnumType.STRING).
 */
@Configuration
public class JpaConfig {
    // PostgreSQL enum casting is handled via columnDefinition in entities
    // and Flyway migration creates the actual DB enum types.
    // Hibernate maps them as strings via @Enumerated(EnumType.STRING).
}
