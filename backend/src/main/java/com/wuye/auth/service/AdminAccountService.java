package com.wuye.auth.service;

import com.wuye.auth.dto.AccountStatusUpdateDTO;
import com.wuye.auth.dto.AdminAccountCreateDTO;
import com.wuye.auth.dto.AdminPasswordResetDTO;
import com.wuye.auth.entity.Account;
import com.wuye.auth.mapper.AccountMapper;
import com.wuye.auth.vo.AdminAccountVO;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.mapper.AccountRoomMapper;
import com.wuye.room.mapper.RoomMapper;
import com.wuye.room.vo.RoomVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminAccountService {

    private static final DateTimeFormatter ACCOUNT_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AccountMapper accountMapper;
    private final RoomMapper roomMapper;
    private final AccountRoomMapper accountRoomMapper;
    private final AccessGuard accessGuard;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AdminAccountService(AccountMapper accountMapper, RoomMapper roomMapper, AccountRoomMapper accountRoomMapper, AccessGuard accessGuard) {
        this.accountMapper = accountMapper;
        this.roomMapper = roomMapper;
        this.accountRoomMapper = accountRoomMapper;
        this.accessGuard = accessGuard;
    }

    public List<AdminAccountVO> listAccounts(LoginUser loginUser, String accountType) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return accountMapper.listByAccountType(normalizeAccountType(accountType)).stream().map(this::toAdminAccountVO).toList();
    }

    @Transactional
    public AdminAccountVO createAdmin(LoginUser loginUser, AdminAccountCreateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        String username = dto.getUsername().trim();
        if (accountMapper.findAnyByUsername(username) != null) {
            throw new BusinessException("CONFLICT", "用户名已存在", HttpStatus.CONFLICT);
        }
        Account account = new Account();
        account.setAccountNo(generateAccountNo());
        account.setAccountType("ADMIN");
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        account.setRealName(dto.getRealName().trim());
        account.setMobile(normalizeMobile(dto.getMobile()));
        account.setNickname(dto.getRealName().trim());
        account.setStatus(1);
        accountMapper.insert(account);
        return toAdminAccountVO(accountMapper.findById(account.getId()));
    }

    @Transactional
    public void updateStatus(LoginUser loginUser, Long accountId, AccountStatusUpdateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Account account = requireAdminAccount(accountId);
        if (loginUser.accountId().equals(accountId) && dto.getStatusValue() == 0) {
            throw new BusinessException("CONFLICT", "不能停用当前登录账户", HttpStatus.CONFLICT);
        }
        accountMapper.updateStatus(account.getId(), dto.getStatusValue());
    }

    @Transactional
    public void resetPassword(LoginUser loginUser, Long accountId, AdminPasswordResetDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Account account = requireAdminAccount(accountId);
        accountMapper.updatePasswordHash(account.getId(), passwordEncoder.encode(dto.getNewPassword()));
    }

    @Transactional
    public void deleteAccount(LoginUser loginUser, Long accountId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        if (loginUser.accountId().equals(accountId)) {
            throw new BusinessException("CONFLICT", "不能停用当前登录账户", HttpStatus.CONFLICT);
        }
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            throw new BusinessException("NOT_FOUND", "账户不存在", HttpStatus.NOT_FOUND);
        }
        if (account.getStatus() != null && account.getStatus() == 0) {
            return;
        }
        accountMapper.updateStatus(accountId, 0);
    }

    public List<RoomVO> listAccountRooms(LoginUser loginUser, Long accountId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            throw new BusinessException("NOT_FOUND", "账户不存在", HttpStatus.NOT_FOUND);
        }
        return roomMapper.listByAccountId(accountId);
    }

    @Transactional
    public void unbindAccountRoom(LoginUser loginUser, Long accountId, Long roomId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Account account = accountMapper.findById(accountId);
        if (account == null) {
            throw new BusinessException("NOT_FOUND", "账户不存在", HttpStatus.NOT_FOUND);
        }
        var binding = accountRoomMapper.findByAccountAndRoom(accountId, roomId);
        if (binding == null) {
            throw new BusinessException("NOT_FOUND", "绑定关系不存在", HttpStatus.NOT_FOUND);
        }
        if (!"ACTIVE".equals(binding.getStatus())) {
            throw new BusinessException("CONFLICT", "当前绑定已停用", HttpStatus.CONFLICT);
        }
        accountRoomMapper.updateStatusById(binding.getId(), "INACTIVE", null);
    }

    private Account requireAdminAccount(Long accountId) {
        Account account = accountMapper.findById(accountId);
        if (account == null || !"ADMIN".equals(account.getAccountType())) {
            throw new BusinessException("NOT_FOUND", "管理员账户不存在", HttpStatus.NOT_FOUND);
        }
        return account;
    }

    private AdminAccountVO toAdminAccountVO(Account account) {
        AdminAccountVO vo = new AdminAccountVO();
        vo.setId(account.getId());
        vo.setAccountNo(account.getAccountNo());
        vo.setAccountType(account.getAccountType());
        vo.setUsername(account.getUsername());
        vo.setRealName(account.getRealName());
        vo.setMobile(account.getMobile());
        vo.setStatus(account.getStatus());
        vo.setLastLoginAt(account.getLastLoginAt());
        return vo;
    }

    private String generateAccountNo() {
        return "ADM" + LocalDateTime.now().format(ACCOUNT_NO_FORMATTER);
    }

    private String normalizeMobile(String mobile) {
        if (mobile == null) {
            return null;
        }
        String normalized = mobile.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeAccountType(String accountType) {
        if (accountType == null) {
            return null;
        }
        String normalized = accountType.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }
}
