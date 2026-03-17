package com.wuye.payment.mapper;

import com.wuye.payment.entity.DunningLog;
import com.wuye.payment.vo.DunningLogVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DunningLogMapper {

    @Insert("""
            INSERT INTO dunning_log(task_id, bill_id, send_channel, status, content, sent_at)
            VALUES(#{taskId}, #{billId}, #{sendChannel}, #{status}, #{content}, #{sentAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DunningLog log);

    @Select("""
            SELECT id,
                   task_id,
                   bill_id,
                   send_channel,
                   status,
                   content,
                   sent_at
            FROM dunning_log
            WHERE bill_id = #{billId}
            ORDER BY id DESC
            """)
    List<DunningLogVO> listByBillId(@Param("billId") Long billId);
}
