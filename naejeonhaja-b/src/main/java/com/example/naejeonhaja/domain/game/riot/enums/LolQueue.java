package com.example.naejeonhaja.domain.game.riot.enums;

import java.util.Arrays;
import java.util.Set;

/**
 * match-v5 의 queueId 와 한국어 이름.
 */
public enum LolQueue {
    CUSTOM(0, "사용자 설정"),
    NORMAL_DRAFT(400, "일반 (드래프트)"),
    SOLO(420, "솔로랭크"),
    NORMAL_BLIND(430, "일반 (블라인드)"),
    FLEX(440, "자유랭크"),
    ARAM(450, "칼바람 나락"),
    QUICKPLAY(490, "빠른 대전"),
    ARAM_CLASH(720, "칼바람 격전"),
    URF(900, "우르프"),
    ONE_FOR_ALL(1020, "단일 챔피언"),
    ARENA(1700, "아레나"),
    ARENA_2(1710, "아레나"),
    URF_ARURF(1900, "우르프"),
    SWIFTPLAY(480, "신속 대전"),
    CLASH(700, "격전");

    /** 포지션 통계/라인 추천에 쓰는 소환사의 협곡 큐 */
    public static final Set<Integer> RIFT_QUEUES = Set.of(400, 420, 430, 440, 480, 490, 700);

    private final int id;
    private final String label;

    LolQueue(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(int queueId) {
        return Arrays.stream(values())
                .filter(q -> q.id == queueId)
                .map(LolQueue::getLabel)
                .findFirst()
                .orElse("기타 모드");
    }
}
