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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
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

    @Autowired
    public StepInstanceServiceImpl(StepInstanceDAO stepInstanceDAO) {
        this.stepInstanceDAO = stepInstanceDAO;
    }

    @Override
    public void updateStepCurrentBatch(long stepInstanceId, int batch) {
        stepInstanceDAO.updateStepCurrentBatch(stepInstanceId, batch);
    }

    @Override
    public void updateStepCurrentExecuteCount(long stepInstanceId, int executeCount) {
        stepInstanceDAO.updateStepCurrentExecuteCount(stepInstanceId, executeCount);
    }

    @Override
    public void updateStepRollingConfigId(long stepInstanceId, long rollingConfigId) {
        stepInstanceDAO.updateStepRollingConfigId(stepInstanceId, rollingConfigId);
    }

    @Override
    public StepInstanceBaseDTO getNextStepInstance(long taskInstanceId,
                                                   int currentStepOrder) {
        return stepInstanceDAO.getNextStepInstance(taskInstanceId, currentStepOrder);
    }

    @Override
    public long addStepInstance(StepInstanceDTO stepInstance) {
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
        long stepInstanceId = stepInstance.getId();
        TaskStepTypeEnum stepType = stepInstance.getStepType();
        if (stepType == TaskStepTypeEnum.SCRIPT) {
            stepInstance.fillScriptStepInfo(stepInstanceDAO.getScriptStepInstance(stepInstanceId));
        } else if (stepType == TaskStepTypeEnum.FILE) {
            stepInstance.fillFileStepInfo(stepInstanceDAO.getFileStepInstance(stepInstanceId));
        } else if (stepType == TaskStepTypeEnum.APPROVAL) {
            stepInstance.fillConfirmStepInfo(stepInstanceDAO.getConfirmStepInstance(stepInstanceId));
        }
    }

    @Override
    public List<StepInstanceBaseDTO> listBaseStepInstanceByTaskInstanceId(long taskInstanceId) {
        return stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);
    }

    @Override
    public List<StepInstanceDTO> listStepInstanceByTaskInstanceId(long taskInstanceId) {
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
    public StepInstanceBaseDTO getBaseStepInstance(long stepInstanceId) {
        return stepInstanceDAO.getStepInstanceBase(stepInstanceId);
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(long appId, long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = getBaseStepInstance(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceBaseDTO getBaseStepInstance(long appId,
                                                   long taskInstanceId,
                                                   long stepInstanceId) throws NotFoundException {
        StepInstanceBaseDTO stepInstance = getBaseStepInstance(appId, stepInstanceId);
        if (!stepInstance.getTaskInstanceId().equals(taskInstanceId)) {
            log.warn("Step instance is not exist, taskInstanceId={}, stepInstanceId: {}",
                taskInstanceId, stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long stepInstanceId) throws NotFoundException {
        StepInstanceBaseDTO stepInstanceBase = stepInstanceDAO.getStepInstanceBase(stepInstanceId);
        if (stepInstanceBase == null) {
            log.warn("StepInstance:{} not exist", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(stepInstanceBase);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long appId, long stepInstanceId) throws NotFoundException {
        StepInstanceDTO stepInstance = getStepInstanceDetail(stepInstanceId);
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceDetail(long appId,
                                                 long taskInstanceId,
                                                 long stepInstanceId) throws NotFoundException {
        StepInstanceDTO stepInstance = getStepInstanceDetail(appId, stepInstanceId);
        if (!stepInstance.getTaskInstanceId().equals(taskInstanceId)) {
            log.warn("StepInstance:{} is not belong to taskInstance:{}", stepInstanceId, taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    @Override
    public StepInstanceBaseDTO getFirstStepInstance(long taskInstanceId) {
        return stepInstanceDAO.getFirstStepInstanceBase(taskInstanceId);
    }

    @Override
    public List<Long> getTaskStepIdList(long taskInstanceId) {
        return stepInstanceDAO.getTaskStepInstanceIdList(taskInstanceId);
    }

    @Override
    public void updateStepStatus(long stepInstanceId, int status) {
        stepInstanceDAO.updateStepStatus(stepInstanceId, status);
    }

    @Override
    public void resetStepExecuteInfoForRetry(long stepInstanceId) {
        stepInstanceDAO.resetStepExecuteInfoForRetry(stepInstanceId);
    }

    @Override
    public void resetStepStatus(long stepInstanceId) {
        stepInstanceDAO.resetStepStatus(stepInstanceId);
    }

    @Override
    public void updateStepStartTime(long stepInstanceId, Long startTime) {
        stepInstanceDAO.updateStepStartTime(stepInstanceId, startTime);
    }

    @Override
    public void updateStepStartTimeIfNull(long stepInstanceId, Long startTime) {
        stepInstanceDAO.updateStepStartTimeIfNull(stepInstanceId, startTime);
    }

    @Override
    public void updateStepEndTime(long stepInstanceId, Long endTime) {
        stepInstanceDAO.updateStepEndTime(stepInstanceId, endTime);
    }

    @Override
    public void addStepInstanceExecuteCount(long stepInstanceId) {
        stepInstanceDAO.addStepInstanceExecuteCount(stepInstanceId);
    }

    @Override
    public void updateStepTotalTime(long stepInstanceId, long totalTime) {
        stepInstanceDAO.updateStepTotalTime(stepInstanceId, totalTime);
    }

    @Override
    public void updateStepExecutionInfo(long stepInstanceId,
                                        RunStatusEnum status,
                                        Long startTime,
                                        Long endTime,
                                        Long totalTime) {
        stepInstanceDAO.updateStepExecutionInfo(stepInstanceId, status, startTime, endTime, totalTime);
    }

    @Override
    public void updateResolvedScriptParam(long stepInstanceId, boolean isSecureParam, String resolvedScriptParam) {
        stepInstanceDAO.updateResolvedScriptParam(stepInstanceId, isSecureParam, resolvedScriptParam);
    }

    @Override
    public void updateResolvedSourceFile(long stepInstanceId, List<FileSourceDTO> resolvedFileSources) {
        if (log.isDebugEnabled()) {
            log.debug("updateResolvedSourceFile={}", JsonUtils.toJson(resolvedFileSources));
        }
        stepInstanceDAO.updateResolvedSourceFile(stepInstanceId, resolvedFileSources);
    }

    @Override
    public void updateResolvedTargetPath(long stepInstanceId, String resolvedTargetPath) {
        stepInstanceDAO.updateResolvedTargetPath(stepInstanceId, resolvedTargetPath);
    }

    @Override
    public void updateConfirmReason(long stepInstanceId, String confirmReason) {
        stepInstanceDAO.updateConfirmReason(stepInstanceId, confirmReason);
    }

    @Override
    public void updateStepOperator(long stepInstanceId, String operator) {
        stepInstanceDAO.updateStepOperator(stepInstanceId, operator);
    }

    @Override
    public StepInstanceDTO getPreExecutableStepInstance(long taskInstanceId, long stepInstanceId) {
        StepInstanceBaseDTO preStepInstance = stepInstanceDAO.getPreExecutableStepInstance(taskInstanceId,
            stepInstanceId);
        if (preStepInstance == null) {
            return null;
        }
        StepInstanceDTO stepInstance = new StepInstanceDTO(preStepInstance);
        fillStepInstanceDetail(stepInstance);
        return stepInstance;
    }

    @Override
    public StepInstanceDTO getStepInstanceByTaskInstanceId(long taskInstanceId) {
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
                FileStepInstanceDTO fileStepInstance = stepInstanceDAO.getFileStepInstance(stepInstance.getId());
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
    public StepInstanceBaseDTO getStepInstanceBase(long stepInstanceId) {
        return stepInstanceDAO.getStepInstanceBase(stepInstanceId);
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
            return getStepInstanceDetail(stepInstance.getId());
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

    @Override
    public Long getStepTaskInstanceId(long appId, long stepInstanceId) {
        return stepInstanceDAO.getTaskInstanceId(appId, stepInstanceId);
    }
}
