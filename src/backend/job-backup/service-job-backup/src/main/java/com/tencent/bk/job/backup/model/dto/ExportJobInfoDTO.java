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

package com.tencent.bk.job.backup.model.dto;

import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.SecretHandlerEnum;
import com.tencent.bk.job.backup.model.web.ExportInfoVO;
import lombok.Data;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * @since 24/7/2020 14:33
 */
@Data
public class ExportJobInfoDTO {

    /**
     * 导出任务 ID
     */
    private String id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 导出任务创建人
     */
    private String creator;

    /**
     * 导出任务创建时间
     */
    private Long createTime;

    /**
     * 导出任务更新时间
     */
    private Long updateTime;

    /**
     * 导出任务状态
     */
    private BackupJobStatusEnum status;

    /**
     * 导出文件加密密码
     */
    private String password;

    /**
     * 用户设置的导出文件名
     */
    private String packageName;

    /**
     * 密文变量处理方式
     */
    private SecretHandlerEnum secretHandler;

    /**
     * 导出文件过期时间
     */
    private Long expireTime;

    /**
     * 待导出的模版、执行方案信息列表
     */
    private List<BackupTemplateInfoDTO> templateInfo;

    /**
     * 导出文件存储的实际文件名
     */
    private String fileName;

    /**
     * 用户语言
     */
    private Locale locale;

    public ExportJobInfoDTO() {
        this.id = UUID.randomUUID().toString();
        this.createTime = System.currentTimeMillis();
        this.updateTime = createTime;
    }

    public static ExportInfoVO toVO(ExportJobInfoDTO exportInfo) {
        if (exportInfo == null) {
            return null;
        }
        ExportInfoVO exportInfoVO = new ExportInfoVO();
        exportInfoVO.setId(exportInfo.getId());
        exportInfoVO.setStatus(exportInfo.getStatus().getStatus());
        exportInfoVO.setLog(null);
        return exportInfoVO;
    }
}
