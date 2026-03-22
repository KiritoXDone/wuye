package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponExchangeRecord;
import com.wuye.coupon.vo.AdminVoucherExchangeVO;
import com.wuye.coupon.vo.ResidentVoucherVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponExchangeRecordMapper {
    @Insert("""
            INSERT INTO coupon_exchange_record(coupon_instance_id, template_id, owner_account_id, goods_name, goods_spec, exchange_status, pickup_site, remark)
            VALUES(#{couponInstanceId}, #{templateId}, #{ownerAccountId}, #{goodsName}, #{goodsSpec}, #{exchangeStatus}, #{pickupSite}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponExchangeRecord record);

    @Select("""
            SELECT id, coupon_instance_id, template_id, owner_account_id, goods_name, goods_spec, exchange_status, pickup_site, remark, created_at, updated_at
            FROM coupon_exchange_record
            WHERE coupon_instance_id = #{couponInstanceId}
            LIMIT 1
            """)
    CouponExchangeRecord findByCouponInstanceId(@Param("couponInstanceId") Long couponInstanceId);

    @Select("""
            SELECT ci.id AS coupon_instance_id,
                   ct.id AS template_id,
                   ct.template_code,
                   ct.name,
                   ct.goods_name,
                   ct.goods_spec,
                   ct.redeem_instruction,
                   ci.status,
                   cer.exchange_status,
                   COALESCE(cer.pickup_site, ct.redeem_instruction) AS pickup_site,
                   ci.issued_at,
                   ci.expires_at
            FROM coupon_instance ci
            JOIN coupon_template ct ON ct.id = ci.template_id
            LEFT JOIN coupon_exchange_record cer ON cer.coupon_instance_id = ci.id
            WHERE ci.owner_account_id = #{accountId}
              AND ct.type = 'VOUCHER'
            ORDER BY ci.issued_at DESC, ci.id DESC
            """)
    List<ResidentVoucherVO> listResidentVouchers(@Param("accountId") Long accountId);

    @Select("""
            SELECT cer.id AS exchange_id,
                   cer.coupon_instance_id,
                   cer.template_id,
                   ct.name AS template_name,
                   cer.owner_account_id,
                   COALESCE(a.real_name, a.username, a.account_no) AS owner_account_name,
                   cer.goods_name,
                   cer.goods_spec,
                   cer.exchange_status,
                   cer.pickup_site,
                   cer.remark,
                   cer.created_at
            FROM coupon_exchange_record cer
            JOIN coupon_template ct ON ct.id = cer.template_id
            LEFT JOIN account a ON a.id = cer.owner_account_id
            WHERE cer.template_id = #{templateId}
            ORDER BY cer.created_at DESC, cer.id DESC
            """)
    List<AdminVoucherExchangeVO> listByTemplateId(@Param("templateId") Long templateId);

    @Update("""
            UPDATE coupon_exchange_record
            SET exchange_status = #{exchangeStatus},
                remark = #{remark},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("exchangeStatus") String exchangeStatus,
                     @Param("remark") String remark);
}
