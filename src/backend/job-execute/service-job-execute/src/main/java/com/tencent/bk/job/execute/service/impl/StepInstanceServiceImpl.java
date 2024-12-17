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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SCRIPT;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SQL;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.MANUAL_CONFIRM;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.SEND_FILE;

@Slf4j
@Service
public class StepInstanceServiceImpl implements StepInstanceService {

    private final StepInstanceDAO stepInstanceDAO;
    private final IdGen idGen;

    @Autowired
    public StepInstanceServiceImpl(StepInstanceDAO stepInstanceDAO, IdGen idGen) {
        this.stepInstanceDAO = stepInstanceDAO;
        this.idGen = idGen;
    }

    @Override
    public void updateStepCurrentBatch(Long taskInstanceId, long stepInstanceId, int batch) {
        stepInstanceDAO.updateStepCurrentBatch(taskInstanceId, stepInstanceId, batch);
    }

    @Override
    public void updateStepRollingConfigId(Long taskInstanceId, long stepInstanceId, long rollingConfigId) {
        stepInstanceDAO.updateStepRollingConfigId(taskInstanceId, stepInstanceId, rollingConfigId);
    }

    @Override
    public StepInstanceBaseDTO getNextStepInstance(Long taskInstanceId,
                                                   int currentStepOrder) {
        return stepInstanceDAO.getNextStepInstance(taskInstanceId, currentStepOrder);
    }

    @Override
    public long addStepInstance(StepInstanceDTO stepInstance) {
        stepInstance.setId(idGen.genStepInstanceId());
        long stepInstanceId = stepInstanceDAO.addStepInstanceBase(stepInstance);
        if (stepInstanceId > 0) {
            stepInstance.setId(stepInstanceId);
            StepExecuteTypeEnum executeType = stepInstance.getExecuteType();
            if (executeType == EXECUTE_SQL || executeType == EXECUTE_SCRIPT) {
                stepInstanceDAO.addScriptStepInstance(stepInstance);
            } else if (executeType == SEND_FILE) {
                stepInstanceDAO.addFileStepInstance(stepInstance);
            } else if (executeType == MANUAL_CONFIRM) {
                stepInstanceDAO.addConfirmStepInstance(stepInstance);
            }
        }
        return stepInstanceId;
    }

    private void fillStepInstanceDetail(StepInstanceDTO stepInstance) {
        Long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        TaskStepTypeEnum stepType = stepInstance.getStepType();
        if (stepType == TaskStepTypeEnum.SCRIPT) {
            stepInstance.fillScriptStepInfo(stepInstanceDAO.getScriptStepInstance(taskInstanceId, stepInstanceId));
        } else if (stepType == TaskStepTypeEnum.FILE) {
            stepInstance.fillFileStepInfo(stepInstanceDAO.getFileStepInstance(taskInstanceId, stepInstanceId));
        } else if (stepType == TaskStepTypeEnum.APPROVAL) {
            stepInstance.fillConfirmStepInfo(stepInstanceDAO.getConfirmStepInstance(taskInstanceId, stepInstanceId));
        }
    }

    @Override
    public List<StepInstanceBaseDTO> listBaseStepInstanceByTaskInstanceId(Long taskInstanceId) {
        return stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
    }

    @Override
    public List<StepInstanceDTO> listStepInstanceByTaskInstanceId(Long taskInstanceId) {
        List<StepInstanceBaseDTO> stepInstanceList = listBaseStepInstanceByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceList)) {
            return Collections.emptyList();
        }
        return stepInstanceList.stream()
            .map(baseStepInstance -> {
                StepInstanceDTO stepInstance = new StepInstanceDTO(baseStepInstance);
                fillStepInstanceDetail(stepInstance);
                return stepInstance;
            }).collect(Collectors.toList());
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(Long taskInstanceId, long stepInstanceId) {
        return stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstanceById(long stepInstanceId) {
        return stepInstanceDAO.getStepInstanceBase(stepInstanceId);
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(long appId, Long taskInstanceId, long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = getBaseStepInstance(taskInstanceId, stepInstanceId);
        if (stepInstance == null) {
            log.warn("Step instance is not exist, taskInstanceId={}, stepInstanceId: {}",
                taskInstanceId, stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(Long taskInstanceId, long stepInstanceId) throws NotFoundException {
        StepInstanceBaseDTO stepInstanceBase = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);
        if (stepInstanceBase == null) {
            log.warn("StepInstance:{} not exist", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(stepInstanceBase);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long appId, Long taskInstanceId,
                                                 long stepInstanceId) throws NotFoundException {
        StepInstanceDTO stepInstance = getStepInstanceDetail(taskInstanceId, stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceBaseDTO getFirstStepInstance(Long taskInstanceId) {
        return stepInstanceDAO.getFirstStepInstanceBase(taskInstanceId);
    }

    @Override
    public List<Long> getTaskStepIdList(Long taskInstanceId) {
        return stepInstanceDAO.getTaskStepInstanceIdList(taskInstanceId);
    }

    @Override
    public void updateStepStatus(Long taskInstanceId, long stepInstanceId, int status) {
        stepInstanceDAO.updateStepStatus(taskInstanceId, stepInstanceId, status);
    }

    @Override
    public void resetStepExecuteInfoForRetry(Long taskInstanceId, long stepInstanceId) {
        stepInstanceDAO.resetStepExecuteInfoForRetry(taskInstanceId, stepInstanceId);
    }

    @Override
    public void resetStepStatus(Long taskInstanceId, long stepInstanceId) {
        stepInstanceDAO.resetStepStatus(taskInstanceId, stepInstanceId);
    }

    @Override
    public void updateStepStartTimeIfNull(Long taskInstanceId, long stepInstanceId, Long startTime) {
        stepInstanceDAO.updateStepStartTimeIfNull(taskInstanceId, stepInstanceId, startTime);
    }

    @Override
    public void updateStepEndTime(Long taskInstanceId, long stepInstanceId, Long endTime) {
        stepInstanceDAO.updateStepEndTime(taskInstanceId, stepInstanceId, endTime);
    }

    @Override
    public void addStepInstanceExecuteCount(Long taskInstanceId, long stepInstanceId) {
        stepInstanceDAO.addStepInstanceExecuteCount(taskInstanceId, stepInstanceId);
    }

    @Override
    public void updateStepExecutionInfo(Long taskInstanceId,
                                        long stepInstanceId,
                                        RunStatusEnum status,
                                        Long startTime,
                                        Long endTime,
                                        Long totalTime) {
        stepInstanceDAO.updateStepExecutionInfo(taskInstanceId, stepInstanceId, status, startTime, endTime, totalTime);
    }

    @Override
    public void updateResolvedScriptParam(Long taskInstanceId,
                                          long stepInstanceId,
                                          boolean isSecureParam,
                                          String resolvedScriptParam) {
        stepInstanceDAO.updateResolvedScriptParam(taskInstanceId, stepInstanceId, isSecureParam, resolvedScriptParam);
    }

    @Override
    public void updateResolvedSourceFile(Long taskInstanceId,
                                         long stepInstanceId,
                                         List<FileSourceDTO> resolvedFileSources) {
        if (log.isDebugEnabled()) {
            log.debug("updateResolvedSourceFile={}", JsonUtils.toJson(resolvedFileSources));
        }
        stepInstanceDAO.updateResolvedSourceFile(taskInstanceId, stepInstanceId, resolvedFileSources);
    }

    @Override
    public void updateResolvedTargetPath(Long taskInstanceId, long stepInstanceId, String resolvedTargetPath) {
        stepInstanceDAO.updateResolvedTargetPath(taskInstanceId, stepInstanceId, resolvedTargetPath);
    }

    @Override
    public void updateConfirmReason(Long taskInstanceId, long stepInstanceId, String confirmReason) {
        stepInstanceDAO.updateConfirmReason(taskInstanceId, stepInstanceId, confirmReason);
    }

    @Override
    public void updateStepOperator(Long taskInstanceId, long stepInstanceId, String operator) {
        stepInstanceDAO.updateStepOperator(taskInstanceId, stepInstanceId, operator);
    }

    @Override
    public StepInstanceDTO getPreExecutableStepInstance(Long taskInstanceId, long stepInstanceId) {
        StepInstanceBaseDTO currentStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);
        if (currentStepInstance == null) {
            return null;
        }
        StepInstanceBaseDTO preStepInstance = stepInstanceDAO.getPreExecutableStepInstance(taskInstanceId,
            currentStepInstance.getStepOrder());
        if (preStepInstance == null) {
            return null;
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(preStepInstance);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public Map<Long, Integer> listStepInstanceIdAndStepOrderMapping(Long taskInstanceId) {
        return stepInstanceDAO.listStepInstanceIdAndStepOrderMapping(taskInstanceId);
    }

    @Override
    public StepInstanceDTO getStepInstanceByTaskInstanceId(Long taskInstanceId) {
        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
        if (CollectionUtils.isEmpty(stepInstanceList)) {
            return null;
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(stepInstanceList.get(0));
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public <K> Map<K, ExecuteObject> computeStepExecuteObjects(StepInstanceBaseDTO stepInstance,
                                                               Function<? super ExecuteObject, K> keyMapper) {

        Map<K, ExecuteObject> executeObjects = new HashMap<>();
        stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly()
            .forEach(executeObject -> {
                K key = keyMapper.apply(executeObject);
                if (key != null) {
                    executeObjects.put(keyMapper.apply(executeObject), executeObject);
                }
            });

        if (stepInstance.isFileStep()) {
            List<FileSourceDTO> fileSourceList;
            if (stepInstance instanceof StepInstanceDTO) {
                fileSourceList = ((StepInstanceDTO) stepInstance).getFileSourceList();
            } else {
                FileStepInstanceDTO fileStepInstance = stepInstanceDAO.getFileStepInstance(
                    stepInstance.getTaskInstanceId(), stepInstance.getId());
                fileSourceList = fileStepInstance.getFileSourceList();
            }

            if (CollectionUtils.isNotEmpty(fileSourceList)) {
                fileSourceList.forEach(
                    fileSource -> {
                        if (fileSource.getServers() != null
                            && CollectionUtils.isNotEmpty(fileSource.getServers().getExecuteObjectsCompatibly())) {
                            fileSource.getServers().getExecuteObjectsCompatibly()
                                .forEach(executeObject -> {
                                    K key = keyMapper.apply(executeObject);
                                    if (key != null) {
                                        executeObjects.put(keyMapper.apply(executeObject), executeObject);
                                    }
                                });
                        }
                    });
            }
        }

        return executeObjects;
    }

    @Override
    public ExecuteObject findExecuteObjectByCompositeKey(StepInstanceBaseDTO stepInstance,
                                                         ExecuteObjectCompositeKey executeObjectCompositeKey) {
        if (stepInstance.isScriptStep()) {
            return stepInstance.getTargetExecuteObjects().findExecuteObjectByCompositeKey(executeObjectCompositeKey);
        } else if (stepInstance.isFileStep()) {
            StepInstanceDTO fileStepInstance = castToStepInstanceDTO(stepInstance);
            ExecuteObject executeObject =
                fileStepInstance.getTargetExecuteObjects().findExecuteObjectByCompositeKey(executeObjectCompositeKey);
            if (executeObject != null) {
                return executeObject;
            }
            for (FileSourceDTO fileSource : fileStepInstance.getFileSourceList()) {
                if (fileSource.getServers() == null) {
                    continue;
                }
                executeObject = fileSource.getServers().findExecuteObjectByCompositeKey(executeObjectCompositeKey);
                if (executeObject != null) {
                    return executeObject;
                }
            }
            log.warn("ExecuteObject not exist query by executeObjectCompositeKey: {}", executeObjectCompositeKey);
            return null;
        } else {
            throw new InternalException("Not support method invoke for step", ErrorCode.INTERNAL_ERROR);
        }
    }

    private StepInstanceDTO castToStepInstanceDTO(StepInstanceBaseDTO stepInstance) {
        if (stepInstance instanceof StepInstanceDTO) {
            return (StepInstanceDTO) stepInstance;
        } else {
            return getStepInstanceDetail(stepInstance.getTaskInstanceId(), stepInstance.getId());
        }
    }

    @Override
    public List<ExecuteObject> findExecuteObjectByCompositeKeys(
        StepInstanceBaseDTO stepInstance,
        Collection<ExecuteObjectCompositeKey> executeObjectCompositeKeys) {
        if (stepInstance.isScriptStep()) {
            return stepInstance.getTargetExecuteObjects().findExecuteObjectByCompositeKeys(executeObjectCompositeKeys);
        } else if (stepInstance.isFileStep()) {
            StepInstanceDTO fileStepInstance = castToStepInstanceDTO(stepInstance);
            Set<ExecuteObject> executeObjects = new HashSet<>();
            List<ExecuteObject> matchTargetExecuteObjects =
                fileStepInstance.getTargetExecuteObjects().findExecuteObjectByCompositeKeys(executeObjectCompositeKeys);
            if (CollectionUtils.isNotEmpty(matchTargetExecuteObjects)) {
                executeObjects.addAll(matchTargetExecuteObjects);
            }
            for (FileSourceDTO fileSource : fileStepInstance.getFileSourceList()) {
                if (fileSource.getServers() == null) {
                    continue;
                }
                List<ExecuteObject> fileSourceExecuteObjects =
                    fileSource.getServers().findExecuteObjectByCompositeKeys(executeObjectCompositeKeys);
                if (CollectionUtils.isNotEmpty(fileSourceExecuteObjects)) {
                    executeObjects.addAll(fileSourceExecuteObjects);
                }
            }
            return new ArrayList<>(executeObjects);
        } else {
            throw new InternalException("Not support method invoke for step", ErrorCode.INTERNAL_ERROR);
        }
    }
}
