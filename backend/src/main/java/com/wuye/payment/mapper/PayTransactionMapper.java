package com.wuye.payment.mapper;

import com.wuye.payment.entity.PayTransaction;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayTransactionMapper {

    @Insert("""
            INSERT INTO pay_transaction(pay_order_no, trade_type, request_json, response_json, transaction_status, error_code, error_message)
            VALUES(#{payOrderNo}, #{tradeType}, #{requestJson}, #{responseJson}, #{transactionStatus}, #{errorCode}, #{errorMessage})
            """)
    int insert(PayTransaction payTransaction);
}
