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

import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @since 29/7/2020 17:35
 */
@Data
public class JobBackupInfoDTO {

    /**
     * 导出文件 ID
     */
    private String id;

    /**
     * 导出人
     */
    private String creator;

    /**
     * 导出时间
     */
    private Long createTime;

    /**
     * 文件过期时间
     */
    private Long expireTime;

    /**
     * 模版基本信息列表
     */
    private List<BackupTemplateInfoDTO> templateInfo;

    /**
     * 模版详细信息
     */
    private Map<Long, TaskTemplateVO> templateDetailInfoMap;

    /**
     * 执行方案详细信息
     */
    private Map<Long, TaskPlanVO> planDetailInfoMap;

    /***
     * 模版变量值列表
     */
    private Map<Long, Map<String, String>> templateVariableValueMap;

    /**
     * 执行方案变量值列表
     */
    private Map<Long, Map<String, String>> planVariableValueMap;

    /**
     * 模版本地文件列表
     */
    private Map<Long, List<String>> templateFileList;

    /**
     * 执行方案本地文件列表
     */
    private Map<Long, List<String>> planFileList;

    /**
     * 引用脚本内容列表
     */
    private Map<String, Map<Long, ServiceScriptDTO>> linkScriptContentMap;

    /**
     * 引用账号信息列表
     */
    private List<ServiceAccountDTO> accountList;
}
