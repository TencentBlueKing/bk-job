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

package com.tencent.bk.job.manage.model.dto.converter;

import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
import com.tencent.bk.job.manage.model.web.vo.script.ScriptRelatedTemplateStepVO;

/**
 * 引用脚本的作业模板步骤Converter
 */
public class ScriptRelatedTemplateStepConverter {

    public static ScriptRelatedTemplateStepVO convertToScriptRelatedTemplateStepVO(ScriptSyncTemplateStepDTO step) {
        if (step == null) {
            return null;
        }

        ScriptRelatedTemplateStepVO vo = new ScriptRelatedTemplateStepVO();
        vo.setScriptId(step.getScriptId());
        vo.setScriptVersionId(step.getScriptVersionId());
        vo.setScriptName(step.getScriptName());
        vo.setScriptStatus(step.getScriptStatus());
        vo.setScriptVersion(step.getScriptVersion());
        vo.setStepId(step.getStepId());
        vo.setStepName(step.getStepName());
        vo.setTemplateId(step.getTemplateId());
        vo.setTemplateName(step.getTemplateName());
        return vo;
    }
}
