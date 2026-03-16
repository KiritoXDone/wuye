package com.wuye.auth.service;

import com.wuye.auth.entity.Account;
import com.wuye.auth.vo.LoginVO;
import com.wuye.common.security.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final String issuer;
    private final long expireHours;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.expire-hours}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.expireHours = expireHours;
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
        vo.setRoles(List.of(account.getAccountType()));
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
        String accountType = claims.get("accountType", String.class);
        String realName = claims.get("realName", String.class);
        List<String> roles = readStringList(claims.get("roles"));
        String dataScope = claims.get("dataScope", String.class);
        List<Long> groupIds = readLongList(claims.get("groupIds"));
        return new LoginUser(accountId, accountType, realName, roles, dataScope, groupIds);
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
                .claim("realName", account.getRealName())
                .claim("roles", List.of(account.getAccountType()))
                .claim("dataScope", resolveDataScope(account.getAccountType()))
                .claim("groupIds", Collections.emptyList())
                .signWith(secretKey)
                .compact();
    }

    private String resolveDataScope(String accountType) {
        if ("ADMIN".equals(accountType) || "FINANCE".equals(accountType)) {
            return "ALL";
        }
        if ("AGENT".equals(accountType)) {
            return "GROUP";
        }
        return "SELF";
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private List<String> readStringList(Object claim) {
        if (!(claim instanceof List<?> values)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value != null) {
                result.add(String.valueOf(value));
            }
        }
        return result;
    }

    private List<Long> readLongList(Object claim) {
        if (!(claim instanceof List<?> values)) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Number number) {
                result.add(number.longValue());
            } else if (value != null) {
                result.add(Long.parseLong(Objects.toString(value)));
            }
        }
        return result;
    }
}
