package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponSeckillCampaign;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponSeckillCampaignMapper {

    @Insert("""
            INSERT INTO coupon_seckill_campaign(campaign_code, template_id, title, total_stock, available_stock, per_user_limit, start_at, end_at, status)
            VALUES(#{campaignCode}, #{templateId}, #{title}, #{totalStock}, #{availableStock}, #{perUserLimit}, #{startAt}, #{endAt}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponSeckillCampaign campaign);

    @Select("""
            SELECT id, campaign_code, template_id, title, total_stock, available_stock, per_user_limit, start_at, end_at, status
            FROM coupon_seckill_campaign
            WHERE id = #{id}
            """)
    CouponSeckillCampaign findById(@Param("id") Long id);

    @Select("""
            SELECT id, campaign_code, template_id, title, total_stock, available_stock, per_user_limit, start_at, end_at, status
            FROM coupon_seckill_campaign
            WHERE id = #{id}
            FOR UPDATE
            """)
    CouponSeckillCampaign findByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE coupon_seckill_campaign
            SET available_stock = available_stock - 1,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND available_stock > 0
              AND status = 1
            """)
    int decrementStock(@Param("id") Long id);
}
