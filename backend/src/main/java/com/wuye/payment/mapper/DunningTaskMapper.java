package com.wuye.payment.mapper;

import com.wuye.payment.entity.DunningTask;
import com.wuye.payment.vo.DunningTaskVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DunningTaskMapper {

    @Insert("""
            INSERT INTO dunning_task(task_no, bill_id, group_id, org_unit_id, tenant_code, trigger_type,
                                     trigger_date, status, remark, executed_at)
            VALUES(#{taskNo}, #{billId}, #{groupId}, #{orgUnitId}, #{tenantCode}, #{triggerType},
                   #{triggerDate}, #{status}, #{remark}, #{executedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DunningTask task);

    @Select("""
            SELECT id, task_no, bill_id, group_id, org_unit_id, tenant_code, trigger_type,
                   trigger_date, status, remark, executed_at
            FROM dunning_task
            WHERE bill_id = #{billId}
              AND trigger_type = #{triggerType}
              AND trigger_date = #{triggerDate}
            """)
    DunningTask findByUniqueKey(@Param("billId") Long billId,
                                @Param("triggerType") String triggerType,
                                @Param("triggerDate") LocalDate triggerDate);

    @Select("""
            SELECT id,
                   task_no,
                   bill_id,
                   group_id,
                   org_unit_id,
                   tenant_code,
                   trigger_type,
                   trigger_date,
                   status,
                   remark,
                   executed_at
            FROM dunning_task
            ORDER BY id DESC
            """)
    List<DunningTaskVO> listAll();
}
