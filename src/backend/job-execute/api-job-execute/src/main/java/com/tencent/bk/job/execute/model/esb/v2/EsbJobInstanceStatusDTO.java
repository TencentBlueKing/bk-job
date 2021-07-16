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

@Data
public class EsbJobInstanceStatusDTO {

    @JsonProperty("is_finished")
    private Boolean isFinished;

    @JsonProperty("job_instance")
    private JobInstance jobInstance;

    private List<Block> blocks;

    @Setter
    @Getter
    public static class JobInstance {
        @JsonProperty("job_instance_id")
        private Long id;
        /**
         * task id
         */
        @JsonProperty("bk_job_id")
        private Long taskId;
        /**
         * 业务id
         */
        @JsonProperty("bk_biz_id")
        private Long appId;
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
         * 执行人
         */
        @JsonProperty("operator")
        private String operator;
        /**
         * 脚本创建时间
         */
        @JsonProperty("create_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long createTime;
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
         * 启动方式： 1.页面执行、2.API调用、3.定时执行
         */
        @JsonProperty("start_way")
        private Integer startWay;
        /**
         * 操作列表
         */
        @JsonProperty("current_step_instance_id")
        private Long currentStepId;
    }

    @Setter
    @Getter
    public static class Block {
        /**
         * 步骤块中包含的各个步骤对象
         */
        @JsonProperty("step_instances")
        private List<StepInst> stepInstances;
    }

    @Setter
    @Getter
    public static class StepInst {
        /**
         * id
         */
        @JsonProperty("step_instance_id")
        private Long id;
        /**
         * 执行步骤id
         */
        @JsonProperty("step_id")
        private Long stepId;
        /**
         * 名称
         */
        @JsonProperty("name")
        private String name;
        /**
         * 步骤类型：1、执行脚本，2、传输文件，3、文本通知, 4、SQL执行
         */
        @JsonProperty("type")
        private Integer type;

        /**
         * 执行人
         */
        @JsonProperty("operator")
        private String operator;
        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        @JsonProperty("status")
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
         * 创建时间
         */
        @JsonProperty("create_time")
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long createTime;
        /**
         * 是否需要暂停，1.需要暂停、0.不需要暂停，默认：0。
         */
        @JsonProperty("pause")
        private Integer isPause;

        @JsonProperty("step_ip_status")
        private List<EsbIpStatusDTO> stepIpResult;
    }
}
