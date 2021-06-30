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
import com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum;
import com.tencent.bk.job.backup.model.web.ImportInfoVO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 24/7/2020 14:32
 */
@Data
public class ImportJobInfoDTO {

    /**
     * 导入任务 ID
     */
    private String id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 导入任务创建人
     */
    private String creator;

    /**
     * 导入任务创建时间
     */
    private Long createTime;

    /**
     * 导入任务更新时间
     */
    private Long updateTime;

    /**
     * 导入任务状态
     */
    private BackupJobStatusEnum status;

    /**
     * 导入文件对应的导出任务 ID
     */
    private String exportId;

    /**
     * 用户上传的文件实际存储文件名
     */
    private String fileName;

    /**
     * 待导入的作业模版、执行方案信息列表
     */
    private List<BackupTemplateInfoDTO> templateInfo;

    /**
     * 重名时自动添加的后缀
     */
    private String duplicateSuffix;

    /**
     * ID 重复时的处理方式
     */
    private DuplicateIdHandlerEnum duplicateIdHandler;

    /**
     * ID 与名称对应关系表
     */
    private IdNameInfoDTO idNameInfo;

    /**
     * 用户语言
     */
    private Locale locale;

    /**
     * 原账号 ID 与现账号 ID 对应表
     */
    private Map<Long, Long> accountIdMap;

    public static ImportInfoVO toVO(ImportJobInfoDTO importJobInfoDTO) {
        if (importJobInfoDTO == null) {
            return null;
        }
        ImportInfoVO importInfoVO = new ImportInfoVO();
        importInfoVO.setId(importJobInfoDTO.getId());
        importInfoVO.setStatus(importJobInfoDTO.getStatus().getStatus());
        if (CollectionUtils.isNotEmpty(importJobInfoDTO.getTemplateInfo())) {
            importInfoVO.setTemplateInfo(importJobInfoDTO.getTemplateInfo().stream().map(BackupTemplateInfoDTO::toVO)
                .collect(Collectors.toList()));
        }
        if (importJobInfoDTO.getIdNameInfo() != null) {
            importInfoVO.setIdNameInfo(IdNameInfoDTO.toVO(importJobInfoDTO.getIdNameInfo()));
        }
        return importInfoVO;
    }
}
