-- 数据库脚本/drop_obsolete_tables.sql
-- 删除废弃表(8张)

-- 废弃指标表(7张)
DROP TABLE IF EXISTS indicator_definition;
DROP TABLE IF EXISTS atomic_indicator;
DROP TABLE IF EXISTS derived_indicator;
DROP TABLE IF EXISTS indicator_precomputed;
DROP TABLE IF EXISTS indicator_pre_calc;
DROP TABLE IF EXISTS indicator_summary;
DROP TABLE IF EXISTS indicator_stat_config;

-- 业务台账(1张)
DROP TABLE IF EXISTS biz_ledger;
