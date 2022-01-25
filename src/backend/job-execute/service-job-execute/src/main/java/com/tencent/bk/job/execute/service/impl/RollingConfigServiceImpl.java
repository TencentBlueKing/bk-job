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
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.dao.TaskInstanceRollingConfigDAO;
import com.tencent.bk.job.execute.engine.rolling.RollingBatchServersResolver;
import com.tencent.bk.job.execute.engine.rolling.RollingServerBatch;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDO;
import com.tencent.bk.job.execute.model.db.RollingServerBatchDO;
import com.tencent.bk.job.execute.service.RollingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RollingConfigServiceImpl implements RollingConfigService {

    private final TaskInstanceRollingConfigDAO taskInstanceRollingConfigDAO;

    @Autowired
    public RollingConfigServiceImpl(TaskInstanceRollingConfigDAO taskInstanceRollingConfigDAO) {
        this.taskInstanceRollingConfigDAO = taskInstanceRollingConfigDAO;
    }

    @Override
    public List<IpDTO> getRollingServers(StepInstanceBaseDTO stepInstance) {
        long rollingConfigId = stepInstance.getRollingConfigId();
        long stepInstanceId = stepInstance.getId();
        int batch = stepInstance.getBatch();

        TaskInstanceRollingConfigDTO rollingConfig =
            taskInstanceRollingConfigDAO.queryRollingConfigById(rollingConfigId);
        if (rollingConfig.isBatchRollingStep(stepInstanceId)) {
            return rollingConfig.getConfig().getServerBatchList().get(batch - 1).getServers();
        } else {
            return stepInstance.getTargetServers().getIpList();
        }
    }

    @Override
    public List<IpDTO> getRollingServers(long stepInstanceId, int batch) {
        return null;
    }

    @Override
    public long saveRollingConfigForFastJob(FastTaskDTO fastTask) {
        StepInstanceDTO stepInstance = fastTask.getStepInstance();

        TaskInstanceRollingConfigDTO taskInstanceRollingConfig = new TaskInstanceRollingConfigDTO();
        taskInstanceRollingConfig.setTaskInstanceId(fastTask.getTaskInstance().getId());

        RollingConfigDTO rollingConfig = fastTask.getRollingConfig();

        String rollingConfigName = StringUtils.isBlank(rollingConfig.getName()) ? "default" : rollingConfig.getName();
        taskInstanceRollingConfig.setConfigName(rollingConfigName);

        RollingConfigDO rollingConfigDO = new RollingConfigDO();
        rollingConfigDO.setName(rollingConfigName);
        rollingConfigDO.setMode(rollingConfig.getMode());
        rollingConfigDO.setExpr(rollingConfig.getExpr());

        RollingBatchServersResolver resolver =
            new RollingBatchServersResolver(fastTask.getStepInstance().getTargetServers().getIpList(),
                rollingConfig.getExpr());
        List<RollingServerBatch> serversBatchList = resolver.resolve();
        rollingConfigDO.setServerBatchList(
            serversBatchList.stream()
                .map(rollingServerBatch ->
                    new RollingServerBatchDO(rollingServerBatch.getBatch(), rollingServerBatch.getServers()))
                .collect(Collectors.toList()));
        rollingConfigDO.setTotalBatch(rollingConfigDO.getServerBatchList().size());
        taskInstanceRollingConfig.setConfig(rollingConfigDO);

        rollingConfigDO.setIncludeStepInstanceIdList(Lists.newArrayList(stepInstance.getId()));
        rollingConfigDO.setBatchRollingStepInstanceIdList(Lists.newArrayList(stepInstance.getId()));
        return taskInstanceRollingConfigDAO.saveRollingConfig(taskInstanceRollingConfig);
    }

    @Override
    public TaskInstanceRollingConfigDTO getRollingConfig(long rollingConfigId) {
        return taskInstanceRollingConfigDAO.queryRollingConfigById(rollingConfigId);
    }
}
