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

package com.tencent.bk.job.manage.dao.template;

import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.model.dto.TemplateStepScriptStatusInfo;

import java.util.Collection;
import java.util.List;

public interface TaskTemplateScriptStepDAO extends TaskScriptStepDAO {

    /**
     * 获取引用该脚本的作业模版所有脚本步骤的脚本状态（包含其他脚本）
     *
     * @param scriptId        脚本 ID
     * @param scriptVersionId 脚本版本 ID
     */
    List<TemplateStepScriptStatusInfo> listAllRelatedTemplateStepsScriptStatusInfo(String scriptId,
                                                                                   Long scriptVersionId);

    /**
     * 获取作业模版包含的所有脚本步骤的脚本状态
     *
     * @param templateId 作业模版 ID
     */
    List<TemplateStepScriptStatusInfo> listStepsScriptStatusInfoByTemplateId(Long templateId);

    /**
     * 批量更新步骤的脚本状态flags
     *
     * @param stepIds           步骤 ID 列表
     * @param scriptStatusFlags 脚本状态flags
     */
    void batchUpdateScriptStatusFlags(Collection<Long> stepIds, int scriptStatusFlags);
}
