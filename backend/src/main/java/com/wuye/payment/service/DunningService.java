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
import java.util.List;

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
        for (Bill bill : billMapper.listOverdueBills(triggerDate)) {
            if (dunningTaskMapper.findByUniqueKey(bill.getId(), triggerType, triggerDate) != null) {
                continue;
            }
            UserGroup group = bill.getGroupId() == null ? null : userGroupMapper.findById(bill.getGroupId());
            OrgUnit orgUnit = group == null || group.getOrgUnitId() == null ? null : orgUnitMapper.findById(group.getOrgUnitId());
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
}
