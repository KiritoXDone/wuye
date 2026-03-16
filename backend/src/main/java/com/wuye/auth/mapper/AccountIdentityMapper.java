package com.wuye.auth.mapper;

import com.wuye.auth.entity.AccountIdentity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountIdentityMapper {

    @Select("""
            SELECT id, account_id, platform, open_id, union_id, platform_user_id, status
            FROM account_identity
            WHERE platform = 'WECHAT' AND open_id = #{openId} AND status = 1
            """)
    AccountIdentity findWechatIdentity(String openId);
}
