package com.wuye.agent.mapper;

import com.wuye.agent.entity.OrgUnit;
import com.wuye.agent.vo.OrgUnitVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgUnitMapper {

    @Select("""
            SELECT id, tenant_code, org_code, name, parent_id, community_id, status
            FROM org_unit
            WHERE id = #{id}
            """)
    OrgUnit findById(@Param("id") Long id);

    @Select("""
            SELECT ou.id,
                   ou.tenant_code,
                   ou.org_code,
                   ou.name,
                   ou.parent_id,
                   parent.name AS parent_name,
                   ou.community_id
            FROM org_unit ou
            LEFT JOIN org_unit parent ON parent.id = ou.parent_id
            WHERE ou.status = 1
            ORDER BY ou.id
            """)
    List<OrgUnitVO> listAll();
}
