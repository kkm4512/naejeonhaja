package com.example.naejeonhaja.domain.game.lol.service;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.lol.dto.common.LolPlayerDto;
import com.example.naejeonhaja.domain.game.lol.dto.req.LolPlayerRequestDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolTeamResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LolTeamService {

    private final LolTeamBalancer balancer;
    private final int alternatives;

    public LolTeamService(@Value("${lol.balance.sub-line-penalty:100}") int subLinePenalty,
                          @Value("${lol.balance.off-role-penalty:250}") int offRolePenalty,
                          @Value("${lol.balance.alternatives:4}") int alternatives) {
        this.balancer = new LolTeamBalancer(subLinePenalty, offRolePenalty);
        this.alternatives = Math.max(0, alternatives);
    }

    /** 소환사의 협곡: 라인 배정 + MMR 밸런싱 */
    public LolTeamResponseDto createRiftTeams(LolPlayerRequestDto dto) {
        return create(dto, LolTeamBalancer.Mode.RIFT);
    }

    /** 칼바람 나락: 라인 없이 MMR 밸런싱만 */
    public LolTeamResponseDto createAbyssTeams(LolPlayerRequestDto dto) {
        return create(dto, LolTeamBalancer.Mode.ABYSS);
    }

    private LolTeamResponseDto create(LolPlayerRequestDto dto, LolTeamBalancer.Mode mode) {
        List<LolPlayerDto> players = dto.getLolPlayers();
        validateUniqueNames(players);
        List<LolTeamResponseDto> results = balancer.balance(players, mode, alternatives + 1, dto.allowOffRoleOrDefault());
        return results.get(0).withAlternatives(results.subList(1, results.size()));
    }

    private void validateUniqueNames(List<LolPlayerDto> players) {
        Set<String> seen = new HashSet<>();
        for (LolPlayerDto p : players) {
            String key = p.getName().trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            if (!seen.add(key)) {
                throw new BaseException(BaseApiResponse.DUPLICATE_PLAYER,
                        "같은 이름의 플레이어가 두 번 들어있어요: " + p.getName());
            }
        }
    }
}
