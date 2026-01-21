/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.constant.ArchiveModeEnum;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "job.backup.archive.execute")
public class ArchiveProperties {
    /**
     * 是否启用 DB 归档
     */
    private boolean enabled;

    /**
     * 归档模式
     *
     * @see ArchiveModeEnum
     */
    private String mode;

    /**
     * 触发时间 CRON 表达式
     */
    private String cron;

    /**
     * DB数据保留天数
     */
    private int keepDays = 30;

    /**
     * 归档数据时间范围计算所依据的时区，如果不指定默认为系统时区
     */
    private String timeZone;

    /**
     * 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
     */
    private int batchInsertRowSize = 1000;

    /**
     * 每次执行删除的最大行数
     */
    private int deleteRowLimit = 1000;

    /**
     * 每批次从 db 表中读取的记录数量
     */
    private int readRowLimit = 1000;

    /**
     * 检查归档结果
     */
    private CheckArchiveResultConfig check;

    private Map<String, TableConfig> tableConfigs;

    /**
     * 归档任务配置
     */
    private ArchiveTasksConfig tasks;

    @Data
    public static class TableConfig {
        /**
         * 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
         */
        private Integer batchInsertRowSize;

        /**
         * 每次执行删除的最大行数
         */
        private Integer deleteRowLimit;

        /**
         * 每批次从 db 表中读取的记录数量
         */
        private Integer readRowLimit;
    }

    @Data
    public static class ArchiveTasksConfig {
        /**
         * 归档任务配置
         */
        private ArchiveTaskConfig jobInstance;
    }

    @Data
    public static class ArchiveTaskConfig {
        /**
         * 数据源模式
         *
         * @see com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode
         */
        private String dataSourceMode;

        /**
         * 要归档的分库分表数据节点配置
         */
        private List<ShardingDataNode> shardingDataNodes;
        /**
         * 归档任务并行执行数量
         */
        private Integer concurrent = 6;
    }


    @Data
    public static class ShardingDataNode {
        /**
         * 数据源名称
         */
        private String dataSourceName;
        /**
         * 分库数量
         */
        private Integer dbCount;
        /**
         * 分表数量
         */
        private Integer tableCount;
    }

    /**
     * 检查归档结果
     * 1. 检查是否有超出保留天数的数据
     * 2. 检查这个时间的归档任务是否完成
     * 若是有超出保留天数的数据，且归档任务完成，则视为归档异常
     */
    @Data
    public static class CheckArchiveResultConfig {

        /**
         * 是否开启
         */
        private boolean enabled;

        /**
         * 检查cron表达式
         */
        private String cron;

        /**
         * 允许存在的超出保留天数的天数
         */
        private int allowExtraDays;

        /**
         * 当发现有归档不完全时，是否重调度这个时间点的归档任务
         */
        private boolean rescheduleEnabled;
    }

}
