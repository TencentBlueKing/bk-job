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

package com.tencent.bk.job.backup.service;

import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;

import java.util.List;

/**
 * @since 29/7/2020 17:46
 */
public interface TaskTemplateService {
    /**
     * 按 ID 拉作业模版信息
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param id       作业模版 ID
     * @return 作业模版信息
     */
    TaskTemplateVO getTemplateById(String username, Long appId, Long id);

    /**
     * 检查作业模版 ID 和名称是否可用
     *
     * @param appId 业务 ID
     * @param id    作业模版 ID
     * @param name  作业模版名称
     * @return 检查结果
     */
    ServiceIdNameCheckDTO checkIdAndName(Long appId, long id, String name);

    /**
     * 新建作业模版
     *
     * @param username     用户名
     * @param appId        业务 ID
     * @param taskTemplate 作业模版信息
     * @return 作业模版 ID
     */
    Long saveTemplate(String username, Long appId, TaskTemplateVO taskTemplate);

    /**
     * 获取作业模版变量信息列表
     *
     * @param username   用户名
     * @param appId      业务 ID
     * @param templateId 作业模版 ID
     * @return 作业模版变量信息列表
     */
    List<ServiceTaskVariableDTO> getTemplateVariable(String username, Long appId, Long templateId);
}
