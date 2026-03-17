package com.wuye.importexport.mapper;

import com.wuye.importexport.entity.ExportJob;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExportJobMapper {
    @Insert("""
            INSERT INTO export_job(export_type, request_json, file_url, status, expired_at)
            VALUES(#{exportType}, #{requestJson}, #{fileUrl}, #{status}, #{expiredAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExportJob exportJob);

    @Select("""
            SELECT id, export_type, request_json, file_url, status, expired_at
            FROM export_job
            WHERE id = #{id}
            """)
    ExportJob findById(@Param("id") Long id);
}
