package com.wuye.bill.mapper;

import com.wuye.bill.entity.Bill;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.bill.vo.HouseholdPaymentOverviewVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BillMapper {

    @Insert("""
            INSERT INTO bill(bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, period_month_key, service_period_start, service_period_end,
                             amount_due, discount_amount_total, amount_paid, due_date, status, source_type, remark)
            VALUES(#{billNo}, #{roomId}, #{groupId}, #{feeType}, #{cycleType}, #{periodYear}, #{periodMonth}, COALESCE(#{periodMonth}, 0), #{servicePeriodStart}, #{servicePeriodEnd},
                   #{amountDue}, #{discountAmountTotal}, #{amountPaid}, #{dueDate}, #{status}, #{sourceType}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Bill bill);

    @Select("""
            <script>
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id = #{roomId}
              AND fee_type = #{feeType}
              AND period_year = #{year}
              AND status IN ('ISSUED', 'PAID')
              <choose>
                <when test='month != null'>
                  AND period_month = #{month}
                </when>
                <otherwise>
                  AND period_month IS NULL
                </otherwise>
              </choose>
            </script>
            """)
    Bill findByUniqueKey(@Param("roomId") Long roomId,
                         @Param("feeType") String feeType,
                         @Param("year") Integer year,
                         @Param("month") Integer month);

    @Select("""
            <script>
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id = #{roomId}
              AND fee_type = #{feeType}
              AND period_year = #{year}
              <choose>
                <when test='month != null'>
                  AND period_month = #{month}
                </when>
                <otherwise>
                  AND period_month IS NULL
                </otherwise>
              </choose>
            LIMIT 1
            </script>
            """)
    Bill findAnyByUniqueKey(@Param("roomId") Long roomId,
                            @Param("feeType") String feeType,
                            @Param("year") Integer year,
                            @Param("month") Integer month);

    @Select("""
            <script>
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id IN
            <foreach collection="roomIds" item="roomId" open="(" separator="," close=")">
                #{roomId}
            </foreach>
              AND fee_type = #{feeType}
              AND period_year = #{year}
              AND status IN ('ISSUED', 'PAID')
              <choose>
                <when test='month != null'>
                  AND period_month = #{month}
                </when>
                <otherwise>
                  AND period_month IS NULL
                </otherwise>
              </choose>
            </script>
            """)
    List<Bill> listActiveByRoomIdsAndPeriod(@Param("roomIds") Collection<Long> roomIds,
                                            @Param("feeType") String feeType,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month);

    @Select("""
            <script>
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id IN
            <foreach collection="roomIds" item="roomId" open="(" separator="," close=")">
                #{roomId}
            </foreach>
              AND fee_type = #{feeType}
              AND period_year = #{year}
              <choose>
                <when test='month != null'>
                  AND period_month = #{month}
                </when>
                <otherwise>
                  AND period_month IS NULL
                </otherwise>
              </choose>
            ORDER BY id ASC
            </script>
            """)
    List<Bill> listByRoomIdsAndPeriod(@Param("roomIds") Collection<Long> roomIds,
                                      @Param("feeType") String feeType,
                                      @Param("year") Integer year,
                                      @Param("month") Integer month);

    @Select("""
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE id = #{billId}
            """)
    Bill findById(@Param("billId") Long billId);

    @Select("""
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE id = #{billId}
            FOR UPDATE
            """)
    Bill findByIdForUpdate(@Param("billId") Long billId);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   rt.type_name AS room_type_name,
                   b.fee_type,
                   b.cycle_type,
                   CASE
                       WHEN b.cycle_type = 'YEAR' THEN CONCAT(b.period_year, '年度')
                       ELSE CONCAT(b.period_year, '-', LPAD(COALESCE(b.period_month, 0), 2, '0'))
                   END AS period,
                   CASE
                       WHEN b.service_period_start IS NOT NULL AND b.service_period_end IS NOT NULL
                           THEN CONCAT(b.service_period_start, ' ~ ', b.service_period_end)
                       ELSE NULL
                   END AS service_period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            LEFT JOIN room_type rt ON rt.id = r.room_type_id
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND b.status IN ('ISSUED', 'PAID')
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
              AND (#{roomId} IS NULL OR b.room_id = #{roomId})
              AND (
                  (b.fee_type = 'PROPERTY' AND NOT (b.period_year > #{currentYear}))
                  OR (
                      b.fee_type = 'WATER'
                      AND (
                          NOT (b.period_year >= #{currentYear})
                          OR (b.period_year = #{currentYear} AND NOT (COALESCE(b.period_month, 0) > #{currentMonth}))
                      )
                  )
                  OR (b.fee_type NOT IN ('PROPERTY', 'WATER'))
              )
            ORDER BY b.period_year DESC, COALESCE(b.period_month, 0) DESC, b.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<BillListItemVO> listByAccountId(@Param("accountId") Long accountId,
                                         @Param("status") String status,
                                         @Param("roomId") Long roomId,
                                         @Param("currentYear") int currentYear,
                                         @Param("currentMonth") int currentMonth,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM bill b
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND b.status IN ('ISSUED', 'PAID')
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
              AND (#{roomId} IS NULL OR b.room_id = #{roomId})
              AND (
                  (b.fee_type = 'PROPERTY' AND NOT (b.period_year > #{currentYear}))
                  OR (
                      b.fee_type = 'WATER'
                      AND (
                          NOT (b.period_year >= #{currentYear})
                          OR (b.period_year = #{currentYear} AND NOT (COALESCE(b.period_month, 0) > #{currentMonth}))
                      )
                  )
                  OR (b.fee_type NOT IN ('PROPERTY', 'WATER'))
              )
            """)
    long countByAccountId(@Param("accountId") Long accountId,
                          @Param("status") String status,
                          @Param("roomId") Long roomId,
                          @Param("currentYear") int currentYear,
                          @Param("currentMonth") int currentMonth);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   rt.type_name AS room_type_name,
                   b.fee_type,
                   b.cycle_type,
                   CASE
                       WHEN b.cycle_type = 'YEAR' THEN CONCAT(b.period_year, '年度')
                       ELSE CONCAT(b.period_year, '-', LPAD(COALESCE(b.period_month, 0), 2, '0'))
                   END AS period,
                   CASE
                       WHEN b.service_period_start IS NOT NULL AND b.service_period_end IS NOT NULL
                           THEN CONCAT(b.service_period_start, ' ~ ', b.service_period_end)
                       ELSE NULL
                   END AS service_period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            LEFT JOIN room_type rt ON rt.id = r.room_type_id
            WHERE (#{periodYear} IS NULL OR b.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR b.period_month = #{periodMonth})
              AND (#{feeType} IS NULL OR #{feeType} = '' OR b.fee_type = #{feeType})
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
              AND (#{roomId} IS NULL OR b.room_id = #{roomId})
            ORDER BY b.period_year DESC, COALESCE(b.period_month, 0) DESC, b.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<BillListItemVO> listAdminBills(@Param("periodYear") Integer periodYear,
                                        @Param("periodMonth") Integer periodMonth,
                                        @Param("feeType") String feeType,
                                        @Param("status") String status,
                                        @Param("roomId") Long roomId,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1)
            FROM bill b
            WHERE (#{periodYear} IS NULL OR b.period_year = #{periodYear})
              AND (#{periodMonth} IS NULL OR b.period_month = #{periodMonth})
              AND (#{feeType} IS NULL OR #{feeType} = '' OR b.fee_type = #{feeType})
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
              AND (#{roomId} IS NULL OR b.room_id = #{roomId})
            """)
    long countAdminBills(@Param("periodYear") Integer periodYear,
                         @Param("periodMonth") Integer periodMonth,
                         @Param("feeType") String feeType,
                         @Param("status") String status,
                         @Param("roomId") Long roomId);

    @Select("""
            <script>
            SELECT r.id AS room_id,
                   r.community_id,
                   c.name AS community_name,
                   r.building_no,
                   r.unit_no,
                   r.room_no,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   rt.type_name AS room_type_name,
                   r.area_m2,
                   bp.id AS property_bill_id,
                   bp.bill_no AS property_bill_no,
                   bp.amount_due AS property_amount_due,
                   bp.amount_paid AS property_amount_paid,
                   COALESCE(bp.status, 'MISSING') AS property_status,
                   bp.due_date AS property_due_date,
                   bp.paid_at AS property_paid_at,
                   bw.id AS water_bill_id,
                   bw.bill_no AS water_bill_no,
                   bw.amount_due AS water_amount_due,
                   bw.amount_paid AS water_amount_paid,
                   COALESCE(bw.status, 'MISSING') AS water_status,
                   bw.due_date AS water_due_date,
                   bw.paid_at AS water_paid_at
            FROM room r
            JOIN community c ON c.id = r.community_id
            LEFT JOIN room_type rt ON rt.id = r.room_type_id
            LEFT JOIN bill bp ON bp.id = (
                SELECT b1.id
                FROM bill b1
                WHERE b1.room_id = r.id
                  AND b1.fee_type = 'PROPERTY'
                  AND b1.period_year = #{periodYear}
                  AND b1.period_month IS NULL
                ORDER BY CASE b1.status
                    WHEN 'ISSUED' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'CANCELLED' THEN 2
                    ELSE 3
                END, b1.id DESC
                LIMIT 1
            )
            LEFT JOIN bill bw ON bw.id = (
                SELECT b2.id
                FROM bill b2
                WHERE b2.room_id = r.id
                  AND b2.fee_type = 'WATER'
                  AND b2.period_year = #{periodYear}
                  AND b2.period_month = #{periodMonth}
                ORDER BY CASE b2.status
                    WHEN 'ISSUED' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'CANCELLED' THEN 2
                    ELSE 3
                END, b2.id DESC
                LIMIT 1
            )
            WHERE r.status = 1
              <if test='communityId != null'>
                AND r.community_id = #{communityId}
              </if>
              <if test='buildingNo != null and buildingNo != ""'>
                AND r.building_no = #{buildingNo}
              </if>
              <if test='unitNo != null and unitNo != ""'>
                AND r.unit_no = #{unitNo}
              </if>
              <if test='roomKeyword != null and roomKeyword != ""'>
                AND (
                  r.room_no LIKE CONCAT('%', #{roomKeyword}, '%')
                  OR CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) LIKE CONCAT('%', #{roomKeyword}, '%')
                )
              </if>
              <if test='propertyStatus != null and propertyStatus != ""'>
                <choose>
                  <when test='propertyStatus == "MISSING"'>
                    AND bp.id IS NULL
                  </when>
                  <otherwise>
                    AND bp.status = #{propertyStatus}
                  </otherwise>
                </choose>
              </if>
              <if test='waterStatus != null and waterStatus != ""'>
                <choose>
                  <when test='waterStatus == "MISSING"'>
                    AND bw.id IS NULL
                  </when>
                  <otherwise>
                    AND bw.status = #{waterStatus}
                  </otherwise>
                </choose>
              </if>
            ORDER BY r.community_id ASC, r.building_no ASC, r.unit_no ASC, r.room_no ASC, r.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<HouseholdPaymentOverviewVO> listAdminHouseholdOverview(@Param("communityId") Long communityId,
                                                                @Param("periodYear") Integer periodYear,
                                                                @Param("periodMonth") Integer periodMonth,
                                                                @Param("buildingNo") String buildingNo,
                                                                @Param("unitNo") String unitNo,
                                                                @Param("roomKeyword") String roomKeyword,
                                                                @Param("propertyStatus") String propertyStatus,
                                                                @Param("waterStatus") String waterStatus,
                                                                @Param("offset") int offset,
                                                                @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM room r
            LEFT JOIN bill bp ON bp.id = (
                SELECT b1.id
                FROM bill b1
                WHERE b1.room_id = r.id
                  AND b1.fee_type = 'PROPERTY'
                  AND b1.period_year = #{periodYear}
                  AND b1.period_month IS NULL
                ORDER BY CASE b1.status
                    WHEN 'ISSUED' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'CANCELLED' THEN 2
                    ELSE 3
                END, b1.id DESC
                LIMIT 1
            )
            LEFT JOIN bill bw ON bw.id = (
                SELECT b2.id
                FROM bill b2
                WHERE b2.room_id = r.id
                  AND b2.fee_type = 'WATER'
                  AND b2.period_year = #{periodYear}
                  AND b2.period_month = #{periodMonth}
                ORDER BY CASE b2.status
                    WHEN 'ISSUED' THEN 0
                    WHEN 'PAID' THEN 1
                    WHEN 'CANCELLED' THEN 2
                    ELSE 3
                END, b2.id DESC
                LIMIT 1
            )
            WHERE r.status = 1
              <if test='communityId != null'>
                AND r.community_id = #{communityId}
              </if>
              <if test='buildingNo != null and buildingNo != ""'>
                AND r.building_no = #{buildingNo}
              </if>
              <if test='unitNo != null and unitNo != ""'>
                AND r.unit_no = #{unitNo}
              </if>
              <if test='roomKeyword != null and roomKeyword != ""'>
                AND (
                  r.room_no LIKE CONCAT('%', #{roomKeyword}, '%')
                  OR CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) LIKE CONCAT('%', #{roomKeyword}, '%')
                )
              </if>
              <if test='propertyStatus != null and propertyStatus != ""'>
                <choose>
                  <when test='propertyStatus == "MISSING"'>
                    AND bp.id IS NULL
                  </when>
                  <otherwise>
                    AND bp.status = #{propertyStatus}
                  </otherwise>
                </choose>
              </if>
              <if test='waterStatus != null and waterStatus != ""'>
                <choose>
                  <when test='waterStatus == "MISSING"'>
                    AND bw.id IS NULL
                  </when>
                  <otherwise>
                    AND bw.status = #{waterStatus}
                  </otherwise>
                </choose>
              </if>
            </script>
            """)
    long countAdminHouseholdOverview(@Param("communityId") Long communityId,
                                     @Param("periodYear") Integer periodYear,
                                     @Param("periodMonth") Integer periodMonth,
                                     @Param("buildingNo") String buildingNo,
                                     @Param("unitNo") String unitNo,
                                     @Param("roomKeyword") String roomKeyword,
                                     @Param("propertyStatus") String propertyStatus,
                                     @Param("waterStatus") String waterStatus);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   rt.type_name AS room_type_name,
                   b.fee_type,
                   b.cycle_type,
                   b.period_year,
                   b.period_month,
                   b.service_period_start,
                   b.service_period_end,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            LEFT JOIN room_type rt ON rt.id = r.room_type_id
            WHERE b.id = #{billId}
            """)
    BillDetailVO findDetailById(@Param("billId") Long billId);

    @Select("""
            SELECT b.id AS bill_id,
                   b.bill_no,
                   b.room_id,
                   CONCAT(r.building_no, '-', r.unit_no, '-', r.room_no) AS room_label,
                   rt.type_name AS room_type_name,
                   b.fee_type,
                   b.cycle_type,
                   CASE
                       WHEN b.cycle_type = 'YEAR' THEN CONCAT(b.period_year, '年度')
                       ELSE CONCAT(b.period_year, '-', LPAD(COALESCE(b.period_month, 0), 2, '0'))
                   END AS period,
                   CASE
                       WHEN b.service_period_start IS NOT NULL AND b.service_period_end IS NOT NULL
                           THEN CONCAT(b.service_period_start, ' ~ ', b.service_period_end)
                       ELSE NULL
                   END AS service_period,
                   b.amount_due,
                   b.amount_paid,
                   b.status,
                   b.due_date
            FROM bill b
            JOIN room r ON r.id = b.room_id
            LEFT JOIN room_type rt ON rt.id = r.room_type_id
            JOIN account_room ar ON ar.room_id = b.room_id
            WHERE ar.account_id = #{accountId}
              AND ar.status = 'ACTIVE'
              AND b.status IN ('ISSUED', 'PAID')
              AND b.room_id = #{roomId}
              AND (#{status} IS NULL OR #{status} = '' OR b.status = #{status})
              AND (
                  (b.fee_type = 'PROPERTY' AND NOT (b.period_year > #{currentYear}))
                  OR (
                      b.fee_type = 'WATER'
                      AND (
                          NOT (b.period_year >= #{currentYear})
                          OR (b.period_year = #{currentYear} AND NOT (COALESCE(b.period_month, 0) > #{currentMonth}))
                      )
                  )
                  OR (b.fee_type NOT IN ('PROPERTY', 'WATER'))
              )
            ORDER BY b.period_year DESC, COALESCE(b.period_month, 0) DESC, b.id DESC
            """)
    List<BillListItemVO> listByAccountIdAndRoom(@Param("accountId") Long accountId,
                                                @Param("roomId") Long roomId,
                                                @Param("status") String status,
                                                @Param("currentYear") int currentYear,
                                                @Param("currentMonth") int currentMonth);

    @Update("""
            UPDATE bill
            SET amount_paid = #{amountPaid},
                status = 'PAID',
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{billId}
              AND status = 'ISSUED'
            """)
    int markPaid(@Param("billId") Long billId,
                 @Param("amountPaid") BigDecimal amountPaid,
                 @Param("paidAt") LocalDateTime paidAt);

    @Update("""
            UPDATE bill
            SET amount_paid = amount_due - COALESCE(discount_amount_total, 0),
                status = 'PAID',
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP,
                remark = CASE
                    WHEN #{remark} IS NULL OR #{remark} = '' THEN remark
                    WHEN remark IS NULL OR remark = '' THEN #{remark}
                    ELSE CONCAT(remark, '；', #{remark})
                END
            WHERE id = #{billId}
              AND status = 'ISSUED'
            """)
    int markPaidOffline(@Param("billId") Long billId,
                        @Param("paidAt") LocalDateTime paidAt,
                        @Param("remark") String remark);

    @Update("""
            UPDATE bill
            SET discount_amount_total = #{discountAmount},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{billId}
            """)
    int updateDiscountAmount(@Param("billId") Long billId, @Param("discountAmount") BigDecimal discountAmount);

    @Select("""
            SELECT id, bill_no, room_id, group_id, fee_type, cycle_type, period_year, period_month, service_period_start, service_period_end,
                   amount_due, discount_amount_total, amount_paid, due_date, status, paid_at, cancelled_at, source_type, remark
            FROM bill
            WHERE room_id = #{roomId}
              AND fee_type = #{feeType}
              AND period_year = #{periodYear}
            ORDER BY COALESCE(period_month, 0) ASC, id ASC
            """)
    List<Bill> listByRoomFeeTypeAndYear(@Param("roomId") Long roomId,
                                        @Param("feeType") String feeType,
                                        @Param("periodYear") Integer periodYear);

    @Update("""
            <script>
            UPDATE bill
            SET amount_paid = amount_due,
                status = 'PAID',
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP,
                remark = CASE
                    WHEN remark IS NULL OR remark = '' THEN #{remark}
                    ELSE CONCAT(remark, '；', #{remark})
                END
            WHERE status = 'ISSUED'
              AND id IN
              <foreach collection="billIds" item="billId" open="(" separator="," close=")">
                #{billId}
              </foreach>
            </script>
            """)
    int markPaidByIds(@Param("billIds") List<Long> billIds,
                      @Param("paidAt") LocalDateTime paidAt,
                      @Param("remark") String remark);

    @Select("""
            SELECT b.id, b.bill_no, b.room_id, b.group_id, b.fee_type, b.cycle_type, b.period_year, b.period_month, b.service_period_start,
                   b.service_period_end, b.amount_due, b.discount_amount_total, b.amount_paid, b.due_date, b.status, b.paid_at, b.cancelled_at,
                   b.source_type, b.remark
            FROM bill b
            WHERE b.status = 'ISSUED'
              AND b.due_date < #{triggerDate}
            ORDER BY b.due_date ASC, b.id ASC
            """)
    List<Bill> listOverdueBills(@Param("triggerDate") LocalDate triggerDate);

    @Select("""
            SELECT COUNT(1)
            FROM bill
            WHERE room_id = #{roomId}
              AND status IN ('ISSUED', 'PAID')
            """)
    long countByRoomId(@Param("roomId") Long roomId);

    @Select("""
            SELECT COUNT(1)
            FROM bill
            WHERE room_id = #{roomId}
              AND fee_type = 'WATER'
              AND period_year = #{periodYear}
              AND period_month = #{periodMonth}
              AND status IN ('ISSUED', 'PAID')
            """)
    long countWaterBillsByRoomAndPeriod(@Param("roomId") Long roomId,
                                        @Param("periodYear") Integer periodYear,
                                        @Param("periodMonth") Integer periodMonth);

    @Update("""
            UPDATE bill
            SET status = 'CANCELLED',
                cancelled_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{billId}
              AND status = 'ISSUED'
            """)
    int deleteById(@Param("billId") Long billId);

    @Update("""
            UPDATE bill
            SET bill_no = #{billNo},
                group_id = #{groupId},
                cycle_type = #{cycleType},
                period_month = #{periodMonth},
                service_period_start = #{servicePeriodStart},
                service_period_end = #{servicePeriodEnd},
                amount_due = #{amountDue},
                discount_amount_total = #{discountAmountTotal},
                amount_paid = #{amountPaid},
                due_date = #{dueDate},
                status = #{status},
                paid_at = #{paidAt},
                cancelled_at = #{cancelledAt},
                source_type = #{sourceType},
                remark = #{remark},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND status = 'CANCELLED'
            """)
    int reissueCancelled(Bill bill);
}
