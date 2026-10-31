package com.example.naejeonhaja.domain.inhouse.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InhouseMatchRepository extends JpaRepository<InhouseMatch, Long> {

    @EntityGraph(attributePaths = "players")
    Page<InhouseMatch> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    @EntityGraph(attributePaths = "players")
    Optional<InhouseMatch> findByIdAndGroupId(Long id, Long groupId);

    @EntityGraph(attributePaths = {"players", "group"})
    Optional<InhouseMatch> findByPublicId(String publicId);

    long countByGroupId(Long groupId);
}
