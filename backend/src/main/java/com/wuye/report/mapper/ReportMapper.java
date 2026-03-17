package com.wuye.report.mapper;

import com.wuye.report.vo.AgentMonthlyReportVO;
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

    @Select("""
            SELECT COUNT(DISTINCT CASE WHEN b.status = 'PAID' THEN b.room_id END) AS paid_count,
                   COUNT(DISTINCT b.room_id) AS total_count,
                   COALESCE(SUM(b.amount_paid), 0) AS paid_amount,
                   COALESCE(SUM(b.discount_amount_total), 0) AS discount_amount,
                   COALESCE(SUM(b.amount_due - b.amount_paid), 0) AS unpaid_amount
            FROM bill b
            JOIN group_room gr ON gr.room_id = b.room_id
            WHERE gr.group_id = #{groupId}
              AND b.period_year = #{periodYear}
              AND b.period_month = #{periodMonth}
            """)
    AgentMonthlyReportVO agentMonthly(@Param("groupId") Long groupId,
                                      @Param("periodYear") Integer periodYear,
                                      @Param("periodMonth") Integer periodMonth);
}
