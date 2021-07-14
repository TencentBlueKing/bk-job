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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 作业实例全局变量值
 */
@Data
public class EsbJobInstanceGlobalVarValueV3DTO {

    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    @JsonProperty("step_instance_var_list")
    private List<EsbStepInstanceGlobalVarValuesV3DTO> stepGlobalVarValues;

    @Setter
    @Getter
    public static class EsbStepInstanceGlobalVarValuesV3DTO {
        @JsonProperty("step_instance_id")
        private Long stepInstanceId;

        @JsonProperty("global_var_list")
        private List<GlobalVarValueV3DTO> globalVarValues;
    }

    @Setter
    @Getter
    public static class GlobalVarValueV3DTO {
        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;

        /**
         * 变量类型
         */
        @JsonProperty("type")
        private Integer type;
    }
}
