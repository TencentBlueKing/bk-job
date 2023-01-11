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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            log.info("Step instance is empty! taskInstanceId: {}", taskInstanceId);
            return resultStepInstanceVariableValuesList;
        }

        List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList =
            stepInstanceVariableDAO.listStepOutputVariableValuesByTaskInstanceId(taskInstanceId);
        List<TaskVariableDTO> globalVars = taskInstanceVariableService.getByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(globalVars)) {
            return resultStepInstanceVariableValuesList;
        }
        Map<String, VariableValueDTO> globalVarValueMap = initGlobalVarMap(globalVars);

        stepInstanceList.forEach(stepInstance -> {
            if (!StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue().equals(stepInstance.getExecuteType())) {
                return;
            }
            StepInstanceVariableValuesDTO resultStepInstanceVariableValues = new StepInstanceVariableValuesDTO();
            resultStepInstanceVariableValues.setStepInstanceId(stepInstance.getId());
            resultStepInstanceVariableValues.setExecuteCount(stepInstance.getExecuteCount());
            List<VariableValueDTO> globalVarValues = new ArrayList<>();
            List<StepInstanceVariableValuesDTO> variableValuesForStep = stepInstanceVariableValuesList.stream()
                .filter(stepInstanceVariableValues ->
                    stepInstanceVariableValues.getStepInstanceId() == stepInstance.getId())
                .sorted(Comparator.comparingInt(StepInstanceVariableValuesDTO::getExecuteCount))
                .collect(Collectors.toList());
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
        if (!variablesAnalyzeResult.isExistAnyVar()) {
            // 如果不存在任何变量，无需进一步处理
            return null;
        }
        StepInstanceVariableValuesDTO inputStepInstanceVariableValues = new StepInstanceVariableValuesDTO();
        inputStepInstanceVariableValues.setTaskInstanceId(taskInstanceId);
        inputStepInstanceVariableValues.setStepInstanceId(stepInstanceId);

        // 初始化全局变量
        List<VariableValueDTO> globalVarValues = new ArrayList<>();
        inputStepInstanceVariableValues.setGlobalParams(globalVarValues);
        // key: varName value: varValue
        Map<String, VariableValueDTO> globalVarValueMap = initGlobalVarMap(taskVariables);
        inputStepInstanceVariableValues.setGlobalParamsMap(globalVarValueMap);

        // 如果作业只包含常量，由于变量值不可变，可以直接返回初始全局变量的值
        if (variablesAnalyzeResult.isExistOnlyConstVar()) {
            if (!globalVarValueMap.isEmpty()) {
                globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
            }
            return inputStepInstanceVariableValues;
        }

        // 如果包含可变变量，那么需要获取前面所有步骤的输出变量值来进行处理
        List<StepInstanceVariableValuesDTO> preStepInstanceVariableValuesList =
            stepInstanceVariableDAO.listSortedPreStepOutputVariableValues(taskInstanceId, stepInstanceId);
        if (CollectionUtils.isEmpty(preStepInstanceVariableValuesList)) {
            if (!globalVarValueMap.isEmpty()) {
                globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
            }
            return inputStepInstanceVariableValues;
        }

        // 按步骤执行先后顺序覆盖更新全局变量值
        updateGlobalVarValues(preStepInstanceVariableValuesList, globalVarValues, globalVarValueMap);

        // 处理命名空间变量
        if (variablesAnalyzeResult.isExistNamespaceVar()) {
            updateNamespaceVarValues(inputStepInstanceVariableValues, preStepInstanceVariableValuesList);
        }

        return inputStepInstanceVariableValues;
    }

    private void updateGlobalVarValues(List<StepInstanceVariableValuesDTO> stepInstanceVariableValuesList,
                                       List<VariableValueDTO> globalVarValues,
                                       Map<String, VariableValueDTO> globalVarValueMap) {
        stepInstanceVariableValuesList.forEach(stepInstanceVariableValues -> {
            List<VariableValueDTO> stepGlobalParams = stepInstanceVariableValues.getGlobalParams();
            if (CollectionUtils.isNotEmpty(stepGlobalParams)) {
                // 覆盖全局变量初始值
                stepGlobalParams.forEach(globalParam -> globalVarValueMap.put(globalParam.getName(), globalParam));
            }
        });
        if (!globalVarValueMap.isEmpty()) {
            globalVarValueMap.forEach((paramName, param) -> globalVarValues.add(param));
        }
    }

    private void updateNamespaceVarValues(StepInstanceVariableValuesDTO inputStepInstanceVariableValues,
                                          List<StepInstanceVariableValuesDTO> preStepInstanceVariableValuesList) {
        if (CollectionUtils.isEmpty(preStepInstanceVariableValuesList)) {
            return;
        }

        List<HostVariableValuesDTO> namespaceVarValues = new ArrayList<>();
        inputStepInstanceVariableValues.setNamespaceParams(namespaceVarValues);
        Map<HostDTO, Map<String, VariableValueDTO>> namespaceParamsMap = new HashMap<>();
        inputStepInstanceVariableValues.setNamespaceParamsMap(namespaceParamsMap);

        preStepInstanceVariableValuesList
            .stream()
            .filter(stepInstanceVariableValues -> CollectionUtils.isNotEmpty(stepInstanceVariableValues.getNamespaceParams()))
            .forEach(stepInstanceVariableValues ->
                stepInstanceVariableValues.getNamespaceParams().forEach(hostVar -> {
                    HostDTO host = new HostDTO();
                    host.setHostId(hostVar.getHostId());
                    if (StringUtils.isNotBlank(hostVar.getCloudIpv4())) {
                        host.setBkCloudId(IpUtils.extractBkCloudId(hostVar.getCloudIpv4()));
                        host.setIp(IpUtils.extractIp(hostVar.getCloudIpv4()));
                    }
                    if (StringUtils.isNotBlank(hostVar.getCloudIpv6())) {
                        host.setBkCloudId(IpUtils.extractBkCloudId(hostVar.getCloudIpv6()));
                        host.setIpv6(IpUtils.extractIp(hostVar.getCloudIpv6()));
                    }
                    Map<String, VariableValueDTO> hostVariables = namespaceParamsMap.computeIfAbsent(host,
                        k -> new HashMap<>());
                    hostVar.getValues().forEach(variable -> hostVariables.put(variable.getName(), variable));
                }));
        if (!namespaceParamsMap.isEmpty()) {
            namespaceParamsMap.forEach((host, param) -> {
                HostVariableValuesDTO hostVariableValues = new HostVariableValuesDTO();
                hostVariableValues.setHostId(host.getHostId());
                hostVariableValues.setCloudIpv4(host.toCloudIp());
                hostVariableValues.setCloudIpv6(host.toCloudIpv6());
                if (param != null && !param.isEmpty()) {
                    List<VariableValueDTO> values = new ArrayList<>();
                    param.forEach((paramName, paramValue) -> values.add(paramValue));
                    hostVariableValues.setValues(values);
                }
                namespaceVarValues.add(hostVariableValues);
            });
        }
    }

    private Map<String, VariableValueDTO> initGlobalVarMap(List<TaskVariableDTO> taskVariables) {
        Map<String, VariableValueDTO> globalVarValueMap = new HashMap<>();
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
        return globalVarValueMap;
    }
}
