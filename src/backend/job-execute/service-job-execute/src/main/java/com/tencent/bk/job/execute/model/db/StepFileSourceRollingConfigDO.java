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

package com.tencent.bk.job.execute.model.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 源文件滚动详细配置DO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@PersistenceObject
public class StepFileSourceRollingConfigDO {
    /**
     * 滚动策略
     *
     * @see com.tencent.bk.job.common.constant.RollingModeEnum
     */
    @JsonProperty("mode")
    private Integer mode;

    /**
     * 一个滚动批次中的最大执行对象数量
     */
    @JsonProperty("maxExecuteObjectNumInBatch")
    private Integer maxExecuteObjectNumInBatch;

    /**
     * 一个滚动批次中的最大文件数量
     */
    @JsonProperty("maxFileNumInBatch")
    private Integer maxFileNumInBatch;

    public static StepFileSourceRollingConfigDO fromStepRollingConfigDTO(StepRollingConfigDTO rollingConfig) {
        StepFileSourceRollingConfigDO fileSourceRollingConfig = new StepFileSourceRollingConfigDO();
        fileSourceRollingConfig.setMode(rollingConfig.getMode());
        Integer maxExecuteObjectNumInBatch = rollingConfig.getFileSource().getMaxExecuteObjectNumInBatch();
        Integer maxFileNumInBatch = rollingConfig.getFileSource().getMaxFileNumInBatch();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(maxExecuteObjectNumInBatch);
        fileSourceRollingConfig.setMaxFileNumInBatch(maxFileNumInBatch);
        return fileSourceRollingConfig;
    }
}
