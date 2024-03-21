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

package com.tencent.bk.job.execute.model.esb.v3.bkci.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiExecuteObjectDTO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class EsbJobInstanceStatusDTO {

    @JsonProperty("finished")
    private boolean finished;

    @JsonProperty("job_instance")
    private JobInstance jobInstance;

    @JsonProperty("step_instance_list")
    private List<StepResult> stepResults;

    @Setter
    @Getter
    public static class JobInstance extends EsbAppScopeDTO {
        @JsonProperty("job_instance_id")
        private Long id;
        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        @JsonProperty("status")
        private Integer status;
        /**
         * 作业实例名称
         */
        @JsonProperty("name")
        private String name;
        /**
         * 脚本创建时间
         */
        @JsonProperty("create_time")
        private Long createTime;
        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;
        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        private Long endTime;
        /**
         * 总耗时，单位：秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
    }

    /**
     * 步骤实例状态
     */
    @Setter
    @Getter
    public static class StepResult {
        /**
         * id
         */
        @JsonProperty("step_instance_id")
        private Long id;
        /**
         * 名称
         */
        @JsonProperty("name")
        private String name;
        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        @JsonProperty("status")
        private Integer status;

        /**
         * 步骤类型：1.脚本步骤; 2.文件步骤; 3.人工确认
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
         * 结束时间
         */
        @JsonProperty("end_time")
        private Long endTime;

        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;

        /**
         * 创建时间
         */
        @JsonProperty("create_time")
        private Long createTime;

        @JsonProperty("execute_object_result_list")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<ExecuteObjectResult> executeObjectResults;
    }

    /**
     * 执行对象任务结果
     */
    @Setter
    @Getter
    public static class ExecuteObjectResult {

        @JsonProperty("execute_object")
        private OpenApiExecuteObjectDTO executeObject;

        private Integer status;

        private String tag;

        @JsonProperty("exit_code")
        private Integer exitCode;

        @JsonProperty("error_code")
        private Integer errorCode;

        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;

        /**
         * 结束时间
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
