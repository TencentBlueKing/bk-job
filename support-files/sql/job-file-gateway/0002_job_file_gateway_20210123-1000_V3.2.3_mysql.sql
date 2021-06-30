SET NAMES utf8mb4;
USE job_file_gateway;

alter table file_source add unique uniq_code(`code`);