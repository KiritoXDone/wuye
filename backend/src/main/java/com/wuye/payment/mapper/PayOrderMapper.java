package com.wuye.payment.mapper;

import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.vo.PaymentStatusVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PayOrderMapper {

    @Insert("""
            INSERT INTO pay_order(pay_order_no, bill_id, account_id, channel, origin_amount, discount_amount, pay_amount,
                                  coupon_instance_id, idempotency_key, status, channel_trade_no, paid_at, expired_at, close_reason,
                                  is_annual_payment, covered_bill_count)
            VALUES(#{payOrderNo}, #{billId}, #{accountId}, #{channel}, #{originAmount}, #{discountAmount}, #{payAmount},
                   #{couponInstanceId}, #{idempotencyKey}, #{status}, #{channelTradeNo}, #{paidAt}, #{expiredAt}, #{closeReason},
                   #{annualPayment}, #{coveredBillCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PayOrder payOrder);

    @Select("""
            SELECT id, pay_order_no, bill_id, account_id, channel, origin_amount, discount_amount, pay_amount,
                   coupon_instance_id, idempotency_key, status, channel_trade_no, paid_at, expired_at, close_reason,
                   is_annual_payment AS annual_payment, covered_bill_count
            FROM pay_order
            WHERE idempotency_key = #{idempotencyKey}
            """)
    PayOrder findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    @Select("""
            SELECT id, pay_order_no, bill_id, account_id, channel, origin_amount, discount_amount, pay_amount,
                   coupon_instance_id, idempotency_key, status, channel_trade_no, paid_at, expired_at, close_reason,
                   is_annual_payment AS annual_payment, covered_bill_count
            FROM pay_order
            WHERE pay_order_no = #{payOrderNo}
            """)
    PayOrder findByPayOrderNo(@Param("payOrderNo") String payOrderNo);

    @Select("""
            SELECT id, pay_order_no, bill_id, account_id, channel, origin_amount, discount_amount, pay_amount,
                   coupon_instance_id, idempotency_key, status, channel_trade_no, paid_at, expired_at, close_reason,
                   is_annual_payment AS annual_payment, covered_bill_count
            FROM pay_order
            WHERE pay_order_no = #{payOrderNo}
            FOR UPDATE
            """)
    PayOrder findByPayOrderNoForUpdate(@Param("payOrderNo") String payOrderNo);

    @Update("""
            UPDATE pay_order
            SET status = #{status},
                channel_trade_no = #{channelTradeNo},
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE pay_order_no = #{payOrderNo}
              AND status IN ('PAYING', 'CREATED')
            """)
    int updateSuccess(@Param("payOrderNo") String payOrderNo,
                      @Param("status") String status,
                      @Param("channelTradeNo") String channelTradeNo,
                      @Param("paidAt") LocalDateTime paidAt);

    @Select("""
            SELECT pay_order_no, bill_id, status, paid_at,
                   is_annual_payment AS annual_payment,
                   covered_bill_count
            FROM pay_order
            WHERE pay_order_no = #{payOrderNo}
            """)
    PaymentStatusVO findStatus(@Param("payOrderNo") String payOrderNo);
}
