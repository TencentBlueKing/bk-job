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

package com.tencent.bk.job.execute.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class V4JobInstanceStatusResp {

    @JsonProperty("finished")
    private Boolean finished;

    @JsonProperty("job_instance")
    private JobInstance jobInstance;

    @JsonProperty("step_instance_list")
    private List<StepInstance> stepInstances;

    @Setter
    @Getter
    public static class JobInstance extends EsbAppScopeDTO {
        @JsonProperty("job_instance_id")
        private Long id;

        /**
         * 作业状态
         * @see com.tencent.bk.job.execute.common.constants.RunStatusEnum
         */
        @JsonProperty("status")
        private Integer status;

        /**
         * 作业实例名称
         */
        @JsonProperty("name")
        private String name;

        /**
         * 创建时间，单位：毫秒
         */
        @JsonProperty("create_time")
        private Long createTime;

        /**
         * 开始时间，单位：毫秒
         */
        @JsonProperty("start_time")
        private Long startTime;

        /**
         * 结束时间，单位：毫秒
         */
        @JsonProperty("end_time")
        private Long endTime;

        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
    }

    @Setter
    @Getter
    public static class StepInstance {
        /**
         * 步骤实例id
         */
        @JsonProperty("step_instance_id")
        private Long id;

        @JsonProperty("name")
        private String name;

        /**
         * 步骤状态
         * @see com.tencent.bk.job.execute.common.constants.RunStatusEnum
         */
        @JsonProperty("status")
        private Integer status;

        /**
         * 步骤类型：1.脚本步骤; 2.文件步骤; 4.SQL步骤
         * @see com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum
         */
        @JsonProperty("type")
        private Integer type;

        /**
         * 执行次数
         */
        @JsonProperty("execute_count")
        private Integer executeCount;

        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;

        /**
         * 结束时间，单位：毫秒
         */
        @JsonProperty("end_time")
        private Long endTime;

        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;

        /**
         * 创建时间，单位：毫秒
         */
        @JsonProperty("create_time")
        private Long createTime;

        @JsonProperty("step_ip_result_list")
        private List<IpResult> stepIpResult;
    }

    @Setter
    @Getter
    public static class IpResult {
        @JsonProperty("bk_host_id")
        private Long hostId;

        private String ip;

        @JsonProperty("bk_cloud_id")
        private Long cloudAreaId;

        /**
         * 作业执行状态
         * @see com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum
         */
        private Integer status;

        /**
         * 用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数
         */
        private String tag;

        /**
         * 脚本在目标机器上的exit code。仅脚本任务存在该参数
         */
        @JsonProperty("exit_code")
        private Integer exitCode;

        /**
         * 开始时间，单位：毫秒
         */
        @JsonProperty("start_time")
        private Long startTime;

        /**
         * 结束时间，单位：毫秒
         */
        @JsonProperty("end_time")
        private Long endTime;

        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
    }
}
