/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.model.esb.v2;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.common.util.json.LongToDecimalJsonSerializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class EsbStepInstanceResultAndLog {
    /**
     * 作业是否结束了
     */
    @JsonProperty("is_finished")
    private Boolean finished;
    /**
     * 作业步骤实例ID
     */
    @JsonProperty("step_instance_id")
    private Long stepInstanceId;
    /**
     * 作业实例名称
     */
    private String name;
    /**
     * 作业步骤状态码: 1.未执行; 2.正在执行;3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; 7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功; 12.步骤强制终止失败
     */
    private Integer status;
    /**
     * 当前步骤下所有ip的日志 按tag分类或 ip的执行状态(ip_status)归类存放
     */
    @JsonProperty("step_results")
    private List<StepInstResultDTO> stepResults;

    @Data
    public static class StepInstResultDTO {
        /**
         * 主机任务状态码:
         * 1.Agent异常; 3.上次已成功; 5.等待执行; 7.正在执行; 9.执行成功;
         * 11.任务失败; 12.任务下发失败; 13.任务超时; 15.任务日志错误; 101.脚本执行失败;
         * 102.脚本执行超时; 103.脚本执行被终止; 104.脚本返回码非零; 202.文件传输失败;
         * 203.源文件不存在; 310.Agent异常; 311.用户名不存在; 320.文件获取失败;
         * 321.文件超出限制; 329.文件传输错误; 399.任务执行出错
         */
        @JsonProperty("ip_status")
        private Integer ipStatus;
        /**
         * 脚本用job_success/job_fail 函数返回的标签内容
         */
        private String tag;
        /**
         * ip 日志内容
         */
        @JsonProperty("ip_logs")
        private List<EsbGseAgentTaskDTO> ipLogs;
    }

    @Data
    public static class EsbGseAgentTaskDTO {
        /**
         * 开始执行时间，YYYY-MM-DD HH:mm:ss
         */
        @JsonProperty("start_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long startTime;
        /**
         * 执行结束时间，YYYY-MM-DD HH:mm:ss格式
         */
        @JsonProperty("end_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long endTime;
        /**
         * 总耗时,秒
         */
        @JsonProperty("total_time")
        @JsonSerialize(using = LongToDecimalJsonSerializer.class)
        private Long totalTime;
        /**
         * 步骤执行次数
         */
        @JsonProperty("execute_count")
        private Integer executeCount;
        /**
         * 作业执行中出错码
         */
        @JsonProperty("error_code")
        private Integer errCode;
        /**
         * shell脚本退出码; 0正常; 非0异常
         */
        @JsonProperty("exit_code")
        private Integer exitCode;
        /**
         * 作业脚本输出的日志内容
         */
        @JsonProperty("log_content")
        private String logContent;

        @JsonProperty("bk_cloud_id")
        private Long cloudAreaId;

        private String ip;
    }
}
