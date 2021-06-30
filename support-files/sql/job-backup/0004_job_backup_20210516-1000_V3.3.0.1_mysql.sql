USE `job_backup`;

SET NAMES utf8mb4;

CREATE TABLE `archive_progress` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `table_name` varchar(256) NOT NULL,
  `last_archived_id` BIGINT(20),
  `last_deleted_id` BIGINT(20),
  `last_archive_time` BIGINT(20) NOT NULL DEFAULT '0',
  `last_delete_time` BIGINT(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`table_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

