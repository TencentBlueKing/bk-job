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
 * @since 22/7/2020 10:23
 */
@Data
@ApiModel("作业导出请求")
public class ExportRequest {

    /**
     * 压缩包名
     */
    @ApiModelProperty(value = "压缩包名", required = true)
    private String packageName;

    /**
     * 密文变量处理方式
     *
     * @see com.tencent.bk.job.backup.constant.SecretHandlerEnum
     */
    @ApiModelProperty(value = "密文变量处理方式 1-保存为空值 2-保存真实值", required = true)
    private Integer secretHandler;

    /**
     * 密码
     */
    @ApiModelProperty(value = "密码", required = true)
    private String password;

    /**
     * 过期时间
     * <p>
     * 单位：天
     */
    @ApiModelProperty(value = "过期时间 单位天", required = true)
    private Long expireTime;

    /**
     * 需要导出的模版信息
     *
     * @see BackupTemplateInfoVO
     */
    @ApiModelProperty(value = "需要导出的模版信息", required = true)
    private List<BackupTemplateInfoVO> templateInfo;

    public boolean validate() {
        if (StringUtils.isBlank(packageName)) {
            JobContextUtil.addDebugMessage("Package name cannot be null");
            return false;
        }
        if (secretHandler < 1 || secretHandler > 2) {
            JobContextUtil.addDebugMessage("Invalid secret handler");
            return false;
        }
        if (StringUtils.isNotBlank(password) && password.length() > 32) {
            JobContextUtil.addDebugMessage("Password too long");
            return false;
        }
        if (expireTime < 0) {
            JobContextUtil.addDebugMessage("Expire time invalid");
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
