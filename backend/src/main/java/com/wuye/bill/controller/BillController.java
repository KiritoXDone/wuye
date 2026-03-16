package com.wuye.bill.controller;

import com.wuye.bill.dto.BillListQuery;
import com.wuye.bill.service.BillQueryService;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BillController {

    private final BillQueryService billQueryService;

    public BillController(BillQueryService billQueryService) {
        this.billQueryService = billQueryService;
    }

    @GetMapping("/me/bills")
    public ApiResponse<PageResponse<BillListItemVO>> myBills(@CurrentUser LoginUser loginUser, BillListQuery query) {
        return ApiResponse.success(billQueryService.listMyBills(loginUser, query));
    }

    @GetMapping("/bills/{billId}")
    public ApiResponse<BillDetailVO> billDetail(@CurrentUser LoginUser loginUser, @PathVariable Long billId) {
        return ApiResponse.success(billQueryService.getBillDetail(loginUser, billId));
    }
}
