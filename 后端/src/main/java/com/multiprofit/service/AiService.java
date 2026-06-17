package com.multiprofit.service;

import com.multiprofit.dto.AiChatRequest;
import com.multiprofit.dto.AiChatResponse;
import com.multiprofit.model.AlertRecord;
import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 */
public interface AiService {

    /**
     * 自然语言数据问答
     */
    AiChatResponse chat(AiChatRequest request);

    /**
     * AI数据校验 - 根因分析
     */
    String analyzeAnomaly(String metricName, String currentValue,
                          String previousValue, double changeRate, String context);

    /**
     * AI自动生成经营简报
     */
    String generateBusinessBrief(String period, String scope);

    /**
     * NL2SQL - 自然语言转查询
     */
    String naturalLanguageToSql(String question);

    /**
     * AI解读数据
     */
    String interpretData(String metricName, Map<String, Object> data);
}
