package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import com.multiprofit.service.DataValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validation")
public class DataValidationController {

    @Autowired
    private DataValidationService validationService;

    /**
     * 执行异常检测
     */
    @PostMapping("/detect")
    public ApiResponse<List<DataValidationService.ValidationResult>> detectAnomaly(
            @RequestParam(required = false, defaultValue = "2026-05") String period,
            @RequestParam(required = false, defaultValue = "ORG") String dimType) {
        return ApiResponse.ok(validationService.detectAnomaly(period, dimType));
    }
}
