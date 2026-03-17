package com.wuye.importexport.mapper;

import com.wuye.importexport.entity.ImportRowError;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImportRowErrorMapper {
    @Insert("""
            INSERT INTO import_row_error(batch_id, row_no, error_code, error_message, raw_data)
            VALUES(#{batchId}, #{rowNo}, #{errorCode}, #{errorMessage}, #{rawData})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImportRowError importRowError);

    @Select("""
            SELECT id, batch_id, row_no, error_code, error_message, raw_data
            FROM import_row_error
            WHERE batch_id = #{batchId}
            ORDER BY row_no ASC, id ASC
            """)
    List<ImportRowError> listByBatchId(@Param("batchId") Long batchId);
}
