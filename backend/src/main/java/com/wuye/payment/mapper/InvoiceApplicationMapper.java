package com.wuye.payment.mapper;

import com.wuye.payment.entity.InvoiceApplication;
import com.wuye.payment.vo.InvoiceApplicationVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InvoiceApplicationMapper {

    @Insert("""
            INSERT INTO invoice_application(application_no, bill_id, pay_order_no, account_id, invoice_title, tax_no,
                                            status, remark, applied_at, processed_at)
            VALUES(#{applicationNo}, #{billId}, #{payOrderNo}, #{accountId}, #{invoiceTitle}, #{taxNo},
                   #{status}, #{remark}, #{appliedAt}, #{processedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InvoiceApplication application);

    @Select("""
            SELECT id, application_no, bill_id, pay_order_no, account_id, invoice_title, tax_no,
                   status, remark, applied_at, processed_at
            FROM invoice_application
            WHERE id = #{id}
            """)
    InvoiceApplication findById(@Param("id") Long id);

    @Select("""
            SELECT id,
                   application_no,
                   bill_id,
                   pay_order_no,
                   invoice_title,
                   tax_no,
                   status,
                   remark,
                   applied_at,
                   processed_at
            FROM invoice_application
            WHERE id = #{id}
            """)
    InvoiceApplicationVO findVOById(@Param("id") Long id);

    @Select("""
            SELECT id,
                   application_no,
                   bill_id,
                   pay_order_no,
                   invoice_title,
                   tax_no,
                   status,
                   remark,
                   applied_at,
                   processed_at
            FROM invoice_application
            WHERE account_id = #{accountId}
            ORDER BY id DESC
            """)
    List<InvoiceApplicationVO> listByAccountId(@Param("accountId") Long accountId);

    @Update("""
            UPDATE invoice_application
            SET status = #{status},
                remark = #{remark},
                processed_at = #{processedAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("remark") String remark,
                     @Param("processedAt") LocalDateTime processedAt);
}
