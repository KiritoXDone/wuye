package com.wuye.payment.mapper;

import com.wuye.payment.entity.PaymentVoucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentVoucherMapper {

    @Insert("""
            INSERT INTO payment_voucher(pay_order_no, bill_id, account_id, voucher_no, amount, status, issued_at, content_json)
            VALUES(#{payOrderNo}, #{billId}, #{accountId}, #{voucherNo}, #{amount}, #{status}, #{issuedAt}, #{contentJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PaymentVoucher voucher);

    @Select("""
            SELECT id, pay_order_no, bill_id, account_id, voucher_no, amount, status, issued_at, content_json
            FROM payment_voucher
            WHERE pay_order_no = #{payOrderNo}
            """)
    PaymentVoucher findByPayOrderNo(@Param("payOrderNo") String payOrderNo);
}
