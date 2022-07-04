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

import com.tencent.bk.job.execute.config.GseTaskTableRouteConfig;
import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.dao.GseTaskLogDAO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.service.GseTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GseTaskServiceImpl implements GseTaskService {

    private final GseTaskDAO gseTaskDAO;
    private final GseTaskLogDAO gseTaskLogDAO;
    private final GseTaskTableRouteConfig gseTaskTableRouteConfig;

    @Autowired
    public GseTaskServiceImpl(GseTaskDAO gseTaskDAO, GseTaskLogDAO gseTaskLogDAO,
                              GseTaskTableRouteConfig gseTaskTableRouteConfig) {
        this.gseTaskDAO = gseTaskDAO;
        this.gseTaskLogDAO = gseTaskLogDAO;
        this.gseTaskTableRouteConfig = gseTaskTableRouteConfig;
    }

    @Override
    public Long saveGseTask(GseTaskDTO gseTask) {
        if (usingNewTable(gseTask.getStepInstanceId())) {
            return gseTaskDAO.saveGseTask(gseTask);
        } else {
            // 兼容实现，发布后删除
            gseTaskLogDAO.saveGseTaskLog(gseTask);
            return null;
        }
    }

    private boolean usingNewTable(long stepInstanceId) {
        return gseTaskTableRouteConfig.isNewTableEnabled()
            && (gseTaskTableRouteConfig.getFromStepInstanceId() == null
            || stepInstanceId > gseTaskTableRouteConfig.getFromStepInstanceId());
    }

    @Override
    public GseTaskDTO getGseTask(long stepInstanceId, int executeCount, int batch) {
        GseTaskDTO gseTask = gseTaskDAO.getGseTask(stepInstanceId, executeCount, batch);
        if (gseTask == null) {
            // 兼容历史数据
            gseTask = gseTaskLogDAO.getGseTaskLog(stepInstanceId, executeCount);
        }
        return gseTask;
    }

    @Override
    public GseTaskDTO getGseTask(long gseTaskId) {
        return gseTaskDAO.getGseTask(gseTaskId);
    }
}
