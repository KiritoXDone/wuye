package com.wuye.payment.service;

import com.wuye.agent.entity.OrgUnit;
import com.wuye.agent.entity.UserGroup;
import com.wuye.agent.mapper.OrgUnitMapper;
import com.wuye.agent.mapper.UserGroupMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.NoGenerator;
import com.wuye.payment.dto.DunningTriggerDTO;
import com.wuye.payment.entity.DunningLog;
import com.wuye.payment.entity.DunningTask;
import com.wuye.payment.mapper.DunningLogMapper;
import com.wuye.payment.mapper.DunningTaskMapper;
import com.wuye.payment.vo.DunningLogVO;
import com.wuye.payment.vo.DunningTaskVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DunningService {

    private final BillMapper billMapper;
    private final DunningTaskMapper dunningTaskMapper;
    private final DunningLogMapper dunningLogMapper;
    private final UserGroupMapper userGroupMapper;
    private final OrgUnitMapper orgUnitMapper;
    private final AccessGuard accessGuard;

    public DunningService(BillMapper billMapper,
                          DunningTaskMapper dunningTaskMapper,
                          DunningLogMapper dunningLogMapper,
                          UserGroupMapper userGroupMapper,
                          OrgUnitMapper orgUnitMapper,
                          AccessGuard accessGuard) {
        this.billMapper = billMapper;
        this.dunningTaskMapper = dunningTaskMapper;
        this.dunningLogMapper = dunningLogMapper;
        this.userGroupMapper = userGroupMapper;
        this.orgUnitMapper = orgUnitMapper;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public List<DunningTaskVO> trigger(LoginUser loginUser, DunningTriggerDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        LocalDate triggerDate = dto == null || dto.getTriggerDate() == null ? LocalDate.now() : dto.getTriggerDate();
        return triggerInternal(triggerDate, "MANUAL");
    }

    @Transactional
    public List<DunningTaskVO> triggerScheduled(LocalDate triggerDate) {
        return triggerInternal(triggerDate, "AUTO");
    }

    public List<DunningTaskVO> listTasks(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return dunningTaskMapper.listAll();
    }

    public List<DunningLogVO> listLogs(LoginUser loginUser, Long billId) {
        accessGuard.requireRole(loginUser, "ADMIN");
        return dunningLogMapper.listByBillId(billId);
    }

    private List<DunningTaskVO> triggerInternal(LocalDate triggerDate, String triggerType) {
        List<DunningTaskVO> created = new ArrayList<>();
        List<Bill> overdueBills = billMapper.listOverdueBills(triggerDate);
        if (overdueBills.isEmpty()) {
            return dunningTaskMapper.listAll();
        }
        Set<Long> existingBillIds = findExistingBillIds(overdueBills, triggerType, triggerDate);
        Map<Long, UserGroup> groupMap = loadGroupMap(overdueBills);
        Map<Long, OrgUnit> orgUnitMap = loadOrgUnitMap(groupMap.values());
        for (Bill bill : overdueBills) {
            if (existingBillIds.contains(bill.getId())) {
                continue;
            }
            UserGroup group = bill.getGroupId() == null ? null : groupMap.get(bill.getGroupId());
            OrgUnit orgUnit = group == null || group.getOrgUnitId() == null ? null : orgUnitMap.get(group.getOrgUnitId());
            DunningTask task = new DunningTask();
            task.setTaskNo("DUN-" + NoGenerator.payOrderNo());
            task.setBillId(bill.getId());
            task.setGroupId(bill.getGroupId());
            task.setOrgUnitId(orgUnit == null ? null : orgUnit.getId());
            task.setTenantCode(orgUnit == null ? null : orgUnit.getTenantCode());
            task.setTriggerType(triggerType);
            task.setTriggerDate(triggerDate);
            task.setStatus("SENT");
            task.setRemark("账单逾期自动催缴");
            task.setExecutedAt(LocalDateTime.now());
            dunningTaskMapper.insert(task);

            DunningLog log = new DunningLog();
            log.setTaskId(task.getId());
            log.setBillId(bill.getId());
            log.setSendChannel("SYSTEM");
            log.setStatus("SENT");
            log.setContent("账单 " + bill.getBillNo() + " 已逾期，系统已生成催缴提醒");
            log.setSentAt(task.getExecutedAt());
            dunningLogMapper.insert(log);
        }
        created.addAll(dunningTaskMapper.listAll());
        return created;
    }

    private Set<Long> findExistingBillIds(List<Bill> overdueBills, String triggerType, LocalDate triggerDate) {
        List<Long> billIds = overdueBills.stream().map(Bill::getId).toList();
        return dunningTaskMapper.listExistingBillIds(billIds, triggerType, triggerDate).stream()
                .collect(Collectors.toSet());
    }

    private Map<Long, UserGroup> loadGroupMap(List<Bill> overdueBills) {
        Set<Long> groupIds = overdueBills.stream()
                .map(Bill::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (groupIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, UserGroup> result = new HashMap<>();
        for (UserGroup group : userGroupMapper.listByIds(groupIds)) {
            result.put(group.getId(), group);
        }
        return result;
    }

    private Map<Long, OrgUnit> loadOrgUnitMap(Iterable<UserGroup> groups) {
        Set<Long> orgUnitIds = new java.util.HashSet<>();
        for (UserGroup group : groups) {
            if (group.getOrgUnitId() != null) {
                orgUnitIds.add(group.getOrgUnitId());
            }
        }
        if (orgUnitIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, OrgUnit> result = new HashMap<>();
        for (OrgUnit orgUnit : orgUnitMapper.listByIds(orgUnitIds)) {
            result.put(orgUnit.getId(), orgUnit);
        }
        return result;
    }
}
