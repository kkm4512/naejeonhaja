package com.example.naejeonhaja.domain.inhouse.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByGroupIdOrderByNameAsc(Long groupId);

    Optional<Member> findByGroupIdAndNameKey(Long groupId, String nameKey);

    Optional<Member> findByGroupIdAndPuuid(Long groupId, String puuid);

    Optional<Member> findByIdAndGroupId(Long id, Long groupId);

    long countByGroupId(Long groupId);
}
