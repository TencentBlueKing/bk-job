USE job_manage;
ALTER TABLE host_topo ADD COLUMN app_id BIGINT(20) UNSIGNED;
ALTER TABLE host_topo ADD INDEX idx_app_id(app_id);