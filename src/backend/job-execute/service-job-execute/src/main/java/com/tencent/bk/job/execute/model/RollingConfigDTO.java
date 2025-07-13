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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.constants.RollingTypeEnum;
import com.tencent.bk.job.execute.model.db.ExecuteObjectRollingConfigDetailDO;
import com.tencent.bk.job.execute.model.db.FileSourceRollingConfigDO;
import lombok.Data;

/**
 * 作业滚动配置实例
 */
@Data
public class RollingConfigDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 执行作业实例id
     */
    private Long taskInstanceId;

    /**
     * 滚动配置名称
     */
    private String configName;

    /**
     * 滚动配置类型
     */
    private Integer type;

    /**
     * 目标执行对象滚动配置
     */
    private ExecuteObjectRollingConfigDetailDO executeObjectRollingConfig;

    /**
     * 源文件滚动配置
     */
    private FileSourceRollingConfigDO stepFileSourceRollingConfigs;

    /**
     * 步骤是否为目标执行对象分批执行
     *
     * @param stepInstanceId 步骤实例ID
     */
    public boolean isExecuteObjectBatchRollingStep(long stepInstanceId) {
        return executeObjectRollingConfig != null
            && executeObjectRollingConfig.getStepRollingConfigs().get(stepInstanceId).isBatch();
    }

    /**
     * 步骤是否为源文件分批执行
     *
     * @param stepInstanceId 步骤实例ID
     */
    public boolean isFileSourceBatchRollingStep(long stepInstanceId) {
        return stepFileSourceRollingConfigs != null
            && stepFileSourceRollingConfigs.getStepFileSourceRollingConfigs().get(stepInstanceId) != null;
    }

    /**
     * 是否是源文件滚动配置
     *
     * @return 布尔值
     */
    public boolean isFileSourceRolling() {
        return RollingTypeEnum.FILE_SOURCE.getValue().equals(type);
    }

    /**
     * 获取序列化后的配置详情
     *
     * @return 序列化后的配置详情
     */
    public String getSerializedConfigDetail() {
        if (isFileSourceRolling()) {
            return JsonUtils.toJson(stepFileSourceRollingConfigs);
        } else {
            return JsonUtils.toJson(executeObjectRollingConfig);
        }
    }

    /**
     * 设置配置详情
     *
     * @param serializedConfigDetail 序列化的配置详情
     */
    public void setConfigDetail(String serializedConfigDetail) {
        if (isFileSourceRolling()) {
            stepFileSourceRollingConfigs = JsonUtils.fromJson(
                serializedConfigDetail,
                FileSourceRollingConfigDO.class
            );
        } else {
            executeObjectRollingConfig = JsonUtils.fromJson(
                serializedConfigDetail,
                ExecuteObjectRollingConfigDetailDO.class
            );
        }
    }

    /**
     * 获取指定步骤的滚动模式
     *
     * @param stepInstanceId 步骤实例ID
     * @return 滚动模式
     */
    public RollingModeEnum getModeOfStep(long stepInstanceId) {
        Integer mode;
        if (isFileSourceRolling()) {
            mode = stepFileSourceRollingConfigs.getStepFileSourceRollingConfigs().get(stepInstanceId).getMode();
        } else {
            mode = executeObjectRollingConfig.getMode();
        }
        return RollingModeEnum.valOf(mode);
    }
}

