package com.wuye.auth.service;

import com.wuye.auth.dto.AdminPasswordLoginDTO;
import com.wuye.auth.dto.RefreshTokenDTO;
import com.wuye.auth.dto.WechatLoginDTO;
import com.wuye.auth.entity.Account;
import com.wuye.auth.entity.AccountIdentity;
import com.wuye.auth.mapper.AccountIdentityMapper;
import com.wuye.auth.mapper.AccountMapper;
import com.wuye.auth.vo.LoginVO;
import com.wuye.auth.vo.ProfileVO;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.service.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final AccountIdentityMapper accountIdentityMapper;
    private final JwtService jwtService;
    private final CouponService couponService;
    private final WechatAuthClient wechatAuthClient;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AuthService(AccountMapper accountMapper,
                       AccountIdentityMapper accountIdentityMapper,
                       JwtService jwtService,
                       CouponService couponService,
                       WechatAuthClient wechatAuthClient) {
        this.accountMapper = accountMapper;
        this.accountIdentityMapper = accountIdentityMapper;
        this.jwtService = jwtService;
        this.couponService = couponService;
        this.wechatAuthClient = wechatAuthClient;
    }

    public LoginVO loginWechat(WechatLoginDTO dto) {
        WechatAuthClient.WechatSession wechatSession = wechatAuthClient.exchangeCode(dto.getCode());
        AccountIdentity identity = accountIdentityMapper.findWechatIdentity(wechatSession.openId());
        if (identity == null) {
            throw new BusinessException("UNAUTHORIZED", "微信账号未绑定本地住户", HttpStatus.UNAUTHORIZED);
        }
        Account account = loadEnabledAccount(identity.getAccountId());
        accountMapper.updateLastLoginAt(account.getId(), LocalDateTime.now());
        couponService.issueLoginCoupons(account);
        return jwtService.issueLogin(account);
    }

    public LoginVO loginAdmin(AdminPasswordLoginDTO dto) {
        Account account = accountMapper.findByUsername(dto.getUsername());
        if (account == null || !isAdminProductRole(account.getAccountType())) {
            throw new BusinessException("UNAUTHORIZED", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), account.getPasswordHash())) {
            throw new BusinessException("UNAUTHORIZED", "用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }
        accountMapper.updateLastLoginAt(account.getId(), LocalDateTime.now());
        return jwtService.issueLogin(account);
    }

    public LoginVO refresh(RefreshTokenDTO dto) {
        LoginUser loginUser = jwtService.parseRefreshToken(dto.getRefreshToken());
        Account account = loadEnabledAccount(loginUser.accountId());
        return jwtService.issueLogin(account);
    }

    public ProfileVO currentProfile(LoginUser loginUser) {
        ProfileVO vo = new ProfileVO();
        vo.setAccountId(loginUser.accountId());
        vo.setAccountType(loginUser.accountType());
        vo.setProductRole(loginUser.productRole());
        vo.setRealName(loginUser.realName());
        vo.setRoles(loginUser.roles());
        return vo;
    }

    public void logout(LoginUser loginUser) {
        accountMapper.updateTokenInvalidBefore(loginUser.accountId(), LocalDateTime.now());
    }

    private Account loadEnabledAccount(Long accountId) {
        Account account = accountMapper.findById(accountId);
        if (account == null || account.getStatus() == null || account.getStatus() != 1) {
            throw new BusinessException("UNAUTHORIZED", "账号不存在或已停用", HttpStatus.UNAUTHORIZED);
        }
        return account;
    }

    private boolean isAdminProductRole(String accountType) {
        return "ADMIN".equals(accountType) || "FINANCE".equals(accountType);
    }
}
