package com.wuye.payment.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.payment.dto.InvoiceApplicationCreateDTO;
import com.wuye.payment.dto.InvoiceApplicationProcessDTO;
import com.wuye.payment.service.InvoiceApplicationService;
import com.wuye.payment.vo.InvoiceApplicationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InvoiceController {

    private final InvoiceApplicationService invoiceApplicationService;

    public InvoiceController(InvoiceApplicationService invoiceApplicationService) {
        this.invoiceApplicationService = invoiceApplicationService;
    }

    @PostMapping("/me/invoices/applications")
    public ApiResponse<InvoiceApplicationVO> apply(@CurrentUser LoginUser loginUser,
                                                   @Valid @RequestBody InvoiceApplicationCreateDTO dto) {
        return ApiResponse.success(invoiceApplicationService.apply(loginUser, dto));
    }

    @GetMapping("/me/invoices/applications")
    public ApiResponse<List<InvoiceApplicationVO>> myApplications(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(invoiceApplicationService.myApplications(loginUser));
    }

    @PostMapping("/admin/invoices/applications/{applicationId}/process")
    public ApiResponse<InvoiceApplicationVO> process(@CurrentUser LoginUser loginUser,
                                                     @PathVariable Long applicationId,
                                                     @Valid @RequestBody InvoiceApplicationProcessDTO dto) {
        return ApiResponse.success(invoiceApplicationService.process(loginUser, applicationId, dto));
    }
}
