use job_manage;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_custom_script_template` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `username` varchar(128) NOT NULL,
  `script_language` tinyint(5) NOT NULL,
  `script_content` longtext NOT NULL,
  PRIMARY KEY (`username`,`script_language`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

