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

import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import com.tencent.bk.job.execute.model.web.vo.RollingConfigVO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * 步骤滚动配置
 */
@Data
public class StepRollingConfigDTO {
    /**
     * 滚动配置名称
     */
    private String name;
    /**
     * 滚动对象
     */
    private Integer type;
    /**
     * 滚动策略
     */
    private Integer mode;
    /**
     * 目标执行对象滚动表达式，当前不支持与源文件滚动同时配置，同时配置时，以目标执行对象滚动为准
     */
    private String expr;
    /**
     * 源文件滚动配置，当前不支持与目标执行对象同时配置
     */
    private FileSourceRollingConfigDTO fileSource;

    public static StepRollingConfigDTO fromRollingConfigVO(RollingConfigVO rollingConfigVO) {
        StepRollingConfigDTO stepRollingConfigDTO = new StepRollingConfigDTO();
        stepRollingConfigDTO.setName(StringUtils.isBlank(rollingConfigVO.getName()) ? "default" :
            rollingConfigVO.getName());
        stepRollingConfigDTO.setType(rollingConfigVO.getType());
        stepRollingConfigDTO.setMode(rollingConfigVO.getMode());
        stepRollingConfigDTO.setExpr(rollingConfigVO.getExpr());
        stepRollingConfigDTO.setFileSource(
            FileSourceRollingConfigDTO.fromVO(rollingConfigVO.getFileSource())
        );
        return stepRollingConfigDTO;
    }

    public static StepRollingConfigDTO fromEsbRollingConfig(EsbRollingConfigDTO rollingConfig) {
        StepRollingConfigDTO stepRollingConfigDTO = new StepRollingConfigDTO();
        stepRollingConfigDTO.setName("default");
        stepRollingConfigDTO.setType(rollingConfig.getType());
        stepRollingConfigDTO.setMode(rollingConfig.getMode());
        stepRollingConfigDTO.setExpr(rollingConfig.getExpression());
        stepRollingConfigDTO.setFileSource(
            FileSourceRollingConfigDTO.fromEsbFileSourceRollingConfig(rollingConfig.getFileSource())
        );
        return stepRollingConfigDTO;
    }

    /**
     * 判断是否为目标执行对象类型的滚动
     *
     * @return 布尔值
     */
    public boolean isTargetExecuteObjectRolling() {
        return RollingTypeEnum.TARGET_EXECUTE_OBJECT.getValue().equals(type) && StringUtils.isNotBlank(expr);
    }

    /**
     * 判断是否为源文件类型的滚动
     *
     * @return 布尔值
     */
    public boolean isFileSourceRolling() {
        return RollingTypeEnum.FILE_SOURCE.getValue().equals(type) && fileSource != null;
    }
}
