package com.wuye.auth.service;

import com.wuye.auth.entity.Account;
import com.wuye.auth.mapper.AccountMapper;
import com.wuye.common.config.AppBootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);
    private static final String BOOTSTRAP_REQUIRED_HASH = "{noop}BOOTSTRAP_REQUIRED";
    private static final String LEGACY_DEFAULT_HASH = "{noop}123456";

    private final AccountMapper accountMapper;
    private final AppBootstrapProperties appBootstrapProperties;
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    public AdminBootstrapInitializer(AccountMapper accountMapper,
                                     AppBootstrapProperties appBootstrapProperties) {
        this.accountMapper = accountMapper;
        this.appBootstrapProperties = appBootstrapProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = normalize(appBootstrapProperties.getAdmin().getUsername());
        if (username == null) {
            return;
        }
        Account account = accountMapper.findAnyByUsername(username);
        if (account == null || !"ADMIN".equals(account.getAccountType())) {
            return;
        }
        String password = normalize(appBootstrapProperties.getAdmin().getPassword());
        if (password == null) {
            if (BOOTSTRAP_REQUIRED_HASH.equals(account.getPasswordHash()) || LEGACY_DEFAULT_HASH.equals(account.getPasswordHash())) {
                log.warn("bootstrap admin '{}' is disabled until APP_BOOTSTRAP_ADMIN_PASSWORD is provided", username);
            }
            return;
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            accountMapper.updatePasswordHash(account.getId(), passwordEncoder.encode(password));
            accountMapper.updateTokenInvalidBefore(account.getId(), LocalDateTime.now());
        }
        if (account.getStatus() == null || account.getStatus() != 1) {
            accountMapper.updateStatus(account.getId(), 1);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
