package com.wuye.importexport.mapper;

import com.wuye.importexport.entity.ImportBatch;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImportBatchMapper {
    @Insert("""
            INSERT INTO import_batch(batch_no, import_type, file_url, status, total_count, success_count, fail_count)
            VALUES(#{batchNo}, #{importType}, #{fileUrl}, #{status}, #{totalCount}, #{successCount}, #{failCount})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImportBatch importBatch);

    @Select("""
            SELECT id, batch_no, import_type, file_url, status, total_count, success_count, fail_count
            FROM import_batch
            WHERE id = #{id}
            """)
    ImportBatch findById(@Param("id") Long id);
}
