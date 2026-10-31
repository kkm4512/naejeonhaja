package com.example.naejeonhaja.domain.inhouse.auth;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroupRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * /api/v1/groups/{code}/** 요청은 X-Group-Token 헤더의 토큰이 그 방 것이어야 통과.
 * 통과하면 요청 속성 "group" 에 방 엔티티를 넣어 둔다 (컨트롤러에서 @RequestAttribute 로 사용).
 */
@Component
@RequiredArgsConstructor
public class GroupAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Group-Token";
    public static final String ATTR = "group";

    private final GroupTokenService tokenService;
    private final InhouseGroupRepository groupRepository;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String code = vars == null ? null : vars.get("code");
        if (code == null) return true;

        InhouseGroup group = groupRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BaseException(BaseApiResponse.GROUP_NOT_FOUND));
        boolean ok = tokenService.verify(request.getHeader(HEADER))
                .map(claims -> tokenService.matches(claims, group))
                .orElse(false);
        if (!ok) {
            throw new BaseException(BaseApiResponse.GROUP_UNAUTHORIZED);
        }
        request.setAttribute(ATTR, group);
        return true;
    }
}
