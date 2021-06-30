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

package com.tencent.bk.job.backup.model.web;

import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @since 22/7/2020 14:42
 */
@Data
@ApiModel("待导入、导出模版信息")
public class BackupTemplateInfoVO {

    /**
     * 模版 ID
     */
    @ApiModelProperty(value = "模版 ID", required = true)
    private Long id;

    /**
     * 执行方案 ID 列表
     */
    @ApiModelProperty(value = "执行方案 ID 列表")
    private List<Long> planId;

    /**
     * 导出全部执行方案
     * <p>
     * 0-按 ID 列表导出 1-忽略 ID 列表导出全部
     */
    @ApiModelProperty(value = "导出全部执行方案 0-按 ID 列表导出 1-忽略 ID 列表导出全部")
    private Integer exportAll;

    public boolean validate() {
        if (id < 0) {
            JobContextUtil.addDebugMessage("Missing template id");
            return false;
        }
        if (exportAll == null) {
            exportAll = 0;
        }
        if (exportAll < 0 || exportAll > 1) {
            JobContextUtil.addDebugMessage("Invalid export all value");
            return false;
        }
        return true;
    }
}
