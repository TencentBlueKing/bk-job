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

package com.tencent.bk.job.analysis.service.ai.impl;

import org.springframework.stereotype.Service;

/**
 * AI模板变量服务，统一提供模板变量占位符
 */
@Service
public class AITemplateVarService {

    public String getTemplateVarPlaceHolder(String varName) {
        String TEMPLATE_VAR_PREFIX = "{BK_JOB_AI_TEMPLATE_VAR";
        return TEMPLATE_VAR_PREFIX + "{" + varName + "}}";
    }

    public String getScriptTypePlaceHolder() {
        return getTemplateVarPlaceHolder("script_type");
    }

    public String getScriptTemplatePlaceHolder() {
        return getTemplateVarPlaceHolder("script_template");
    }

    public String getScriptParamsPlaceHolder() {
        return getTemplateVarPlaceHolder("script_params");
    }

    public String getScriptContentPlaceHolder() {
        return getTemplateVarPlaceHolder("script_content");
    }

    public String getErrorContentPlaceHolder() {
        return getTemplateVarPlaceHolder("error_content");
    }

    public String getFileTaskErrorSourcePlaceHolder() {
        return getTemplateVarPlaceHolder("file_task_error_source");
    }

    public String getUploadFileErrorDataPlaceHolder() {
        return getTemplateVarPlaceHolder("upload_file_error_data");
    }

    public String getDownloadFileErrorDataPlaceHolder() {
        return getTemplateVarPlaceHolder("download_file_error_data");
    }

    public String getBkHelperLinkPlaceHolder() {
        return getTemplateVarPlaceHolder("bk_helper_link");
    }

    public String getStepInstanceNamePlaceHolder() {
        return getTemplateVarPlaceHolder("step_instance_name");
    }
}
