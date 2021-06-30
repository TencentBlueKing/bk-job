SET NAMES utf8mb4;
USE job_analysis;

-- ------------------------------------------------
-- 已去除保险模式，删除保险模式相关统计数据
-- ------------------------------------------------
delimiter $$
drop procedure if exists proc_delete_analysis_fileTransferMode_data;
create procedure proc_delete_analysis_fileTransferMode_data()
begin
lp : loop
    delete from `statistics` where resource='executedFastFile' and dimension='fileTransferMode' and dimension_value='SAFE' limit 10000;
    if row_count() < 10000 then
        leave lp;
    end if;
    select sleep(1);
end loop;
end $$

delimiter ;
call proc_delete_analysis_fileTransferMode_data;