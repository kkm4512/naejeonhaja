package com.example.naejeonhaja.domain.inhouse.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InhouseGroupRepository extends JpaRepository<InhouseGroup, Long> {
    Optional<InhouseGroup> findByCode(String code);

    boolean existsByCode(String code);
}
