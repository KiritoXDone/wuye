package com.wuye.auth.service;

import com.wuye.auth.entity.Account;
import com.wuye.auth.mapper.AccountMapper;
import com.wuye.auth.vo.LoginVO;
import com.wuye.agent.service.AgentAuthorizationService;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer;
    private final long expireHours;
    private final AccountMapper accountMapper;
    private final AgentAuthorizationService agentAuthorizationService;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expire-hours}") long expireHours,
                      AccountMapper accountMapper,
                      AgentAuthorizationService agentAuthorizationService) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expireHours = expireHours;
        this.accountMapper = accountMapper;
        this.agentAuthorizationService = agentAuthorizationService;
    }

    public LoginVO issueLogin(Account account) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusHours(expireHours);
        String accessToken = buildToken(account, now, expireAt, "access");
        String refreshToken = buildToken(account, now, now.plusHours(expireHours * 2), "refresh");

        LoginVO vo = new LoginVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(expireHours * 3600);
        vo.setAccountId(account.getId());
        vo.setAccountType(account.getAccountType());
        vo.setProductRole(resolveProductRole(account));
        vo.setRoles(resolveRoles(account));
        vo.setNeedResetPassword(Boolean.FALSE);
        return vo;
    }

    public LoginUser parseAccessToken(String token) {
        Claims claims = parseToken(token, "access");
        return toLoginUser(claims);
    }

    public LoginUser parseRefreshToken(String token) {
        Claims claims = parseToken(token, "refresh");
        return toLoginUser(claims);
    }

    private Claims parseToken(String token, String tokenType) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (!tokenType.equals(claims.get("tokenType", String.class))) {
            throw new IllegalArgumentException("token type mismatch");
        }
        return claims;
    }

    private LoginUser toLoginUser(Claims claims) {
        Long accountId = ((Number) claims.get("accountId")).longValue();
        Account account = accountMapper.findById(accountId);
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException("UNAUTHORIZED", "账号不存在或已停用", HttpStatus.UNAUTHORIZED);
        }
        Date issuedAt = claims.getIssuedAt();
        if (account.getTokenInvalidBefore() != null && issuedAt != null) {
            Date tokenInvalidBefore = toDate(account.getTokenInvalidBefore());
            if (!issuedAt.after(tokenInvalidBefore)) {
                throw new BusinessException("UNAUTHORIZED", "Token 已失效", HttpStatus.UNAUTHORIZED);
            }
        }
        return new LoginUser(
                account.getId(),
                account.getAccountType(),
                resolveProductRole(account),
                account.getRealName(),
                resolveRoles(account),
                resolveDataScope(account),
                resolveGroupIds(account)
        );
    }

    private String buildToken(Account account, LocalDateTime issuedAt, LocalDateTime expiredAt, String tokenType) {
        return Jwts.builder()
                .issuer(issuer)
                .issuedAt(toDate(issuedAt))
                .expiration(toDate(expiredAt))
                .subject(String.valueOf(account.getId()))
                .claim("tokenType", tokenType)
                .claim("accountId", account.getId())
                .claim("accountType", account.getAccountType())
                .claim("productRole", resolveProductRole(account))
                .claim("realName", account.getRealName())
                .claim("roles", resolveRoles(account))
                .claim("dataScope", resolveDataScope(account))
                .claim("groupIds", resolveGroupIds(account))
                .signWith(secretKey)
                .compact();
    }

    private String resolveProductRole(Account account) {
        String accountType = account.getAccountType();
        if ("ADMIN".equals(accountType) || "FINANCE".equals(accountType)) {
            return "ADMIN";
        }
        if ("AGENT".equals(accountType)) {
            return "AGENT";
        }
        return "USER";
    }

    private List<String> resolveRoles(Account account) {
        return List.of(resolveProductRole(account));
    }

    private String resolveDataScope(Account account) {
        String productRole = resolveProductRole(account);
        if ("ADMIN".equals(productRole)) {
            return "ALL";
        }
        if ("AGENT".equals(productRole)) {
            return "GROUP";
        }
        return "SELF";
    }

    private List<Long> resolveGroupIds(Account account) {
        if (!"AGENT".equals(account.getAccountType())) {
            return Collections.emptyList();
        }
        return agentAuthorizationService.loadAuthorizedGroupIds(account.getId());
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

}
