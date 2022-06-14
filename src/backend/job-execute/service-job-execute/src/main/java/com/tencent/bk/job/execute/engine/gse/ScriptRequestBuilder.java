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

package com.tencent.bk.job.execute.engine.gse;

import com.tencent.bk.job.execute.engine.gse.v2.model.Agent;
import com.tencent.bk.job.execute.engine.gse.v2.model.AtomicScriptTask;
import com.tencent.bk.job.execute.engine.gse.v2.model.AtomicScriptTaskRelation;
import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.execute.engine.gse.v2.model.GseScript;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE脚本下发请求参数构造器
 */
public class ScriptRequestBuilder {
    private final ExecuteScriptRequest request;

    public ScriptRequestBuilder() {
        request = new ExecuteScriptRequest();
    }

    /**
     * 添加脚本文件
     *
     * @param storeDir       脚本文件存储目录
     * @param scriptFileName 脚本文件名称
     * @param scriptContent  脚本内容
     */
    public ScriptRequestBuilder addScriptFile(String storeDir, String scriptFileName, String scriptContent) {
        GseScript gseScript = new GseScript();
        gseScript.setStoreDir(storeDir);
        gseScript.setName(scriptFileName);
        gseScript.setContent(scriptContent);
        request.addScript(gseScript);
        return this;
    }

    /**
     * 新增脚本任务
     *
     * @param agents         目标agent列表
     * @param storeDir       脚本文件存储目录
     * @param scriptFileName 脚本文件名称
     * @param scriptParam    脚本执行参数
     * @param timeout        超时时间
     */
    public ScriptRequestBuilder addScriptTask(List<Agent> agents, String storeDir, String scriptFileName,
                                              String scriptParam, int timeout) {
        AtomicScriptTask atomicScriptTask = new AtomicScriptTask();
        atomicScriptTask.setTimeout(timeout);

        int atomicTaskId = request.getAtomicTasks().size();
        atomicScriptTask.setTaskId(atomicTaskId);

        String exeCmd = storeDir + "/" + scriptFileName;
        if (StringUtils.isNotBlank(scriptParam)) {
            exeCmd += " " + scriptParam;
        }
        atomicScriptTask.setCommand(exeCmd);

        request.addAtomicScriptTask(atomicScriptTask);

        //为了保证任务串行执行，需要设置relation参数
        if (atomicTaskId > 0) {
            AtomicScriptTaskRelation relation = new AtomicScriptTaskRelation();
            relation.setTaskId(atomicTaskId);

            List<Integer> dependencyTaskIdList = new ArrayList<>();
            int dependencyTaskId = 0;
            while (dependencyTaskId < atomicTaskId) {
                dependencyTaskIdList.add(dependencyTaskId);
                dependencyTaskId++;
            }
            relation.setIndex(dependencyTaskIdList);
            request.addAtomicTaskRelation(relation);
        }

        request.setAgents(agents);

        return this;
    }

    public ExecuteScriptRequest build() {
        return request;
    }
}
