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

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.execute.dao.RollingConfigDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.rolling.RollingBatchExecuteObjectsResolver;
import com.tencent.bk.job.execute.engine.rolling.RollingExecuteObjectBatch;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import com.tencent.bk.job.execute.model.db.RollingExecuteObjectsBatchDO;
import com.tencent.bk.job.execute.model.db.StepRollingConfigDO;
import com.tencent.bk.job.execute.service.RollingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RollingConfigServiceImpl implements RollingConfigService {

    private final RollingConfigDAO rollingConfigDAO;
    private final IdGen idGen;


    @Autowired
    public RollingConfigServiceImpl(RollingConfigDAO rollingConfigDAO, IdGen idGen) {
        this.rollingConfigDAO = rollingConfigDAO;
        this.idGen = idGen;
    }

    @Override
    public List<ExecuteObject> getRollingServers(StepInstanceBaseDTO stepInstance, Integer batch) {
        long rollingConfigId = stepInstance.getRollingConfigId();
        long stepInstanceId = stepInstance.getId();

        RollingConfigDTO rollingConfig =
            rollingConfigDAO.queryRollingConfigById(stepInstance.getTaskInstanceId(), rollingConfigId);
        if (rollingConfig.isBatchRollingStep(stepInstanceId)) {
            if (batch == null || batch == 0) {
                // 忽略滚动批次，返回当前步骤的所有目标服务器
                return stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly();
            } else {
                return rollingConfig
                    .getConfigDetail()
                    .getExecuteObjectsBatchListCompatibly()
                    .stream()
                    .filter(serverBatch -> serverBatch.getBatch().equals(batch))
                    .findFirst()
                    .orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR))
                    .getExecuteObjectsCompatibly();
            }
        } else {
            return stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly();
        }
    }

    @Override
    public RollingConfigDTO saveRollingConfigForFastJob(FastTaskDTO fastTask) {
        StepInstanceDTO stepInstance = fastTask.getStepInstance();

        RollingConfigDTO taskInstanceRollingConfig = new RollingConfigDTO();
        taskInstanceRollingConfig.setTaskInstanceId(fastTask.getTaskInstance().getId());

        StepRollingConfigDTO rollingConfig = fastTask.getRollingConfig();

        String rollingConfigName = StringUtils.isBlank(rollingConfig.getName()) ? "default" : rollingConfig.getName();
        taskInstanceRollingConfig.setConfigName(rollingConfigName);

        RollingConfigDetailDO rollingConfigDetailDO = new RollingConfigDetailDO();
        rollingConfigDetailDO.setName(rollingConfigName);
        rollingConfigDetailDO.setMode(rollingConfig.getMode());
        rollingConfigDetailDO.setExpr(rollingConfig.getExpr());

        RollingBatchExecuteObjectsResolver resolver =
            new RollingBatchExecuteObjectsResolver(
                fastTask.getStepInstance().getTargetExecuteObjects().getExecuteObjectsCompatibly(),
                rollingConfig.getExpr());
        List<RollingExecuteObjectBatch> executeObjectsBatchList = resolver.resolve();
        rollingConfigDetailDO.setExecuteObjectsBatchList(
            executeObjectsBatchList.stream()
                .map(rollingExecuteObjectBatch ->
                    new RollingExecuteObjectsBatchDO(rollingExecuteObjectBatch.getBatch(),
                        rollingExecuteObjectBatch.getExecuteObjects()))
                .collect(Collectors.toList()));

        rollingConfigDetailDO.setTotalBatch(rollingConfigDetailDO.getExecuteObjectsBatchListCompatibly().size());
        taskInstanceRollingConfig.setConfigDetail(rollingConfigDetailDO);

        rollingConfigDetailDO.setIncludeStepInstanceIdList(Lists.newArrayList(stepInstance.getId()));
        Map<Long, StepRollingConfigDO> stepRollingConfigs = new HashMap<>();
        stepRollingConfigs.put(stepInstance.getId(), new StepRollingConfigDO(true));
        rollingConfigDetailDO.setStepRollingConfigs(stepRollingConfigs);

        Long rollingConfigId = addRollingConfig(taskInstanceRollingConfig);
        taskInstanceRollingConfig.setId(rollingConfigId);
        return taskInstanceRollingConfig;
    }

    @Override
    public RollingConfigDTO getRollingConfig(Long taskInstanceId, long rollingConfigId) {
        return rollingConfigDAO.queryRollingConfigById(taskInstanceId, rollingConfigId);
    }

    @Override
    public boolean isTaskRollingEnabled(long taskInstanceId) {
        return rollingConfigDAO.existsRollingConfig(taskInstanceId);
    }

    @Override
    public long addRollingConfig(RollingConfigDTO rollingConfig) {
        rollingConfig.setId(idGen.genRollingConfigId());
        return rollingConfigDAO.saveRollingConfig(rollingConfig);
    }
}
