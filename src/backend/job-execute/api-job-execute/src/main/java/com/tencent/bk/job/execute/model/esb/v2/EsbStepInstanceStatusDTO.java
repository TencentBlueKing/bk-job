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
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.common.util.json.LongToDecimalJsonSerializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
public class EsbStepInstanceStatusDTO {
    @JsonProperty("is_finished")
    private Boolean isFinished;

    @JsonProperty("step_instance_detail")
    private StepInstance stepInstance;

    @JsonProperty("step_instance_analyse_result")
    private List<Map<String, Object>> ayalyseResult;

    @Getter
    @Setter
    @ToString
    public static class StepInstance extends EsbAppScopeDTO {
        @JsonProperty("step_instance_id")
        private Long id;

        /**
         * 执行步骤id
         */
        @JsonProperty("step_id")
        private Long stepId;

        /**
         * 执行作业实例id
         */
        @JsonProperty("job_instance_id")
        private Long taskInstanceId;

        /**
         * 名称
         */
        private String name;

        /**
         * 步骤类型：1、执行脚本，2、传输文件，3、文本通知, 4、SQL执行
         */
        private Integer type;

        /**
         * 目标机器的ip列表，逗号分割
         */
        @JsonProperty("target_ips")
        private String ipList;

        /**
         * Agent异常的ip列表，逗号分割
         */
        @JsonProperty("abnormal_agent_ips")
        private String badIpList;

        /**
         * 执行人
         */
        private String operator;

        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        private Integer status;

        /**
         * 执行次数
         */
        @JsonProperty("execute_count")
        private Integer executeCount;

        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long startTime;

        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long endTime;

        /**
         * 总耗时，单位：秒
         */
        @JsonProperty("total_time")
        @JsonSerialize(using = LongToDecimalJsonSerializer.class)
        private Long totalTime;

        /**
         * 总ip数量
         */
        @JsonProperty("total_ip_num")
        private Integer totalIPNum;

        /**
         * 没有agent
         */
        @JsonProperty("abnormal_agent_ip_num")
        private Integer badIPNum;

        /**
         * 有agent
         */
        @JsonProperty("running_ip_num")
        private Integer runIPNum;

        /**
         * 失败ip数量
         */
        @JsonProperty("fail_ip_num")
        private Integer failIPNum;

        /**
         * 成功ip数量
         */
        @JsonProperty("success_ip_num")
        private Integer successIPNum;
    }
}
