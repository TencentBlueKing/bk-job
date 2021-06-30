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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceVariableDAO;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StepInstanceVariableValueServiceImpl implements StepInstanceVariableValueService {

    private final StepInstanceVariableDAO stepInstanceVariableDAO;
    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceVariableService taskInstanceVariableService;

    @Autowired
    public StepInstanceVariableValueServiceImpl(StepInstanceVariableDAO stepInstanceVariableDAO,
                                                TaskInstanceService taskInstanceService,
                                                TaskInstanceVariableService taskInstanceVariableService) {
        this.stepInstanceVariableDAO = stepInstanceVariableDAO;
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
    }

    @Override
    public void saveVariableValues(StepInstanceVariableValuesDTO variableValues) {
        stepInstanceVariableDAO.saveVariableValues(variableValues);
    }

    @Override
    public List<StepInstanceVariableValuesDTO> computeOutputVariableValuesForAllStep(long taskInstanceId) {
        List<StepInstanceVariableValuesDTO> resultStepInstanceVariableValuesList = new ArrayList<>();
        List<StepInstanceBaseDTO> stepInstanceList =
            taskInstanceService.listStepInstanceByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceList)) {
            return resultStepInstanceVariableValuesList;
        }

        List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList =
            stepInstanceVariableDAO.listStepOutputVariableValuesByTaskInstanceId(taskInstanceId);
        List<TaskVariableDTO> globalVars = taskInstanceVariableService.getByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(globalVars)) {
            return resultStepInstanceVariableValuesList;
        }
        Map<String, VariableValueDTO> globalVarValueMap = new HashMap<>();
        initGlobalVarMap(globalVars, globalVarValueMap);


        stepInstanceList.forEach(stepInstance -> {
            if (!StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue().equals(stepInstance.getExecuteType())) {
                return;
            }
            StepInstanceVariableValuesDTO resultStepInstanceVariableValues = new StepInstanceVariableValuesDTO();
            resultStepInstanceVariableValues.setStepInstanceId(stepInstance.getId());
            resultStepInstanceVariableValues.setExecuteCount(stepInstance.getExecuteCount());
            List<VariableValueDTO> globalVarValues = new ArrayList<>();
            List<StepInstanceVariableValuesDTO> variableValuesForStep = stepInstanceVariableValuesList.stream()
                .filter(stepInstanceVariableValues -> stepInstanceVariableValues.getStepInstanceId() == stepInstance.getId())
                .sorted(Comparator.comparingInt(StepInstanceVariableValuesDTO::getExecuteCount)).collect(Collectors.toList());
            log.info("variableValuesForStep->{}", variableValuesForStep);
            variableValuesForStep.forEach(variableValues -> {
                if (CollectionUtils.isNotEmpty(variableValues.getGlobalParams())) {
                    variableValues.getGlobalParams().forEach(globalVar -> globalVarValueMap.put(globalVar.getName(),
                        globalVar));
                }
            });
            if (!globalVarValueMap.isEmpty()) {
                globalVarValueMap.forEach((varName, value) -> globalVarValues.add(value));
                resultStepInstanceVariableValues.setGlobalParams(globalVarValues);
                resultStepInstanceVariableValues.setGlobalParamsMap(new HashMap<>(globalVarValueMap));
            }
            resultStepInstanceVariableValuesList.add(resultStepInstanceVariableValues);
        });
        return resultStepInstanceVariableValuesList;
    }

    @Override
    public StepInstanceVariableValuesDTO computeInputStepInstanceVariableValues(long taskInstanceId,
                                                                                long stepInstanceId,
                                                                                List<TaskVariableDTO> taskVariables) {
        TaskVariablesAnalyzeResult variablesAnalyzeResult = new TaskVariablesAnalyzeResult(taskVariables);
        StepInstanceVariableValuesDTO inputStepInstanceVariableValues = new StepInstanceVariableValuesDTO();
        inputStepInstanceVariableValues.setTaskInstanceId(taskInstanceId);
        inputStepInstanceVariableValues.setStepInstanceId(stepInstanceId);
        if (!variablesAnalyzeResult.isExistAnyVar()) {
            return inputStepInstanceVariableValues;
        }

        List<HostVariableValuesDTO> namespaceVarValues = new ArrayList<>();
        List<VariableValueDTO> globalVarValues = new ArrayList<>();
        Map<String, VariableValueDTO> globalVarValueMap = new HashMap<>();
        Map<String, Map<String, VariableValueDTO>> namespaceVarValueMap = new HashMap<>();
        inputStepInstanceVariableValues.setNamespaceParams(namespaceVarValues);
        inputStepInstanceVariableValues.setNamespaceParamsMap(namespaceVarValueMap);
        inputStepInstanceVariableValues.setGlobalParams(globalVarValues);
        inputStepInstanceVariableValues.setGlobalParamsMap(globalVarValueMap);

        initGlobalVarMap(taskVariables, globalVarValueMap);

        if (variablesAnalyzeResult.isExistOnlyConstVar()) {
            if (!globalVarValueMap.isEmpty()) {
                globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
            }
            return inputStepInstanceVariableValues;
        }

        List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList =
            stepInstanceVariableDAO.listSortedPreStepOutputVariableValues(taskInstanceId, stepInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceVariableValuesList)) {
            if (!globalVarValueMap.isEmpty()) {
                globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
            }
            return inputStepInstanceVariableValues;
        }

        stepInstanceVariableValuesList.forEach(stepInstanceVariableValues -> {
            List<VariableValueDTO> stepGlobalParams = stepInstanceVariableValues.getGlobalParams();
            if (CollectionUtils.isNotEmpty(stepGlobalParams)) {
                stepGlobalParams.forEach(globalParam -> globalVarValueMap.put(globalParam.getName(), globalParam));
            }

            if (variablesAnalyzeResult.isExistNamespaceVar()) {
                List<HostVariableValuesDTO> stepNamespaceParams = stepInstanceVariableValues.getNamespaceParams();
                if (CollectionUtils.isNotEmpty(stepNamespaceParams)) {
                    stepNamespaceParams.forEach(hostVariableValues -> {
                        if (CollectionUtils.isEmpty(hostVariableValues.getValues())) {
                            return;
                        }
                        Map<String, VariableValueDTO> hostVariables = namespaceVarValueMap.computeIfAbsent(
                            hostVariableValues.getIp(), k -> new HashMap<>());
                        hostVariableValues.getValues().forEach(variable -> hostVariables.put(variable.getName(),
                            variable));
                    });
                }
            }
        });

        if (!globalVarValueMap.isEmpty()) {
            globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
        }

        if (variablesAnalyzeResult.isExistNamespaceVar()) {
            namespaceVarValueMap.forEach((ip, param) -> {
                HostVariableValuesDTO hostVariableValues = new HostVariableValuesDTO();
                hostVariableValues.setIp(ip);
                if (param != null && !param.isEmpty()) {
                    List<VariableValueDTO> values = new ArrayList<>();
                    param.forEach((paramName, paramValue) -> values.add(paramValue));
                    hostVariableValues.setValues(values);
                }
                namespaceVarValues.add(hostVariableValues);
            });
        }

        return inputStepInstanceVariableValues;
    }

    private void initGlobalVarMap(List<TaskVariableDTO> taskVariables,
                                  Map<String, VariableValueDTO> globalVarValueMap) {
        taskVariables.forEach(taskVariable -> {
            VariableValueDTO variableValue = new VariableValueDTO();
            variableValue.setName(taskVariable.getName());
            variableValue.setType(taskVariable.getType());
            variableValue.setValue(taskVariable.getValue());
            variableValue.setServerValue(taskVariable.getTargetServers());
            if (TaskVariableTypeEnum.valOf(taskVariable.getType()) != TaskVariableTypeEnum.NAMESPACE) {
                globalVarValueMap.put(variableValue.getName(), variableValue);
            }
        });
    }
}
