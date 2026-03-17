package com.wuye.audit.mapper;

import com.wuye.audit.entity.AuditLog;
import com.wuye.audit.vo.AuditLogVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditLogMapper {

    @Insert("""
            INSERT INTO audit_log(biz_type, biz_id, action, operator_id, ip, user_agent, detail_json)
            VALUES(#{bizType}, #{bizId}, #{action}, #{operatorId}, #{ip}, #{userAgent}, #{detailJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AuditLog auditLog);

    @Select("""
            SELECT id,
                   biz_type,
                   biz_id,
                   action,
                   operator_id,
                   ip,
                   user_agent,
                   detail_json,
                   created_at
            FROM audit_log
            WHERE (#{bizType} IS NULL OR #{bizType} = '' OR biz_type = #{bizType})
              AND (#{bizId} IS NULL OR #{bizId} = '' OR biz_id = #{bizId})
              AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
              AND (#{createdAtStart} IS NULL OR created_at >= #{createdAtStart})
              AND (#{createdAtEnd} IS NULL OR created_at <= #{createdAtEnd})
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<AuditLogVO> listPage(@Param("bizType") String bizType,
                              @Param("bizId") String bizId,
                              @Param("operatorId") Long operatorId,
                              @Param("createdAtStart") LocalDateTime createdAtStart,
                              @Param("createdAtEnd") LocalDateTime createdAtEnd,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM audit_log
            WHERE (#{bizType} IS NULL OR #{bizType} = '' OR biz_type = #{bizType})
              AND (#{bizId} IS NULL OR #{bizId} = '' OR biz_id = #{bizId})
              AND (#{operatorId} IS NULL OR operator_id = #{operatorId})
              AND (#{createdAtStart} IS NULL OR created_at >= #{createdAtStart})
              AND (#{createdAtEnd} IS NULL OR created_at <= #{createdAtEnd})
            """)
    long count(@Param("bizType") String bizType,
               @Param("bizId") String bizId,
               @Param("operatorId") Long operatorId,
               @Param("createdAtStart") LocalDateTime createdAtStart,
               @Param("createdAtEnd") LocalDateTime createdAtEnd);
}
