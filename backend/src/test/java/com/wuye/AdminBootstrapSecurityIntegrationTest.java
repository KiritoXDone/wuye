package com.wuye;

import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBootstrapSecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    void bootstrapAdminPasswordIsNoLongerPersistedAsLegacyWeakDefault() {
        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM account WHERE username = 'admin'",
                String.class);
        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM account WHERE username = 'admin'",
                Integer.class);

        assertThat(passwordHash)
                .isNotBlank()
                .isNotEqualTo("{noop}123456")
                .isNotEqualTo("{noop}BOOTSTRAP_REQUIRED")
                .startsWith("{");
        assertThat(status).isEqualTo(1);
    }
}
