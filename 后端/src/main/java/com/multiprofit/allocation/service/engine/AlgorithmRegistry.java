package com.multiprofit.allocation.service.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 算法注册中心
 * 负责管理所有分摊算法的注册和获取
 */
@Slf4j
@Component
public class AlgorithmRegistry {

    /**
     * 算法存储: algorithmCode -> algorithm
     */
    private final Map<String, AllocationAlgorithm> algorithms = new ConcurrentHashMap<>();

    /**
     * 自动注册所有实现AllocationAlgorithm接口的Bean
     */
    @Autowired
    public void registerAlgorithms(List<AllocationAlgorithm> algorithmList) {
        for (AllocationAlgorithm algorithm : algorithmList) {
            register(algorithm);
        }
        log.info("分摊算法注册完成，共注册 {} 个算法", algorithms.size());
    }

    /**
     * 注册单个算法
     */
    public void register(AllocationAlgorithm algorithm) {
        String code = algorithm.getCode();
        if (algorithms.containsKey(code)) {
            log.warn("分摊算法已存在，将被覆盖: {}", code);
        }
        algorithms.put(code, algorithm);
        log.info("注册分摊算法: {} - {}", code, algorithm.getName());
    }

    /**
     * 注销算法
     */
    public void unregister(String code) {
        AllocationAlgorithm removed = algorithms.remove(code);
        if (removed != null) {
            log.info("注销分摊算法: {}", code);
        }
    }

    /**
     * 获取算法
     */
    public AllocationAlgorithm getAlgorithm(String code) {
        AllocationAlgorithm algorithm = algorithms.get(code);
        if (algorithm == null) {
            throw new IllegalArgumentException("分摊算法不存在: " + code);
        }
        return algorithm;
    }

    /**
     * 获取所有算法
     */
    public List<AllocationAlgorithm> getAllAlgorithms() {
        return new ArrayList<>(algorithms.values());
    }

    /**
     * 检查算法是否存在
     */
    public boolean hasAlgorithm(String code) {
        return algorithms.containsKey(code);
    }

    /**
     * 获取算法数量
     */
    public int getAlgorithmCount() {
        return algorithms.size();
    }

    /**
     * 校验算法参数
     */
    public AllocationAlgorithm.ValidationResult validateAlgorithmParams(String code, Map<String, Object> params) {
        AllocationAlgorithm algorithm = getAlgorithm(code);
        if (algorithm == null) {
            return AllocationAlgorithm.ValidationResult.failure(List.of("算法不存在: " + code));
        }
        return algorithm.validateParams(params);
    }

    /**
     * 执行算法
     */
    public List<AllocationAlgorithm.AllocationResult> executeAlgorithm(String code, AllocationAlgorithm.AllocationContext context) {
        AllocationAlgorithm algorithm = getAlgorithm(code);
        if (algorithm == null) {
            throw new IllegalArgumentException("算法不存在: " + code);
        }

        // 校验参数
        if (context.getAlgorithmParams() != null) {
            AllocationAlgorithm.ValidationResult validationResult = algorithm.validateParams(context.getAlgorithmParams());
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException("算法参数校验失败: " + String.join(", ", validationResult.getErrors()));
            }
        }

        return algorithm.execute(context);
    }
}
