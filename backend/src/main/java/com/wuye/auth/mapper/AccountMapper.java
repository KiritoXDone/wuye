package com.wuye.auth.mapper;

import com.wuye.auth.entity.Account;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface AccountMapper {

    @Select("""
            SELECT id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status, last_login_at
            FROM account
            WHERE id = #{id}
            """)
    Account findById(Long id);

    @Select("""
            SELECT id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status, last_login_at
            FROM account
            WHERE username = #{username} AND status = 1
            """)
    Account findByUsername(String username);

    @Update("UPDATE account SET last_login_at = #{lastLoginAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
