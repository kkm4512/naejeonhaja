package com.example.naejeonhaja.domain.inhouse.match;

/**
 * 내전 MMR 보정 (Elo 방식).
 *
 * <p>두 팀 평균 MMR 차이로 블루팀 기대 승률을 구한다. 평균 200점(=티어 하나) 높으면 약 91%.
 * 이긴 팀은 K × (1 − 기대 승률), 진 팀은 그만큼 뺀다. 팽팽한 경기(50%)면 ±20.
 * 예상을 뒤집고 이기면 더 많이 오르고, 이길 만한 경기를 이기면 조금 오른다.</p>
 */
public final class InhouseRating {

    public static final int K = 40;
    private static final double SCALE = 200.0;

    private InhouseRating() {
    }

    public static double expectedBlue(int blueTotal, int redTotal) {
        double diff = (redTotal - blueTotal) / 5.0;
        return 1.0 / (1.0 + Math.pow(10, diff / SCALE));
    }

    /** @return [블루 선수 각자 변화량, 레드 선수 각자 변화량] */
    public static int[] deltas(int blueTotal, int redTotal, InhouseMatch.Team winner) {
        double eBlue = expectedBlue(blueTotal, redTotal);
        double scoreBlue = winner == InhouseMatch.Team.BLUE ? 1 : 0;
        int blue = (int) Math.round(K * (scoreBlue - eBlue));
        // 지는 쪽이 0이 되지 않도록 최소 1
        if (blue == 0) blue = winner == InhouseMatch.Team.BLUE ? 1 : -1;
        return new int[]{blue, -blue};
    }
}
