package com.multiprofit.controller;

import com.multiprofit.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/master")
public class MasterDataController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询主数据列表
     */
    @GetMapping("/{dimType}")
    public ApiResponse<Map<String, Object>> getList(
            @PathVariable String dimType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int pageSize) {

        StringBuilder sql = new StringBuilder(
            "SELECT id, code, name, parent_id, level, sort_order, status, ext_attrs, create_time " +
            "FROM dimension_master WHERE dim_type = '" + dimType + "'"
        );

        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (name LIKE '%").append(keyword).append("%' OR code LIKE '%").append(keyword).append("%')");
        }

        sql.append(" ORDER BY level, sort_order");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());

        // 构建树形结构
        List<Map<String, Object>> tree = buildTree(rows, 0L);

        Map<String, Object> result = new HashMap<>();
        result.put("total", rows.size());
        result.put("tree", tree);
        result.put("flat", rows);

        return ApiResponse.ok(result);
    }

    /**
     * 获取树形结构
     */
    @GetMapping("/{dimType}/tree")
    public ApiResponse<List<Map<String, Object>>> getTree(@PathVariable String dimType) {
        String sql = String.format(
            "SELECT id, code, name, parent_id, level, sort_order, status FROM dimension_master " +
            "WHERE dim_type = '%s' ORDER BY level, sort_order", dimType
        );
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        return ApiResponse.ok(buildTree(rows, 0L));
    }

    /**
     * 新增主数据
     */
    @PostMapping("/{dimType}")
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String dimType,
            @RequestBody Map<String, Object> body) {

        String code = (String) body.get("code");
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : 0L;
        int level = body.get("level") != null ? Integer.parseInt(body.get("level").toString()) : 1;
        int sortOrder = body.get("sortOrder") != null ? Integer.parseInt(body.get("sortOrder").toString()) : 0;
        String extAttrs = (String) body.get("extAttrs");

        // 检查编码唯一性
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dimension_master WHERE code = ? AND dim_type = ?",
            Integer.class, code, dimType
        );
        if (count != null && count > 0) {
            return ApiResponse.error("编码已存在");
        }

        jdbcTemplate.update(
            "INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, status, ext_attrs) " +
            "VALUES (?, ?, ?, ?, ?, ?, 1, ?)",
            code, name, dimType, parentId, level, sortOrder, extAttrs
        );

        // 获取新增的ID
        Long id = jdbcTemplate.queryForObject("SELECT SCOPE_IDENTITY()", Long.class);

        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("code", code);
        result.put("name", name);
        return ApiResponse.ok(result);
    }

    /**
     * 修改主数据
     */
    @PutMapping("/{dimType}/{id}")
    public ApiResponse<String> update(
            @PathVariable String dimType,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {

        String name = (String) body.get("name");
        String code = (String) body.get("code");
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        String extAttrs = (String) body.get("extAttrs");

        StringBuilder sql = new StringBuilder("UPDATE dimension_master SET ");
        List<Object> params = new ArrayList<>();
        List<String> sets = new ArrayList<>();

        if (name != null) { sets.add("name = ?"); params.add(name); }
        if (code != null) { sets.add("code = ?"); params.add(code); }
        if (status != null) { sets.add("status = ?"); params.add(status); }
        if (extAttrs != null) { sets.add("ext_attrs = ?"); params.add(extAttrs); }

        if (sets.isEmpty()) {
            return ApiResponse.error("没有要更新的字段");
        }

        sql.append(String.join(", ", sets));
        sql.append(" WHERE id = ? AND dim_type = ?");
        params.add(id);
        params.add(dimType);

        jdbcTemplate.update(sql.toString(), params.toArray());
        return ApiResponse.ok("更新成功");
    }

    /**
     * 删除主数据
     */
    @DeleteMapping("/{dimType}/{id}")
    public ApiResponse<String> delete(@PathVariable String dimType, @PathVariable Long id) {
        // 检查是否有子节点
        Integer childCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM dimension_master WHERE parent_id = ? AND dim_type = ?",
            Integer.class, id, dimType
        );
        if (childCount != null && childCount > 0) {
            return ApiResponse.error("该节点下有子节点，无法删除");
        }

        jdbcTemplate.update("DELETE FROM dimension_master WHERE id = ? AND dim_type = ?", id, dimType);
        return ApiResponse.ok("删除成功");
    }

    /**
     * 批量启用/停用
     */
    @PostMapping("/{dimType}/batch-status")
    public ApiResponse<String> batchStatus(
            @PathVariable String dimType,
            @RequestBody Map<String, Object> body) {

        List<Integer> ids = (List<Integer>) body.get("ids");
        int status = (int) body.get("status");

        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error("请选择要操作的数据");
        }

        String idStr = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        jdbcTemplate.update(
            String.format("UPDATE dimension_master SET status = %d WHERE id IN (%s) AND dim_type = '%s'",
                status, idStr, dimType)
        );

        return ApiResponse.ok(status == 1 ? "批量启用成功" : "批量停用成功");
    }

    /**
     * 构建树形结构
     */
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
