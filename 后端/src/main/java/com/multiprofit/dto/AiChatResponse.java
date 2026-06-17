package com.multiprofit.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * AI对话响应
 */
@Data
public class AiChatResponse {
    /** AI回答文本 */
    private String answer;
    /** 查询到的数据（可选） */
    private List<Map<String, Object>> data;
    /** 建议的图表类型 */
    private String chartType;
    /** 图表数据 */
    private Object chartData;
}
