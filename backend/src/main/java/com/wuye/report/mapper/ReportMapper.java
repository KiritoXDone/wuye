package com.wuye.report.mapper;

import com.wuye.report.vo.AdminMonthlyReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReportMapper {

    @Select("""
            SELECT #{periodYear} AS period_year,
                   #{periodMonth} AS period_month,
                   COUNT(DISTINCT CASE WHEN status = 'PAID' THEN room_id END) AS paid_count,
                   COUNT(DISTINCT room_id) AS total_count,
                   COALESCE(SUM(amount_paid), 0) AS paid_amount,
                   COALESCE(SUM(discount_amount_total), 0) AS discount_amount,
                   COALESCE(SUM(amount_due - amount_paid), 0) AS unpaid_amount
            FROM bill
            WHERE period_year = #{periodYear}
              AND period_month = #{periodMonth}
            """)
    AdminMonthlyReportVO monthly(@Param("periodYear") Integer periodYear, @Param("periodMonth") Integer periodMonth);
}
