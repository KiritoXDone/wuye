package com.wuye.bill.controller;

import com.wuye.bill.dto.PropertyBillGenerateDTO;
import com.wuye.bill.dto.WaterBillGenerateDTO;
import com.wuye.bill.dto.WaterMeterCreateDTO;
import com.wuye.bill.dto.WaterReadingCreateDTO;
import com.wuye.bill.entity.WaterMeter;
import com.wuye.bill.entity.WaterMeterReading;
import com.wuye.bill.service.PropertyBillGenerateService;
import com.wuye.bill.service.WaterBillGenerateService;
import com.wuye.bill.service.WaterReadingService;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminBillController {

    private final PropertyBillGenerateService propertyBillGenerateService;
    private final WaterReadingService waterReadingService;
    private final WaterBillGenerateService waterBillGenerateService;

    public AdminBillController(PropertyBillGenerateService propertyBillGenerateService,
                               WaterReadingService waterReadingService,
                               WaterBillGenerateService waterBillGenerateService) {
        this.propertyBillGenerateService = propertyBillGenerateService;
        this.waterReadingService = waterReadingService;
        this.waterBillGenerateService = waterBillGenerateService;
    }

    @PostMapping("/bills/generate/property")
    public ApiResponse<Map<String, Object>> generateProperty(@CurrentUser LoginUser loginUser,
                                                             @Valid @RequestBody PropertyBillGenerateDTO dto) {
        return ApiResponse.success(Map.of("generatedCount", propertyBillGenerateService.generate(loginUser, dto)));
    }

    @PostMapping("/water-meters")
    public ApiResponse<WaterMeter> createWaterMeter(@CurrentUser LoginUser loginUser,
                                                    @Valid @RequestBody WaterMeterCreateDTO dto) {
        return ApiResponse.success(waterReadingService.createOrUpdateMeter(loginUser, dto));
    }

    @PostMapping("/water-readings")
    public ApiResponse<WaterMeterReading> createWaterReading(@CurrentUser LoginUser loginUser,
                                                             @Valid @RequestBody WaterReadingCreateDTO dto) {
        return ApiResponse.success(waterReadingService.createReading(loginUser, dto));
    }

    @PostMapping("/bills/generate/water")
    public ApiResponse<Map<String, Object>> generateWater(@CurrentUser LoginUser loginUser,
                                                          @Valid @RequestBody WaterBillGenerateDTO dto) {
        return ApiResponse.success(Map.of("generatedCount", waterBillGenerateService.generate(loginUser, dto)));
    }
}
