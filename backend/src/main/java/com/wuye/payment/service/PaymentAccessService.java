package com.wuye.payment.service;

import com.wuye.bill.entity.Bill;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.room.service.RoomBindingService;
import org.springframework.stereotype.Service;

@Service
public class PaymentAccessService {

    private final AccessGuard accessGuard;
    private final RoomBindingService roomBindingService;

    public PaymentAccessService(AccessGuard accessGuard, RoomBindingService roomBindingService) {
        this.accessGuard = accessGuard;
        this.roomBindingService = roomBindingService;
    }

    public void requireResidentBillAccess(LoginUser loginUser, Bill bill) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        accessGuard.requireSelfRoom(
                loginUser,
                bill != null && roomBindingService.hasActiveBinding(loginUser.accountId(), bill.getRoomId())
        );
    }

    public boolean isAdmin(LoginUser loginUser) {
        return loginUser != null && loginUser.hasRole("ADMIN");
    }
}
