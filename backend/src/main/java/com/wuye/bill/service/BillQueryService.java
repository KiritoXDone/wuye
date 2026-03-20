package com.wuye.bill.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.dto.BillListQuery;
import com.wuye.bill.dto.AdminBillListQuery;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillLineMapper;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.bill.mapper.WaterReadingMapper;
import com.wuye.bill.vo.AdminWaterReadingVO;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillLineVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.PageResponse;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.coupon.service.CouponService;
import com.wuye.room.service.RoomBindingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BillQueryService {

    private final BillMapper billMapper;
    private final BillLineMapper billLineMapper;
    private final WaterReadingMapper waterReadingMapper;
    private final RoomBindingService roomBindingService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;

    public BillQueryService(BillMapper billMapper,
                            BillLineMapper billLineMapper,
                             WaterReadingMapper waterReadingMapper,
                             RoomBindingService roomBindingService,
                             AccessGuard accessGuard,
                             ObjectMapper objectMapper,
                             CouponService couponService) {
        this.billMapper = billMapper;
        this.billLineMapper = billLineMapper;
        this.waterReadingMapper = waterReadingMapper;
        this.roomBindingService = roomBindingService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
        this.couponService = couponService;
    }

    public PageResponse<BillListItemVO> listMyBills(LoginUser loginUser, BillListQuery query) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<BillListItemVO> list = billMapper.listByAccountId(loginUser.accountId(), query.getStatus(), query.getRoomId(), offset, pageSize);
        long total = billMapper.countByAccountId(loginUser.accountId(), query.getStatus(), query.getRoomId());
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    public PageResponse<BillListItemVO> listRoomBills(LoginUser loginUser, Long roomId) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        roomBindingService.myRoom(loginUser, roomId);
        List<BillListItemVO> list = billMapper.listByAccountIdAndRoom(loginUser.accountId(), roomId);
        return new PageResponse<>(list, 1, list.size() == 0 ? 20 : list.size(), list.size());
    }

    public BillDetailVO getBillDetail(LoginUser loginUser, Long billId) {
        Bill bill = billMapper.findById(billId);
        if (bill == null) {
            throw new BusinessException("NOT_FOUND", "账单不存在", HttpStatus.NOT_FOUND);
        }
        if (!loginUser.hasRole("ADMIN")) {
            accessGuard.requireRole(loginUser, "RESIDENT");
            accessGuard.requireSelfRoom(loginUser, roomBindingService.hasActiveBinding(loginUser.accountId(), bill.getRoomId()));
        }
        BillDetailVO detail = billMapper.findDetailById(billId);
        if (detail == null) {
            throw new BusinessException("NOT_FOUND", "账单不存在", HttpStatus.NOT_FOUND);
        }
        List<BillLineVO> lines = billLineMapper.findByBillId(billId);
        lines.forEach(line -> line.setExt(parseExt(line)));
        detail.setBillLines(lines);
        if (loginUser.hasRole("RESIDENT")) {
            detail.setAvailableCoupons(Collections.unmodifiableList(couponService.listBillCoupons(loginUser, bill)));
        } else {
            detail.setAvailableCoupons(Collections.emptyList());
        }
        return detail;
    }

    public PageResponse<BillListItemVO> listAdminBills(LoginUser loginUser, AdminBillListQuery query) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<BillListItemVO> list = billMapper.listAdminBills(query.getPeriodYear(), query.getPeriodMonth(), query.getFeeType(), query.getStatus(), offset, pageSize);
        long total = billMapper.countAdminBills(query.getPeriodYear(), query.getPeriodMonth(), query.getFeeType(), query.getStatus());
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    public List<AdminWaterReadingVO> listAdminWaterReadings(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        return waterReadingMapper.listAdminReadings(periodYear, periodMonth);
    }

    private Map<String, Object> parseExt(BillLineVO line) {
        if (line.getExtJson() == null || line.getExtJson().isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(line.getExtJson(), new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("failed to parse bill line ext json", ex);
        }
    }
}
