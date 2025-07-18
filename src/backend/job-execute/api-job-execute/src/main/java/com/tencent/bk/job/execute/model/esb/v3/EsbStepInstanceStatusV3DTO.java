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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class EsbStepInstanceStatusV3DTO {

    /**
     * 步骤实例id
     */
    @JsonProperty("step_instance_id")
    private Long id;

    /**
     * 执行次数
     */
    @JsonProperty("execute_count")
    private Integer executeCount;
    /**
     * 名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 步骤类型：1.脚本步骤; 2.文件步骤; 4.SQL步骤
     */
    @JsonProperty("type")
    private Integer type;
    /**
     * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
     */
    @JsonProperty("status")
    private Integer status;
    /**
     * 创建时间
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
     * 总耗时，单位：毫秒
     */
    @JsonProperty("total_time")
    private Long totalTime;

    @JsonProperty("step_result_group_list")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<StepResultGroup> stepResultGroupList;

    @Setter
    @Getter
    public static class StepResultGroup {
        @JsonProperty("result_type")
        private Integer resultType;

        @JsonProperty("result_type_desc")
        private String resultTypeDesc;

        private String tag;

        @JsonProperty("host_size")
        private Integer hostSize;

        @JsonProperty("host_result_list")
        private List<HostResult> hostResultList;
    }

    @Setter
    @Getter
    public static class HostResult {
        @JsonProperty("bk_host_id")
        private Long hostId;

        private String ip;

        private String ipv6;

        @JsonProperty("bk_agent_id")
        private String agentId;

        @JsonProperty("bk_cloud_id")
        private Long cloudAreaId;

        @JsonProperty("bk_cloud_name")
        private String cloudAreaName;

        private Integer status;

        @JsonProperty("status_desc")
        private String statusDesc;

        private String tag;

        @JsonProperty("exit_code")
        private Integer exitCode;

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
