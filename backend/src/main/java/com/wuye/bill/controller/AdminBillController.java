package com.wuye.bill.controller;

import com.wuye.bill.dto.PropertyBillGenerateDTO;
import com.wuye.bill.dto.AdminBillListQuery;
import com.wuye.bill.dto.WaterBillGenerateDTO;
import com.wuye.bill.dto.WaterMeterCreateDTO;
import com.wuye.bill.dto.WaterReadingCreateDTO;
import com.wuye.bill.entity.WaterMeter;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.service.BillQueryService;
import com.wuye.bill.service.PropertyBillGenerateService;
import com.wuye.bill.service.WaterBillGenerateService;
import com.wuye.bill.service.WaterReadingService;
import com.wuye.bill.vo.AdminWaterReadingVO;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBillController {

    private final PropertyBillGenerateService propertyBillGenerateService;
    private final WaterReadingService waterReadingService;
    private final WaterBillGenerateService waterBillGenerateService;
    private final BillQueryService billQueryService;

    public AdminBillController(PropertyBillGenerateService propertyBillGenerateService,
                               WaterReadingService waterReadingService,
                               WaterBillGenerateService waterBillGenerateService,
                               BillQueryService billQueryService) {
        this.propertyBillGenerateService = propertyBillGenerateService;
        this.waterReadingService = waterReadingService;
        this.waterBillGenerateService = waterBillGenerateService;
        this.billQueryService = billQueryService;
    }

    @GetMapping("/bills")
    public ApiResponse<PageResponse<BillListItemVO>> listBills(@CurrentUser LoginUser loginUser, AdminBillListQuery query) {
        return ApiResponse.success(billQueryService.listAdminBills(loginUser, query));
    }

    @GetMapping("/bills/{billId}")
    public ApiResponse<BillDetailVO> billDetail(@CurrentUser LoginUser loginUser, @PathVariable Long billId) {
        return ApiResponse.success(billQueryService.getBillDetail(loginUser, billId));
    }

    @PostMapping("/bills/generate/property-yearly")
    public ApiResponse<Map<String, Object>> generatePropertyYearly(@CurrentUser LoginUser loginUser,
                                                                   @Valid @RequestBody PropertyBillGenerateDTO dto) {
        return ApiResponse.success(Map.of("generatedCount", propertyBillGenerateService.generate(loginUser, dto)));
    }

    @PostMapping("/bills/generate/property")
    public ApiResponse<Map<String, Object>> generateProperty(@CurrentUser LoginUser loginUser,
                                                             @Valid @RequestBody PropertyBillGenerateDTO dto) {
        return generatePropertyYearly(loginUser, dto);
    }

    @PostMapping("/water-meters")
    public ApiResponse<WaterMeter> createWaterMeter(@CurrentUser LoginUser loginUser,
                                                    @Valid @RequestBody WaterMeterCreateDTO dto) {
        return ApiResponse.success(waterReadingService.createOrUpdateMeter(loginUser, dto));
    }

    @GetMapping("/water-readings")
    public ApiResponse<List<AdminWaterReadingVO>> listWaterReadings(@CurrentUser LoginUser loginUser,
                                                                    @RequestParam(value = "periodYear", required = false) Integer periodYear,
                                                                    @RequestParam(value = "periodMonth", required = false) Integer periodMonth) {
        return ApiResponse.success(billQueryService.listAdminWaterReadings(loginUser, periodYear, periodMonth));
    }

    @PostMapping("/water-readings")
    public ApiResponse<Map<String, Object>> createWaterReading(@CurrentUser LoginUser loginUser,
                                                                @Valid @RequestBody WaterReadingCreateDTO dto) {
        return ApiResponse.success(waterReadingService.createReading(loginUser, dto));
    }

    @PostMapping("/bills/generate/water")
    public ApiResponse<Map<String, Object>> generateWater(@CurrentUser LoginUser loginUser,
                                                          @Valid @RequestBody WaterBillGenerateDTO dto) {
        return ApiResponse.success(Map.of("generatedCount", waterBillGenerateService.generate(loginUser, dto)));
    }
}
