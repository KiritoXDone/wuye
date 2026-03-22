package com.wuye.auth.mapper;

import com.wuye.auth.entity.Account;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
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

    @Select("""
            <script>
            SELECT id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status, last_login_at
            FROM account
            <where>
                <if test='accountType != null and accountType != ""'>
                    account_type = #{accountType}
                </if>
            </where>
            ORDER BY id DESC
            </script>
            """)
    java.util.List<Account> listByAccountType(@Param("accountType") String accountType);

    @Select("""
            SELECT id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status, last_login_at
            FROM account
            WHERE username = #{username}
            LIMIT 1
            """)
    Account findAnyByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO account (account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status, created_at, updated_at)
            VALUES (#{accountNo}, #{accountType}, #{username}, #{passwordHash}, #{nickname}, #{mobile}, #{realName}, #{avatarUrl}, #{status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account account);

    @Update("UPDATE account SET status = #{status}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE account SET password_hash = #{passwordHash}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    @Update("UPDATE account SET last_login_at = #{lastLoginAt}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Update("UPDATE account SET status = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
