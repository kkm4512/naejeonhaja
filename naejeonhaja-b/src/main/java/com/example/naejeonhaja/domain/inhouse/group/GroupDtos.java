package com.example.naejeonhaja.domain.inhouse.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class GroupDtos {

    private GroupDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "방 이름을 입력해주세요.") @Size(max = 40, message = "방 이름은 40자 이하로 입력해주세요.") String name,
            @NotBlank(message = "비밀번호를 입력해주세요.") @Size(min = 4, max = 64, message = "비밀번호는 4~64자로 입력해주세요.") String password) {
    }

    public record JoinRequest(@NotBlank(message = "비밀번호를 입력해주세요.") String password) {
    }

    /** 입장 성공 응답 — token 을 X-Group-Token 헤더로 보내면 방 데이터에 접근할 수 있다 */
    public record Session(String code, String name, String token) {
    }

    public record Summary(String code, String name, long memberCount, long matchCount) {
    }
}
