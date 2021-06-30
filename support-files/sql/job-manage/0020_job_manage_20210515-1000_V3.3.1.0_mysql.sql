use job_manage;

SET NAMES utf8mb4;

alter table `host` modify `display_ip` text not null comment '展示IP';
