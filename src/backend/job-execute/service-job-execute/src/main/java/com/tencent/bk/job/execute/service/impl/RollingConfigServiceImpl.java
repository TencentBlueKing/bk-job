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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.dao.RollingConfigDAO;
import com.tencent.bk.job.execute.engine.rolling.RollingBatchServersResolver;
import com.tencent.bk.job.execute.engine.rolling.RollingServerBatch;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
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

    private final RollingConfigDAO rollingConfigDAO;

    @Autowired
    public RollingConfigServiceImpl(RollingConfigDAO rollingConfigDAO) {
        this.rollingConfigDAO = rollingConfigDAO;
    }

    @Override
    public List<HostDTO> getRollingServers(StepInstanceBaseDTO stepInstance, Integer batch) {
        long rollingConfigId = stepInstance.getRollingConfigId();
        long stepInstanceId = stepInstance.getId();

        RollingConfigDTO rollingConfig =
            rollingConfigDAO.queryRollingConfigById(rollingConfigId);
        if (rollingConfig.isBatchRollingStep(stepInstanceId)) {
            if (batch == null || batch == 0) {
                // 忽略滚动批次，返回当前步骤的所有目标服务器
                return stepInstance.getTargetServers().getIpList();
            } else {
                return rollingConfig.getConfigDetail().getServerBatchList()
                    .stream().filter(serverBatch -> serverBatch.getBatch().equals(batch))
                    .findFirst().orElseThrow(() -> new InternalException(ErrorCode.INTERNAL_ERROR)).getServers();
            }
        } else {
            return stepInstance.getTargetServers().getIpList();
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

        RollingBatchServersResolver resolver =
            new RollingBatchServersResolver(fastTask.getStepInstance().getTargetServers().getIpList(),
                rollingConfig.getExpr());
        List<RollingServerBatch> serversBatchList = resolver.resolve();
        rollingConfigDetailDO.setServerBatchList(
            serversBatchList.stream()
                .map(rollingServerBatch ->
                    new RollingServerBatchDO(rollingServerBatch.getBatch(), rollingServerBatch.getServers()))
                .collect(Collectors.toList()));
        rollingConfigDetailDO.setTotalBatch(rollingConfigDetailDO.getServerBatchList().size());
        taskInstanceRollingConfig.setConfigDetail(rollingConfigDetailDO);

        rollingConfigDetailDO.setIncludeStepInstanceIdList(Lists.newArrayList(stepInstance.getId()));
        rollingConfigDetailDO.setBatchRollingStepInstanceIdList(Lists.newArrayList(stepInstance.getId()));
        Long rollingConfigId= rollingConfigDAO.saveRollingConfig(taskInstanceRollingConfig);
        taskInstanceRollingConfig.setId(rollingConfigId);
        return taskInstanceRollingConfig;
    }

    @Override
    public RollingConfigDTO getRollingConfig(long rollingConfigId) {
        return rollingConfigDAO.queryRollingConfigById(rollingConfigId);
    }
}
