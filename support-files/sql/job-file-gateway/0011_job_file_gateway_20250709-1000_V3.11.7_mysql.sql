SET NAMES utf8mb4;
USE job_file_gateway;

-- ----------------------------
-- Table structure for file_source_white_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_white_info`
(
    `id`               int(11)                            NOT NULL AUTO_INCREMENT,
    `type`             varchar(128) CHARACTER SET utf8mb4 NOT NULL COMMENT '白名单数据类型',
    `content`          varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '白名单数据内容',
    `remark`           text         CHARACTER SET utf8mb4     NULL COMMENT '备注',
    `creator`          varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '创建人',
    `create_time`      datetime                           NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_type_content`(`type`,`content`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;
