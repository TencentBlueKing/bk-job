SET NAMES utf8mb4;
USE job_analysis;

alter table statistics modify `value` mediumtext not null comment '统计值';