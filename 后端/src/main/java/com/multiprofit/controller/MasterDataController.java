package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/master")
public class MasterDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** dimType → dim_* 表名映射 */
    private String getDimTable(String dimType) {
        switch (dimType) {
            case "ORG": return "dim_organization";
            case "BIZ_LINE": return "dim_biz_line";
            case "DEPT": return "dim_dept";
            case "PRODUCT": return "dim_product";
            case "CHANNEL": return "dim_channel";
            case "MANAGER": return "dim_manager";
            case "CUSTOMER": return "dim_customer_type";
            default: throw new IllegalArgumentException("Unknown dimType: " + dimType);
        }
    }

    @GetMapping("/{dimType}")
    public ApiResponse<Map<String, Object>> getList(
            @PathVariable String dimType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize) {

        String table = getDimTable(dimType);
        StringBuilder sql = new StringBuilder(
            "SELECT id, code, name, parent_id, level, sort_order, status, create_time FROM " + table + " WHERE 1=1");

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (name LIKE '%").append(keyword).append("%' OR code LIKE '%").append(keyword).append("%')");
        }
        sql.append(" ORDER BY level, sort_order");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());
        List<Map<String, Object>> tree = buildTree(rows, 0L);

        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("tree", tree);
        result.put("flat", rows);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{dimType}/tree")
    public ApiResponse<List<Map<String, Object>>> getTree(@PathVariable String dimType) {
        String table = getDimTable(dimType);
        String sql = "SELECT id, code, name, parent_id, level, sort_order, status FROM " + table + " ORDER BY level, sort_order";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return ApiResponse.ok(buildTree(rows, 0L));
    }

    @PostMapping("/{dimType}")
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String dimType,
            @RequestBody Map<String, Object> body) {

        String table = getDimTable(dimType);
        String code = (String) body.get("code");
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : 0L;
        int level = body.get("level") != null ? Integer.parseInt(body.get("level").toString()) : 1;
        int sortOrder = body.get("sortOrder") != null ? Integer.parseInt(body.get("sortOrder").toString()) : 0;

        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + table + " WHERE code = ?", Integer.class, code);
        if (count != null && count > 0) {
            return ApiResponse.error("编码已存在");
        }

        jdbcTemplate.update(
            "INSERT INTO " + table + " (code, name, parent_id, level, sort_order, status) VALUES (?, ?, ?, ?, ?, 1)",
            code, name, parentId, level, sortOrder);

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("code", code);
        result.put("name", name);
        return ApiResponse.ok(result);
    }

    @PutMapping("/{dimType}/{id}")
    public ApiResponse<String> update(
            @PathVariable String dimType, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String table = getDimTable(dimType);
        String name = (String) body.get("name");
        String code = (String) body.get("code");
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (name != null) { sets.add("name = ?"); params.add(name); }
        if (code != null) { sets.add("code = ?"); params.add(code); }
        if (status != null) { sets.add("status = ?"); params.add(status); }

        if (sets.isEmpty()) return ApiResponse.error("没有要更新的字段");

        String sql = "UPDATE " + table + " SET " + String.join(", ", sets) + " WHERE id = ?";
        params.add(id);
        jdbcTemplate.update(sql, params.toArray());
        return ApiResponse.ok("更新成功");
    }

    @DeleteMapping("/{dimType}/{id}")
    public ApiResponse<String> delete(@PathVariable String dimType, @PathVariable Long id) {
        String table = getDimTable(dimType);
        Integer childCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM " + table + " WHERE parent_id = ?", Integer.class, id);
        if (childCount != null && childCount > 0) {
            return ApiResponse.error("该节点下有子节点，无法删除");
        }
        jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
        return ApiResponse.ok("删除成功");
    }

    @PostMapping("/{dimType}/batch-status")
    public ApiResponse<String> batchStatus(
            @PathVariable String dimType, @RequestBody Map<String, Object> body) {

        String table = getDimTable(dimType);
        List<Integer> ids = (List<Integer>) body.get("ids");
        int status = (int) body.get("status");
        if (ids == null || ids.isEmpty()) return ApiResponse.error("请选择要操作的数据");

        String idStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        jdbcTemplate.update("UPDATE " + table + " SET status = ? WHERE id IN (" + idStr + ")", status);
        return ApiResponse.ok(status == 1 ? "批量启用成功" : "批量停用成功");
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> flatList, Long parentId) {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map<String, Object> item : flatList) {
            Long itemParentId = item.get("parent_id") != null ?
                ((Number) item.get("parent_id")).longValue() : 0L;
            if (itemParentId.equals(parentId)) {
                Map<String, Object> node = new HashMap<>(item);
                Long itemId = ((Number) item.get("id")).longValue();
                List<Map<String, Object>> children = buildTree(flatList, itemId);
                node.put("children", children);
                node.put("hasChildren", !children.isEmpty());
                tree.add(node);
            }
        }
        return tree;
    }
}
