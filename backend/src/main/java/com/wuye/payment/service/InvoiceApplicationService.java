package com.wuye.payment.service;

import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.NoGenerator;
import com.wuye.payment.dto.InvoiceApplicationCreateDTO;
import com.wuye.payment.dto.InvoiceApplicationProcessDTO;
import com.wuye.payment.entity.InvoiceApplication;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.mapper.InvoiceApplicationMapper;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.vo.InvoiceApplicationVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceApplicationService {

    private final InvoiceApplicationMapper invoiceApplicationMapper;
    private final PayOrderMapper payOrderMapper;
    private final BillMapper billMapper;
    private final PaymentAccessService paymentAccessService;
    private final AccessGuard accessGuard;

    public InvoiceApplicationService(InvoiceApplicationMapper invoiceApplicationMapper,
                                     PayOrderMapper payOrderMapper,
                                     BillMapper billMapper,
                                     PaymentAccessService paymentAccessService,
                                     AccessGuard accessGuard) {
        this.invoiceApplicationMapper = invoiceApplicationMapper;
        this.payOrderMapper = payOrderMapper;
        this.billMapper = billMapper;
        this.paymentAccessService = paymentAccessService;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public InvoiceApplicationVO apply(LoginUser loginUser, InvoiceApplicationCreateDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(dto.getPayOrderNo());
        if (payOrder == null || !"SUCCESS".equals(payOrder.getStatus()) || !dto.getBillId().equals(payOrder.getBillId())) {
            throw new BusinessException("CONFLICT", "仅已支付账单可申请发票", HttpStatus.CONFLICT);
        }
        Bill bill = billMapper.findById(dto.getBillId());
        paymentAccessService.requireResidentBillAccess(loginUser, bill);
        InvoiceApplication application = new InvoiceApplication();
        application.setApplicationNo("INV-" + NoGenerator.payOrderNo());
        application.setBillId(dto.getBillId());
        application.setPayOrderNo(dto.getPayOrderNo());
        application.setAccountId(loginUser.accountId());
        application.setInvoiceTitle(dto.getInvoiceTitle());
        application.setTaxNo(dto.getTaxNo());
        application.setStatus("APPLIED");
        application.setRemark("居民提交开票申请");
        application.setAppliedAt(LocalDateTime.now());
        invoiceApplicationMapper.insert(application);
        return invoiceApplicationMapper.findVOById(application.getId());
    }

    public List<InvoiceApplicationVO> myApplications(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        return invoiceApplicationMapper.listByAccountId(loginUser.accountId());
    }

    @Transactional
    public InvoiceApplicationVO process(LoginUser loginUser, Long applicationId, InvoiceApplicationProcessDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        InvoiceApplication application = invoiceApplicationMapper.findById(applicationId);
        if (application == null) {
            throw new BusinessException("NOT_FOUND", "发票申请不存在", HttpStatus.NOT_FOUND);
        }
        invoiceApplicationMapper.updateStatus(applicationId, dto.getStatus().toUpperCase(), dto.getRemark(), LocalDateTime.now());
        return invoiceApplicationMapper.findVOById(applicationId);
    }
}
