package com.wuye.coupon.mapper;

import com.wuye.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponTemplateMapper {
    @Insert("""
            INSERT INTO coupon_template(template_code, type, fee_type, name, discount_mode, value_amount, threshold_amount,
                                        valid_from, valid_to, stackable, status)
            VALUES(#{templateCode}, #{type}, #{feeType}, #{name}, #{discountMode}, #{valueAmount}, #{thresholdAmount},
                   #{validFrom}, #{validTo}, #{stackable}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CouponTemplate couponTemplate);

    @Select("""
            SELECT id, template_code, type, fee_type, name, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE status = 1
            ORDER BY id DESC
            """)
    List<CouponTemplate> listActive();

    @Select("""
            SELECT id, template_code, type, fee_type, name, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE template_code = #{templateCode}
            """)
    CouponTemplate findByTemplateCode(@Param("templateCode") String templateCode);

    @Select("""
            SELECT id, template_code, type, fee_type, name, discount_mode, value_amount, threshold_amount,
                   valid_from, valid_to, stackable, status
            FROM coupon_template
            WHERE id = #{id}
            """)
    CouponTemplate findById(@Param("id") Long id);
}
