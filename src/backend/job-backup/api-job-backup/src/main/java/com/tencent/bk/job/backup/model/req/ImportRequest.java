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

package com.tencent.bk.job.backup.model.req;

import com.tencent.bk.job.backup.model.web.BackupTemplateInfoVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 23/7/2020 10:48
 */
@Data
@ApiModel("作业导入请求")
public class ImportRequest {

    /**
     * 重名时添加的后缀
     */
    @ApiModelProperty(value = "重名后缀", required = true)
    private String duplicateSuffix;

    /**
     * ID 冲突时的处理方式
     *
     * @see com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum
     */
    @ApiModelProperty(value = "冲突 ID 处理，0-不保留，自增 1-保留，冲突时自增 2-保留，冲突时不导入", required = true)
    private Integer duplicateIdHandler;

    /**
     * 需要导入的模版信息
     *
     * @see BackupTemplateInfoVO
     */
    @ApiModelProperty(value = "需要导入的模版信息", required = true)
    private List<BackupTemplateInfoVO> templateInfo;

    public boolean validate() {
        if (StringUtils.isBlank(duplicateSuffix)) {
            JobContextUtil.addDebugMessage("Duplicate suffix cannot be empty");
            return false;
        }
        if (duplicateIdHandler == null || duplicateIdHandler < 0 || duplicateIdHandler > 2) {
            JobContextUtil.addDebugMessage("Invalid duplicate handler");
            return false;
        }
        if (CollectionUtils.isEmpty(templateInfo)) {
            JobContextUtil.addDebugMessage("Template info cannot be empty");
            return false;
        }
        for (BackupTemplateInfoVO backupTemplateInfoVO : templateInfo) {
            if (!backupTemplateInfoVO.validate()) {
                JobContextUtil.addDebugMessage("Template info invalid");
                return false;
            }
        }
        return true;
    }
}
