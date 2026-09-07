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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableTypeDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 判断请求里的某个全局变量取值是否属于密文类型。
 * <p>
 * 变量的身份在 v4 协议里既可以给 ID 也可以给名称，与下游 {@code resolvePlanVariable} 保持同一套定位规则：
 * <b>给了 ID 就以 ID 为准，只给名称才按名称定位</b>，避免两处判定出现分叉。
 */
@Slf4j
public class CipherVarMatcher {

    private final Map<Long, TaskVariableTypeEnum> typeById = new HashMap<>();
    private final Map<String, TaskVariableTypeEnum> typeByName = new HashMap<>();

    public CipherVarMatcher(List<ServiceTaskVariableTypeDTO> variables) {
        if (variables == null) {
            return;
        }
        for (ServiceTaskVariableTypeDTO variable : variables) {
            TaskVariableTypeEnum type = TaskVariableTypeEnum.valOf(variable.getType());
            if (variable.getId() != null) {
                typeById.put(variable.getId(), type);
            }
            if (StringUtils.isNotBlank(variable.getName())) {
                typeByName.put(variable.getName(), type);
            }
        }
    }

    /**
     * 变量是否需要加密。
     * <p>
     * 定位不到变量时按<b>需要加密</b>处理：正常流程下预检已确认变量存在，此处定位不到多半是数据异常，
     * 此时宁可加密一个普通变量，也不能把密码类变量明文写进库里。
     */
    public boolean needEncrypt(Long varId, String varName) {
        TaskVariableTypeEnum type = resolveType(varId, varName);
        if (type == null) {
            log.warn("Global var not found in job plan/template, encrypt it anyway. varId={}, varName={}",
                varId, varName);
            return true;
        }
        return type == TaskVariableTypeEnum.CIPHER;
    }

    private TaskVariableTypeEnum resolveType(Long varId, String varName) {
        if (varId != null) {
            return typeById.get(varId);
        }
        return StringUtils.isBlank(varName) ? null : typeByName.get(varName);
    }
}
