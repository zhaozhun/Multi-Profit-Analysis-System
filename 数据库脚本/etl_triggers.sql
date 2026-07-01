-- 数据库脚本/etl_triggers.sql
-- ETL触发器（需要SUPER权限，当前环境无法创建）
-- 改为应用层触发方案：在Service层监听数据变更，自动调用ETL
-- 创建时间：2026-06-30

-- 以下SQL需要SUPER权限才能执行，仅供参考
-- 实际方案改为在后端Service层实现自动触发

/*
DELIMITER //

-- 删除已有触发器
DROP TRIGGER IF EXISTS trg_loan_after_insert//
DROP TRIGGER IF EXISTS trg_loan_after_update//
DROP TRIGGER IF EXISTS trg_loan_after_delete//
DROP TRIGGER IF EXISTS trg_deposit_after_insert//
DROP TRIGGER IF EXISTS trg_deposit_after_update//
DROP TRIGGER IF EXISTS trg_deposit_after_delete//

-- 贷款数据插入触发器
CREATE TRIGGER trg_loan_after_insert
AFTER INSERT ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 贷款数据更新触发器
CREATE TRIGGER trg_loan_after_update
AFTER UPDATE ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 贷款数据删除触发器
CREATE TRIGGER trg_loan_after_delete
AFTER DELETE ON loan_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(OLD.account_period);
END//

-- 存款数据插入触发器
CREATE TRIGGER trg_deposit_after_insert
AFTER INSERT ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 存款数据更新触发器
CREATE TRIGGER trg_deposit_after_update
AFTER UPDATE ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(NEW.account_period);
END//

-- 存款数据删除触发器
CREATE TRIGGER trg_deposit_after_delete
AFTER DELETE ON deposit_indicator_detail
FOR EACH ROW
BEGIN
    CALL sp_etl_recalculate(OLD.account_period);
END//

DELIMITER ;
*/
