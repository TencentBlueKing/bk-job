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

import com.tencent.bk.job.execute.dao.GseTaskDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskSimpleDTO;
import com.tencent.bk.job.execute.service.GseTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GseTaskServiceImpl implements GseTaskService {

    private final GseTaskDAO gseTaskDAO;
    private final IdGen idGen;

    @Autowired
    public GseTaskServiceImpl(GseTaskDAO gseTaskDAO, IdGen idGen) {
        this.gseTaskDAO = gseTaskDAO;
        this.idGen = idGen;
    }

    @Override
    public Long saveGseTask(GseTaskDTO gseTask) {
        gseTask.setId(idGen.genGseTaskId());
        return gseTaskDAO.saveGseTask(gseTask);
    }

    @Override
    public boolean updateGseTask(GseTaskDTO gseTask) {
        return gseTaskDAO.updateGseTask(gseTask);
    }

    @Override
    public GseTaskDTO getGseTask(Long taskInstanceId, long stepInstanceId, int executeCount, Integer batch) {
        return gseTaskDAO.getGseTask(taskInstanceId, stepInstanceId, executeCount, batch);
    }

    @Override
    public GseTaskDTO getGseTask(Long taskInstanceId, long gseTaskId) {
        return gseTaskDAO.getGseTask(taskInstanceId, gseTaskId);
    }

    @Override
    public GseTaskSimpleDTO getGseTaskSimpleInfo(String gseTaskId) {
        return gseTaskDAO.getGseTaskSimpleInfo(gseTaskId);
    }

    @Override
    public List<GseTaskSimpleDTO> listGseTaskSimpleInfo(Long stepInstanceId,
                                                        Integer executeCount,
                                                        Integer batch) {
        return gseTaskDAO.ListGseTaskSimpleInfo(stepInstanceId, executeCount, batch);
    }
}
