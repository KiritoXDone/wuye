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
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AuthService(AccountMapper accountMapper,
                       AccountIdentityMapper accountIdentityMapper,
                       JwtService jwtService,
                       CouponService couponService) {
        this.accountMapper = accountMapper;
        this.accountIdentityMapper = accountIdentityMapper;
        this.jwtService = jwtService;
        this.couponService = couponService;
    }

    public LoginVO loginWechat(WechatLoginDTO dto) {
        AccountIdentity identity = accountIdentityMapper.findWechatIdentity(dto.getCode());
        if (identity == null) {
            throw new BusinessException("UNAUTHORIZED", "微信登录 code 无效", HttpStatus.UNAUTHORIZED);
        }
        Account account = loadEnabledAccount(identity.getAccountId());
        rejectAgentAccount(account);
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
        rejectAgentAccount(account);
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

    private void rejectAgentAccount(Account account) {
        if ("AGENT".equals(account.getAccountType())) {
            throw new BusinessException("UNAUTHORIZED", "该账户类型已停止产品登录", HttpStatus.UNAUTHORIZED);
        }
    }
}
