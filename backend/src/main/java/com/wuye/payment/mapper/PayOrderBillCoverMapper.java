package com.wuye.payment.mapper;

import com.wuye.payment.entity.PayOrderBillCover;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PayOrderBillCoverMapper {

    @Insert("""
            INSERT INTO pay_order_bill_cover(pay_order_no, bill_id, room_id, period_year, period_month)
            VALUES(#{payOrderNo}, #{billId}, #{roomId}, #{periodYear}, #{periodMonth})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PayOrderBillCover cover);

    @Select("""
            SELECT id, pay_order_no, bill_id, room_id, period_year, period_month, created_at, updated_at
            FROM pay_order_bill_cover
            WHERE pay_order_no = #{payOrderNo}
            ORDER BY period_year ASC, period_month ASC, id ASC
            """)
    List<PayOrderBillCover> findByPayOrderNo(@Param("payOrderNo") String payOrderNo);
}
