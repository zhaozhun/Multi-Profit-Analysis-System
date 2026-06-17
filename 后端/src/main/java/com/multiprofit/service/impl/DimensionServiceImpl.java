package com.multiprofit.service.impl;

import com.multiprofit.dto.DashboardDTO;
import com.multiprofit.dto.DimensionAnalysisDTO;
import com.multiprofit.service.DimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DimensionServiceImpl implements DimensionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public DimensionAnalysisDTO getAnalysisData(String dimType, String startDate, String endDate,
                                                  String caliberType, Long parentId, Integer level) {
        DimensionAnalysisDTO dto = new DimensionAnalysisDTO();
        dto.setDimType(dimType);
        dto.setDimLabel(getDimLabel(dimType));
        dto.setCurrentLevel(level != null ? level : 1);

        // KPI汇总（使用JOIN查询）
        String sumSql = """
            SELECT
              sum(b.revenue) as revenue,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost,
              sum(b.net_profit) as net_profit,
              sum(b.loan_revenue) as loan_revenue,
              sum(b.loan_ftp_cost) as loan_ftp_cost,
              sum(b.loan_risk_cost) as loan_risk_cost,
              sum(b.loan_op_cost) as loan_op_cost,
              sum(b.loan_profit) as loan_profit,
              sum(b.deposit_revenue) as deposit_revenue,
              sum(b.deposit_interest) as deposit_interest,
              sum(b.deposit_op_cost) as deposit_op_cost,
              sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            """;
        Map<String, Object> sumData = jdbcTemplate.queryForMap(sumSql, startDate, endDate, caliberType);
        dto.setKpiCards(buildKpiCards(sumData));

        // 瀑布图
        dto.setWaterfall(buildWaterfall(sumData));

        // 排名
        dto.setRanking(getRanking(dimType, startDate, endDate, caliberType, "net_profit", 10));

        // 成本结构
        dto.setCostStructure(buildCostStructure(sumData));

        // 树形数据
        dto.setTreeData(getTreeData(dimType, startDate, endDate, caliberType, parentId));

        // 钻取路径
        if (parentId != null) {
            dto.setDrillPath(getDrillPath(dimType, parentId));
        }

        return dto;
    }

    @Override
    public List<DimensionAnalysisDTO.TreeNode> getTreeData(String dimType, String startDate, String endDate,
                                                            String caliberType, Long parentId) {
        String idCol = getDimIdColumn(dimType);

        // 使用递归CTE获取叶子节点数据，然后聚合到父节点
        // 1. 先获取所有叶子节点的盈利数据
        String leafProfitSql = String.format("""
            SELECT
              b.%s as dim_id,
              sum(b.revenue) as revenue,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost,
              sum(b.net_profit) as net_profit,
              sum(b.loan_revenue) as loan_revenue,
              sum(b.loan_ftp_cost) as loan_ftp_cost,
              sum(b.loan_risk_cost) as loan_risk_cost,
              sum(b.loan_op_cost) as loan_op_cost,
              sum(b.loan_profit) as loan_profit,
              sum(b.deposit_revenue) as deposit_revenue,
              sum(b.deposit_interest) as deposit_interest,
              sum(b.deposit_op_cost) as deposit_op_cost,
              sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            GROUP BY b.%s
            """, idCol, idCol);
        List<Map<String, Object>> leafRows = jdbcTemplate.queryForList(leafProfitSql, startDate, endDate, caliberType);

        // 构建叶子节点盈利数据映射
        Map<Long, Map<String, Object>> leafProfitMap = new HashMap<>();
        for (Map<String, Object> row : leafRows) {
            Long dimId = ((Number) row.get("dim_id")).longValue();
            leafProfitMap.put(dimId, row);
        }

        // 2. 获取维度层级结构
        String hierarchySql = String.format("""
            SELECT id, code, name, parent_id, level, sort_order
            FROM dimension_master
            WHERE dim_type = '%s'
            ORDER BY level, sort_order
            """, dimType);
        List<Map<String, Object>> allNodes = jdbcTemplate.queryForList(hierarchySql);

        // 3. 构建节点映射和子节点映射
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();

        for (Map<String, Object> node : allNodes) {
            Long id = ((Number) node.get("id")).longValue();
            Long pId = ((Number) node.get("parent_id")).longValue();
            nodeMap.put(id, node);

            if (!childrenMap.containsKey(pId)) {
                childrenMap.put(pId, new ArrayList<>());
            }
            childrenMap.get(pId).add(node);
        }

        // 4. 递归聚合盈利数据（从根节点向下聚合）
        Map<Long, Map<String, BigDecimal>> aggregatedProfit = new HashMap<>();
        // 从所有根节点（parent_id=0）开始递归
        List<Map<String, Object>> rootNodes = childrenMap.getOrDefault(0L, Collections.emptyList());
        for (Map<String, Object> rootNode : rootNodes) {
            Long rootId = ((Number) rootNode.get("id")).longValue();
            aggregateProfitData(rootId, childrenMap, leafProfitMap, aggregatedProfit);
        }

        // 5. 获取要显示的节点
        List<Map<String, Object>> targetNodes;
        if (parentId == null || parentId == 0) {
            targetNodes = childrenMap.getOrDefault(0L, Collections.emptyList());
        } else {
            targetNodes = childrenMap.getOrDefault(parentId, Collections.emptyList());
        }

        // 6. 构建树节点
        List<DimensionAnalysisDTO.TreeNode> result = new ArrayList<>();
        for (Map<String, Object> node : targetNodes) {
            Long nodeId = ((Number) node.get("id")).longValue();
            int childCount = childrenMap.getOrDefault(nodeId, Collections.emptyList()).size();

            DimensionAnalysisDTO.TreeNode treeNode = new DimensionAnalysisDTO.TreeNode();
            treeNode.setId(nodeId);
            treeNode.setCode(node.get("code") != null ? String.valueOf(node.get("code")) : "node_" + nodeId);
            treeNode.setName(String.valueOf(node.get("name")));
            treeNode.setChildCount(childCount);
            treeNode.setLeaf(childCount == 0);
            treeNode.setLevel(((Number) node.get("level")).intValue());

            // 设置盈利数据
            Map<String, BigDecimal> profit = aggregatedProfit.get(nodeId);
            if (profit != null) {
                treeNode.setRevenue(profit.getOrDefault("revenue", BigDecimal.ZERO));
                treeNode.setFtpCost(profit.getOrDefault("ftp_cost", BigDecimal.ZERO));
                treeNode.setRiskCost(profit.getOrDefault("risk_cost", BigDecimal.ZERO));
                treeNode.setOpCost(profit.getOrDefault("op_cost", BigDecimal.ZERO));
                treeNode.setNetProfit(profit.getOrDefault("net_profit", BigDecimal.ZERO));
                treeNode.setLoanRevenue(profit.getOrDefault("loan_revenue", BigDecimal.ZERO));
                treeNode.setLoanFtpCost(profit.getOrDefault("loan_ftp_cost", BigDecimal.ZERO));
                treeNode.setLoanRiskCost(profit.getOrDefault("loan_risk_cost", BigDecimal.ZERO));
                treeNode.setLoanOpCost(profit.getOrDefault("loan_op_cost", BigDecimal.ZERO));
                treeNode.setLoanProfit(profit.getOrDefault("loan_profit", BigDecimal.ZERO));
                treeNode.setDepositRevenue(profit.getOrDefault("deposit_revenue", BigDecimal.ZERO));
                treeNode.setDepositInterest(profit.getOrDefault("deposit_interest", BigDecimal.ZERO));
                treeNode.setDepositOpCost(profit.getOrDefault("deposit_op_cost", BigDecimal.ZERO));
                treeNode.setDepositProfit(profit.getOrDefault("deposit_profit", BigDecimal.ZERO));
            } else {
                treeNode.setRevenue(BigDecimal.ZERO);
                treeNode.setFtpCost(BigDecimal.ZERO);
                treeNode.setRiskCost(BigDecimal.ZERO);
                treeNode.setOpCost(BigDecimal.ZERO);
                treeNode.setNetProfit(BigDecimal.ZERO);
            }

            // 计算成本收入比
            BigDecimal rev = treeNode.getRevenue();
            BigDecimal totalCost = treeNode.getFtpCost().add(treeNode.getRiskCost()).add(treeNode.getOpCost());
            if (rev.compareTo(BigDecimal.ZERO) != 0) {
                treeNode.setCostIncomeRatio(totalCost.multiply(new BigDecimal("100"))
                    .divide(rev, 2, BigDecimal.ROUND_HALF_UP));
            }

            treeNode.setProfitStatus(treeNode.getNetProfit().compareTo(BigDecimal.ZERO) >= 0 ? "PROFIT" : "LOSS");

            result.add(treeNode);
        }

        return result;
    }

    /**
     * 递归聚合盈利数据（从叶子节点向上）
     */
    private Map<String, BigDecimal> aggregateProfitData(Long nodeId,
                                                         Map<Long, List<Map<String, Object>>> childrenMap,
                                                         Map<Long, Map<String, Object>> leafProfitMap,
                                                         Map<Long, Map<String, BigDecimal>> aggregatedProfit) {
        Map<String, BigDecimal> profit = new HashMap<>();
        profit.put("revenue", BigDecimal.ZERO);
        profit.put("ftp_cost", BigDecimal.ZERO);
        profit.put("risk_cost", BigDecimal.ZERO);
        profit.put("op_cost", BigDecimal.ZERO);
        profit.put("net_profit", BigDecimal.ZERO);
        profit.put("loan_revenue", BigDecimal.ZERO);
        profit.put("loan_ftp_cost", BigDecimal.ZERO);
        profit.put("loan_risk_cost", BigDecimal.ZERO);
        profit.put("loan_op_cost", BigDecimal.ZERO);
        profit.put("loan_profit", BigDecimal.ZERO);
        profit.put("deposit_revenue", BigDecimal.ZERO);
        profit.put("deposit_interest", BigDecimal.ZERO);
        profit.put("deposit_op_cost", BigDecimal.ZERO);
        profit.put("deposit_profit", BigDecimal.ZERO);

        List<Map<String, Object>> children = childrenMap.getOrDefault(nodeId, Collections.emptyList());

        // 如果节点在 leafProfitMap 中有数据（说明 biz_ledger 中有该节点的数据），直接使用
        Map<String, Object> leafData = leafProfitMap.get(nodeId);
        if (leafData != null) {
            profit.put("revenue", toBD(leafData.get("revenue")));
            profit.put("ftp_cost", toBD(leafData.get("ftp_cost")));
            profit.put("risk_cost", toBD(leafData.get("risk_cost")));
            profit.put("op_cost", toBD(leafData.get("op_cost")));
            profit.put("net_profit", toBD(leafData.get("net_profit")));
            profit.put("loan_revenue", toBD(leafData.get("loan_revenue")));
            profit.put("loan_ftp_cost", toBD(leafData.get("loan_ftp_cost")));
            profit.put("loan_risk_cost", toBD(leafData.get("loan_risk_cost")));
            profit.put("loan_op_cost", toBD(leafData.get("loan_op_cost")));
            profit.put("loan_profit", toBD(leafData.get("loan_profit")));
            profit.put("deposit_revenue", toBD(leafData.get("deposit_revenue")));
            profit.put("deposit_interest", toBD(leafData.get("deposit_interest")));
            profit.put("deposit_op_cost", toBD(leafData.get("deposit_op_cost")));
            profit.put("deposit_profit", toBD(leafData.get("deposit_profit")));
        } else if (!children.isEmpty()) {
            // 父节点且没有直接数据，递归聚合子节点数据
            for (Map<String, Object> child : children) {
                Long childId = ((Number) child.get("id")).longValue();
                Map<String, BigDecimal> childProfit = aggregateProfitData(childId, childrenMap, leafProfitMap, aggregatedProfit);

                for (String key : profit.keySet()) {
                    profit.put(key, profit.get(key).add(childProfit.getOrDefault(key, BigDecimal.ZERO)));
                }
            }
        }

        aggregatedProfit.put(nodeId, profit);
        return profit;
    }

    @Override
    public List<DimensionAnalysisDTO.RankItem> getRanking(String dimType, String startDate, String endDate,
                                                           String caliberType, String rankBy, int limit) {
        String idCol = getDimIdColumn(dimType);
        String orderBy = "net_profit".equals(rankBy) ? "net_profit" : "revenue";

        String sql = String.format("""
            SELECT dm.name, sum(b.revenue) as revenue, sum(b.net_profit) as net_profit,
              sum(b.loan_profit) as loan_profit, sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            JOIN dimension_master dm ON b.%s = dm.id
            WHERE b.stat_date >= ? AND b.stat_date <= ? AND b.caliber_type = ?
            GROUP BY dm.name
            ORDER BY %s DESC
            LIMIT %d
            """, idCol, orderBy, limit);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, startDate, endDate, caliberType);
        List<DimensionAnalysisDTO.RankItem> items = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            DimensionAnalysisDTO.RankItem item = new DimensionAnalysisDTO.RankItem();
            item.setName(String.valueOf(row.get("name")));
            item.setNetProfit(toBD(row.get("net_profit")));
            item.setLoanProfit(toBD(row.get("loan_profit")));
            item.setDepositProfit(toBD(row.get("deposit_profit")));
            item.setRevenue(toBD(row.get("revenue")));
            item.setRankIndex(rank++);
            items.add(item);
        }
        return items;
    }

    @Override
    public DimensionAnalysisDTO.TableRow getDetail(Long dimId, String dimType, String startDate, String endDate) {
        Map<String, Object> master = jdbcTemplate.queryForMap(
            "SELECT name FROM dimension_master WHERE id = ?", dimId
        );
        String name = String.valueOf(master.get("name"));
        String idCol = getDimIdColumn(dimType);

        String sql = String.format("""
            SELECT sum(b.revenue) as revenue, sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost, sum(b.op_cost) as op_cost, sum(b.net_profit) as net_profit,
              sum(b.loan_profit) as loan_profit, sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            WHERE b.%s = ? AND b.stat_date >= ? AND b.stat_date <= ?
            """, idCol);
        Map<String, Object> data = jdbcTemplate.queryForMap(sql, dimId, startDate, endDate);

        DimensionAnalysisDTO.TableRow row = new DimensionAnalysisDTO.TableRow();
        row.setId(dimId);
        row.setName(name);
        row.setRevenue(toBD(data.get("revenue")));
        row.setFtpCost(toBD(data.get("ftp_cost")));
        row.setRiskCost(toBD(data.get("risk_cost")));
        row.setOpCost(toBD(data.get("op_cost")));
        row.setNetProfit(toBD(data.get("net_profit")));
        row.setLoanProfit(toBD(data.get("loan_profit")));
        row.setDepositProfit(toBD(data.get("deposit_profit")));
        return row;
    }

    @Override
    public List<DimensionAnalysisDTO.TableRow> crossDrill(String fromDimType, String fromDimName,
                                                           String toDimType, String startDate, String endDate,
                                                           String caliberType) {
        String fromIdCol = getDimIdColumn(fromDimType);
        String toIdCol = getDimIdColumn(toDimType);

        String sql = String.format("""
            SELECT
              target_dm.name as name,
              sum(b.revenue) as revenue,
              sum(b.ftp_cost) as ftp_cost,
              sum(b.risk_cost) as risk_cost,
              sum(b.op_cost) as op_cost,
              sum(b.net_profit) as net_profit,
              sum(b.loan_profit) as loan_profit,
              sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            JOIN dimension_master source_dm ON b.%s = source_dm.id
            JOIN dimension_master target_dm ON b.%s = target_dm.id
            WHERE source_dm.name = ?
              AND b.stat_date >= ? AND b.stat_date <= ?
              AND b.caliber_type = ?
            GROUP BY target_dm.name
            ORDER BY net_profit DESC
            """, fromIdCol, toIdCol);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, fromDimName, startDate, endDate, caliberType);
        List<DimensionAnalysisDTO.TableRow> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            DimensionAnalysisDTO.TableRow tableRow = new DimensionAnalysisDTO.TableRow();
            tableRow.setName(String.valueOf(row.get("name")));
            tableRow.setParentName(fromDimName);
            tableRow.setRevenue(toBD(row.get("revenue")));
            tableRow.setFtpCost(toBD(row.get("ftp_cost")));
            tableRow.setRiskCost(toBD(row.get("risk_cost")));
            tableRow.setOpCost(toBD(row.get("op_cost")));
            tableRow.setNetProfit(toBD(row.get("net_profit")));
            tableRow.setLoanProfit(toBD(row.get("loan_profit")));
            tableRow.setDepositProfit(toBD(row.get("deposit_profit")));

            BigDecimal rev = tableRow.getRevenue();
            BigDecimal totalCost = tableRow.getFtpCost().add(tableRow.getRiskCost()).add(tableRow.getOpCost());
            if (rev.compareTo(BigDecimal.ZERO) != 0) {
                tableRow.setCostIncomeRatio(totalCost.multiply(new BigDecimal("100"))
                    .divide(rev, 2, BigDecimal.ROUND_HALF_UP));
            }
            tableRow.setProfitStatus(tableRow.getNetProfit().compareTo(BigDecimal.ZERO) >= 0 ? "PROFIT" : "LOSS");
            result.add(tableRow);
        }

        return result;
    }

    /**
     * 交叉钻取 - 返回树状结构
     */
    public List<Map<String, Object>> crossDrillTree(String fromDimType, Long fromDimId,
                                                     String toDimType, String startDate, String endDate,
                                                     String caliberType) {
        String fromIdCol = getDimIdColumn(fromDimType);
        String toIdCol = getDimIdColumn(toDimType);

        // 1. 获取目标维度的完整层级
        String hierarchySql = String.format("""
            SELECT id, code, name, parent_id, level
            FROM dimension_master
            WHERE dim_type = '%s'
            ORDER BY level, sort_order
            """, toDimType);
        List<Map<String, Object>> allNodes = jdbcTemplate.queryForList(hierarchySql);

        // 2. 获取盈利数据（按目标维度聚合）
        String profitSql = String.format("""
            SELECT
              b.%s as dim_id,
              sum(b.revenue) as revenue,
              sum(b.net_profit) as net_profit,
              sum(b.loan_profit) as loan_profit,
              sum(b.deposit_profit) as deposit_profit
            FROM biz_ledger b
            WHERE b.%s = ?
              AND b.stat_date >= ? AND b.stat_date <= ?
              AND b.caliber_type = ?
            GROUP BY b.%s
            """, toIdCol, fromIdCol, toIdCol);
        List<Map<String, Object>> profitRows = jdbcTemplate.queryForList(profitSql, fromDimId, startDate, endDate, caliberType);

        // 构建 profit map
        Map<Long, Map<String, Object>> profitMap = new HashMap<>();
        for (Map<String, Object> row : profitRows) {
            Long dimId = ((Number) row.get("dim_id")).longValue();
            profitMap.put(dimId, row);
        }

        // 3. 构建树
        Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();

        for (Map<String, Object> node : allNodes) {
            Long id = ((Number) node.get("id")).longValue();
            Long parentId = ((Number) node.get("parent_id")).longValue();
            nodeMap.put(id, node);

            if (!childrenMap.containsKey(parentId)) {
                childrenMap.put(parentId, new ArrayList<>());
            }
            childrenMap.get(parentId).add(node);
        }

        // 4. 递归构建树，带上盈利数据
        List<Map<String, Object>> roots = new ArrayList<>();
        for (Map<String, Object> node : allNodes) {
            Long parentId = ((Number) node.get("parent_id")).longValue();
            if (parentId == 0) {
                roots.add(buildTreeNode(node, childrenMap, profitMap));
            }
        }

        return roots;
    }

    private Map<String, Object> buildTreeNode(Map<String, Object> node,
                                                Map<Long, List<Map<String, Object>>> childrenMap,
                                                Map<Long, Map<String, Object>> profitMap) {
        Long id = ((Number) node.get("id")).longValue();
        Map<String, Object> treeNode = new HashMap<>();
        treeNode.put("id", id);
        treeNode.put("code", node.get("code"));
        treeNode.put("name", node.get("name"));
        treeNode.put("level", node.get("level"));

        // 盈利数据
        Map<String, Object> profit = profitMap.get(id);
        treeNode.put("revenue", profit != null ? toBD(profit.get("revenue")) : BigDecimal.ZERO);
        treeNode.put("netProfit", profit != null ? toBD(profit.get("net_profit")) : BigDecimal.ZERO);
        treeNode.put("loanProfit", profit != null ? toBD(profit.get("loan_profit")) : BigDecimal.ZERO);
        treeNode.put("depositProfit", profit != null ? toBD(profit.get("deposit_profit")) : BigDecimal.ZERO);

        // 子节点
        List<Map<String, Object>> children = childrenMap.getOrDefault(id, Collections.emptyList());
        List<Map<String, Object>> childTreeNodes = new ArrayList<>();
        for (Map<String, Object> child : children) {
            childTreeNodes.add(buildTreeNode(child, childrenMap, profitMap));
        }
        treeNode.put("children", childTreeNodes);
        treeNode.put("childCount", children.size());
        treeNode.put("isLeaf", children.isEmpty());

        return treeNode;
    }

    @Override
    public List<Map<String, Object>> getDrillPath(String dimType, Long currentId) {
        List<Map<String, Object>> path = new ArrayList<>();
        Long id = currentId;

        while (id != null && id > 0) {
            Map<String, Object> master;
            try {
                master = jdbcTemplate.queryForMap(
                    "SELECT id, name, parent_id, level FROM dimension_master WHERE id = ?", id
                );
            } catch (Exception e) {
                break;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", master.get("id"));
            item.put("name", master.get("name"));
            item.put("level", master.get("level"));
            item.put("dimType", dimType);
            path.add(0, item);

            Object parentId = master.get("parent_id");
            id = parentId != null ? ((Number) parentId).longValue() : null;
        }

        return path;
    }

    @Override
    public List<Map<String, Object>> getDimHierarchy(String dimType) {
        String sql = String.format(
            "SELECT id, code, name, parent_id, level FROM dimension_master " +
            "WHERE dim_type = '%s' ORDER BY level, sort_order", dimType
        );
        return jdbcTemplate.queryForList(sql);
    }

    // === Private Helpers ===

    private List<DimensionAnalysisDTO.TreeNode> buildTreeNodes(List<Map<String, Object>> rows) {
        List<DimensionAnalysisDTO.TreeNode> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            DimensionAnalysisDTO.TreeNode node = new DimensionAnalysisDTO.TreeNode();
            node.setId(((Number) row.get("id")).longValue());
            node.setKey(row.get("code") != null ? String.valueOf(row.get("code")) : "node_" + node.getId());
            node.setName(String.valueOf(row.get("name")));
            node.setCode(row.get("code") != null ? String.valueOf(row.get("code")) : "");
            node.setLevel(((Number) row.get("level")).intValue());

            int childCount = row.get("child_count") != null ? ((Number) row.get("child_count")).intValue() : 0;
            node.setChildCount(childCount);
            node.setLeaf(childCount == 0);

            // 盈利数据
            node.setRevenue(toBD(row.get("revenue")));
            node.setFtpCost(toBD(row.get("ftp_cost")));
            node.setRiskCost(toBD(row.get("risk_cost")));
            node.setOpCost(toBD(row.get("op_cost")));
            node.setNetProfit(toBD(row.get("net_profit")));

            // 贷款数据
            node.setLoanRevenue(toBD(row.get("loan_revenue")));
            node.setLoanFtpCost(toBD(row.get("loan_ftp_cost")));
            node.setLoanRiskCost(toBD(row.get("loan_risk_cost")));
            node.setLoanOpCost(toBD(row.get("loan_op_cost")));
            node.setLoanProfit(toBD(row.get("loan_profit")));

            // 存款数据
            node.setDepositRevenue(toBD(row.get("deposit_revenue")));
            node.setDepositInterest(toBD(row.get("deposit_interest")));
            node.setDepositOpCost(toBD(row.get("deposit_op_cost")));
            node.setDepositProfit(toBD(row.get("deposit_profit")));

            // 成本收入比
            BigDecimal rev = node.getRevenue();
            BigDecimal totalCost = node.getFtpCost().add(node.getRiskCost()).add(node.getOpCost());
            if (rev.compareTo(BigDecimal.ZERO) != 0) {
                node.setCostIncomeRatio(totalCost.multiply(new BigDecimal("100"))
                    .divide(rev, 2, BigDecimal.ROUND_HALF_UP));
            }

            node.setProfitStatus(node.getNetProfit().compareTo(BigDecimal.ZERO) >= 0 ? "PROFIT" : "LOSS");

            result.add(node);
        }
        return result;
    }

    private List<DashboardDTO.KpiCard> buildKpiCards(Map<String, Object> data) {
        List<DashboardDTO.KpiCard> cards = new ArrayList<>();
        cards.add(buildCard("总利润", toBD(data.get("net_profit")), "#52c41a"));
        cards.add(buildCard("贷款利润", toBD(data.get("loan_profit")), "#1890ff"));
        cards.add(buildCard("存款利润", toBD(data.get("deposit_profit")), "#722ed1"));
        cards.add(buildCard("贷款收入", toBD(data.get("loan_revenue")), "#36cfc9"));
        cards.add(buildCard("存款收入", toBD(data.get("deposit_revenue")), "#b37feb"));
        cards.add(buildCard("FTP成本", toBD(data.get("ftp_cost")), "#fa8c16"));
        cards.add(buildCard("风险成本", toBD(data.get("risk_cost")), "#f5222d"));
        cards.add(buildCard("运营成本", toBD(data.get("op_cost")), "#8c8c8c"));
        return cards;
    }

    private DashboardDTO.KpiCard buildCard(String name, BigDecimal value, String color) {
        DashboardDTO.KpiCard card = new DashboardDTO.KpiCard();
        card.setMetricName(name);
        card.setValue(value);
        card.setColor(color);
        card.setUnit("万元");
        return card;
    }

    private DashboardDTO.WaterfallData buildWaterfall(Map<String, Object> data) {
        DashboardDTO.WaterfallData waterfall = new DashboardDTO.WaterfallData();
        waterfall.setRevenue(toBD(data.get("revenue")));
        waterfall.setFtpCost(toBD(data.get("ftp_cost")));
        waterfall.setRiskCost(toBD(data.get("risk_cost")));
        waterfall.setOpCost(toBD(data.get("op_cost")));
        waterfall.setNetProfit(toBD(data.get("net_profit")));
        return waterfall;
    }

    private List<DashboardDTO.DimPieItem> buildCostStructure(Map<String, Object> data) {
        List<DashboardDTO.DimPieItem> items = new ArrayList<>();
        BigDecimal ftp = toBD(data.get("ftp_cost"));
        BigDecimal risk = toBD(data.get("risk_cost"));
        BigDecimal op = toBD(data.get("op_cost"));
        BigDecimal total = ftp.add(risk).add(op);

        items.add(buildPieItem("FTP成本", ftp, total));
        items.add(buildPieItem("风险成本", risk, total));
        items.add(buildPieItem("运营成本", op, total));
        return items;
    }

    private DashboardDTO.DimPieItem buildPieItem(String name, BigDecimal value, BigDecimal total) {
        DashboardDTO.DimPieItem item = new DashboardDTO.DimPieItem();
        item.setName(name);
        item.setValue(value);
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            item.setRatio(value.multiply(new BigDecimal("100")).divide(total, 2, BigDecimal.ROUND_HALF_UP));
        }
        return item;
    }

    /**
     * 维度ID列名映射（外键字段）
     */
    private String getDimIdColumn(String dimType) {
        return switch (dimType) {
            case "ORG" -> "org_id";
            case "BIZ_LINE" -> "biz_line_id";
            case "DEPT" -> "dept_id";
            case "PRODUCT" -> "product_id";
            case "CHANNEL" -> "channel_id";
            case "MANAGER" -> "manager_id";
            case "CUSTOMER" -> "customer_id";
            default -> "org_id";
        };
    }

    private String getDimLabel(String dimType) {
        return switch (dimType) {
            case "ORG" -> "机构";
            case "BIZ_LINE" -> "条线";
            case "DEPT" -> "部门";
            case "PRODUCT" -> "产品";
            case "CHANNEL" -> "渠道";
            case "MANAGER" -> "客户经理";
            case "CUSTOMER" -> "客户";
            default -> "维度";
        };
    }

    private BigDecimal toBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }
}
