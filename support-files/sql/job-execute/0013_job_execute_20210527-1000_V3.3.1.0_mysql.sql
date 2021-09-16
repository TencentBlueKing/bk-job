USE job_execute;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `dangerous_record` (
  `row_create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `row_update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `id`              bigint(20) NOT NULL AUTO_INCREMENT,
  `rule_id`         bigint(20) NOT NULL,
  `rule_expression` varchar(255) NOT NULL,
  `app_id`          bigint(20) NOT NULL,
  `app_name`        varchar(1024) NOT NULL,
  `operator`        varchar(128) NOT NULL,
  `script_language` tinyint(4) NOT NULL,
  `script_content`  longtext /*!99104 COMPRESSED */ NOT NULL, 
  `create_time`     bigint(20) NOT NULL,
  `startup_mode`    tinyint(4) NOT NULL,
  `client`          varchar(128) NOT NULL,  
  `action`            tinyint(4) NOT NULL,
  `check_result`    text NOT NULL,
  `ext_data` text,
  PRIMARY KEY (`id`),
  KEY `idx_create_time_rule_id` (`create_time`,`rule_id`),
  KEY `idx_create_time_rule_expression` (`create_time`,`rule_expression`),
  KEY `idx_create_time_app_id` (`create_time`,`app_id`),
  KEY `idx_create_time_operator` (`create_time`,`operator`),
  KEY `idx_create_time_startup_mode` (`create_time`,`startup_mode`),
  KEY `idx_create_time_client` (`create_time`,`client`),
  KEY `idx_create_time_mode` (`create_time`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

