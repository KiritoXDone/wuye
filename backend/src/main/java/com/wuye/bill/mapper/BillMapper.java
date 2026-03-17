package com.wuye.bill.mapper;

import com.wuye.bill.entity.Bill;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BillMapper {

    @Insert("""
            INSERT INTO bill(bill_no, room_id, fee_type, period_year, period_month, amount_due, discount_amount_total,
                             amount_paid, due_date, status, source_type, remark)
            VALUES(#{billNo}, #{roomId}, #{feeType}, #{periodYear}, #{periodMonth}, #{amountDue}, #{discountAmountTotal},
                   #{amountPaid}, #{dueDate}, #{status}, #{sourceType}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Bill bill);

    @Select("""
            SELECT id, bill_no, room_id, fee_type, period_year, period_month, amount_due, discount_amount_total,
                   amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id = #{roomId}
              AND fee_type = #{feeType}
              AND period_year = #{year}
              AND period_month = #{month}
            """)
    Bill findByUniqueKey(@Param("roomId") Long roomId,
                         @Param("feeType") String feeType,
                         @Param("year") Integer year,
                         @Param("month") Integer month);

    @Select("""
            SELECT id, bill_no, room_id, fee_type, period_year, period_month, amount_due, discount_amount_total,
                   amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE id = #{billId}
            """)
    Bill findById(@Param("billId") Long billId);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   b.fee_type,
                   CONCAT(b.period_year, '-', LPAD(b.period_month, 2, '0')) AS period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
            ORDER BY b.period_year DESC, b.period_month DESC, b.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<BillListItemVO> listByAccountId(@Param("accountId") Long accountId,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM bill b
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
            """)
    long countByAccountId(@Param("accountId") Long accountId, @Param("status") String status);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   b.fee_type,
                   CONCAT(b.period_year, '-', LPAD(b.period_month, 2, '0')) AS period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            WHERE (#{periodYear} IS NULL OR b.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR b.period_month = #{periodMonth})
              AND (#{feeType} IS NULL OR #{feeType} = '' OR b.fee_type = #{feeType})
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
            ORDER BY b.period_year DESC, b.period_month DESC, b.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<BillListItemVO> listAdminBills(@Param("periodYear") Integer periodYear,
                                        @Param("periodMonth") Integer periodMonth,
                                        @Param("feeType") String feeType,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM bill b
            WHERE (#{periodYear} IS NULL OR b.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR b.period_month = #{periodMonth})
              AND (#{feeType} IS NULL OR #{feeType} = '' OR b.fee_type = #{feeType})
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
            """)
    long countAdminBills(@Param("periodYear") Integer periodYear,
                         @Param("periodMonth") Integer periodMonth,
                         @Param("feeType") String feeType,
                         @Param("status") String status);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   b.fee_type,
                   b.period_year,
                   b.period_month,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            WHERE b.id = #{billId}
            """)
    BillDetailVO findDetailById(@Param("billId") Long billId);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   b.fee_type,
                   CONCAT(b.period_year, '-', LPAD(b.period_month, 2, '0')) AS period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND b.room_id = #{roomId}
            ORDER BY b.period_year DESC, b.period_month DESC, b.id DESC
            """)
    List<BillListItemVO> listByAccountIdAndRoom(@Param("accountId") Long accountId, @Param("roomId") Long roomId);

    @Update("""
            UPDATE bill
            SET amount_paid = #{amountPaid},
                status = 'PAID',
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{billId}
            """)
    int markPaid(@Param("billId") Long billId,
                 @Param("amountPaid") BigDecimal amountPaid,
                 @Param("paidAt") LocalDateTime paidAt);
}
