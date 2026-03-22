package com.wuye.bill.mapper;

import com.wuye.bill.entity.BillLine;
import com.wuye.bill.vo.BillLineVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BillLineMapper {

    @Insert("""
            INSERT INTO bill_line(bill_id, line_no, line_type, item_name, unit_price, quantity, line_amount, ext_json)
            VALUES(#{billId}, #{lineNo}, #{lineType}, #{itemName}, #{unitPrice}, #{quantity}, #{lineAmount}, #{extJson})
            """)
    int insert(BillLine billLine);

    @Select("""
            SELECT line_no, line_type, item_name, unit_price, quantity, line_amount, ext_json AS ext_json
            FROM bill_line
            WHERE bill_id = #{billId}
            ORDER BY line_no
            """)
    List<BillLineVO> findByBillId(@Param("billId") Long billId);

    @Delete("""
            DELETE FROM bill_line
            WHERE bill_id = #{billId}
            """)
    int deleteByBillId(@Param("billId") Long billId);
}
