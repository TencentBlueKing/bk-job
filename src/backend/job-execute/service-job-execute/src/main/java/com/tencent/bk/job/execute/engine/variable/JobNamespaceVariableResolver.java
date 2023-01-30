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

package com.tencent.bk.job.execute.engine.variable;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.consts.JobBuildInVariables;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务前置步骤目标主机-变量解析器
 */
@Service
@Slf4j
public class JobNamespaceVariableResolver implements VariableResolver {
    private static final String NAMESPACE_VARIABLE_PREFIX = "JOB_NAMESPACE_";

    public JobNamespaceVariableResolver() {
    }

    @Override
    public boolean isMatch(String variable) {
        return variable.startsWith(NAMESPACE_VARIABLE_PREFIX);
    }

    public String resolve(VariableResolveContext context, String variableName) {
        StepInstanceVariableValuesDTO stepInputVariables = context.getStepInputVariables();
        if (stepInputVariables == null || CollectionUtils.isEmpty(stepInputVariables.getNamespaceParams())) {
            return "{}";
        }
        Map<String, Map<String, String>> namespaceVarsValues = new HashMap<>();
        List<HostVariableValuesDTO> namespaceValues = stepInputVariables.getNamespaceParams();
        namespaceValues.forEach(namespaceValue -> {
            if (CollectionUtils.isNotEmpty(namespaceValue.getValues())) {
                namespaceValue.getValues().forEach(variableValue -> {
                    Map<String, String> ipAndValueMap = namespaceVarsValues.computeIfAbsent(variableValue.getName(),
                        k -> new HashMap<>());
                    ipAndValueMap.put(namespaceValue.getPrimaryCloudIp(), variableValue.getValue());
                });

            }
        });
        if (namespaceValues.isEmpty()) {
            return "{}";
        }

        log.debug("Variable name: {}, namespaceVarsValues: {}", variableName, namespaceVarsValues);
        if (JobBuildInVariables.JOB_NAMESPACE_ALL.equals(variableName)) {
            return JsonUtils.toJson(namespaceVarsValues);
        } else {
            String namespaceVariableName = getNamespaceVariableName(variableName);
            Map<String, String> namespaceVariableValue = namespaceVarsValues.get(namespaceVariableName);
            if (namespaceVariableValue == null) {
                return "{}";
            }
            return JsonUtils.toJson(namespaceVariableValue);
        }

    }

    private String getNamespaceVariableName(String variableName) {
        return variableName.substring(NAMESPACE_VARIABLE_PREFIX.length());
    }

}
