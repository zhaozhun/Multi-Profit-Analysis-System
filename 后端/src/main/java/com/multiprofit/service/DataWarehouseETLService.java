package com.multiprofit.service;

import java.util.Map;

public interface DataWarehouseETLService {
    /**
     * 执行ETL流程
     * @param period 期间（2025-06）
     * @return 执行结果
     */
    Map<String, Object> executeETL(String period);
}
