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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateDTO;
import com.tencent.bk.job.manage.model.dto.customsetting.ScriptTemplateVariableRenderDTO;

import java.util.List;

/**
 * 用户自定义脚本模板Service
 */
public interface CustomScriptTemplateService {
    /**
     * 查询用户自定义的脚本模板
     *
     * @param username 用户名
     * @return 脚本模板列表；如果不存在，返回空的List
     */
    List<ScriptTemplateDTO> listCustomScriptTemplate(String username);

    /**
     * 保存用户的自定义脚本模板
     *
     * @param username       用户名
     * @param scriptTemplate 脚本模板
     */
    void saveScriptTemplate(String username, ScriptTemplateDTO scriptTemplate);

    /**
     * 脚本渲染
     *
     * @param scriptTemplateVariableRender 脚本模板变量值
     * @param scriptTemplate               脚本模板
     */
    void renderScriptTemplate(ScriptTemplateVariableRenderDTO scriptTemplateVariableRender,
                              ScriptTemplateDTO scriptTemplate);
}
