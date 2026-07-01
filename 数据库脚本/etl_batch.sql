-- 数据库脚本/etl_batch.sql
-- 批量ETL执行
-- 创建时间：2026-06-30

DELIMITER //

DROP PROCEDURE IF EXISTS sp_etl_batch//

CREATE PROCEDURE sp_etl_batch()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_period VARCHAR(10);
    DECLARE cur CURSOR FOR SELECT DISTINCT account_period FROM loan_indicator_detail ORDER BY account_period;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_period;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- 调用单期ETL
        CALL sp_etl_recalculate(v_period);
        SELECT CONCAT('ETL完成: ', v_period) as status;
    END LOOP;

    CLOSE cur;
END//

DELIMITER ;
