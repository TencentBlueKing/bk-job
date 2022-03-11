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

import com.tencent.bk.gse.taskapi.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * GSE脚本下发请求参数构造器
 */
public class ScriptRequestBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptRequestBuilder.class);
    private api_script_request scriptRqeuest;

    public ScriptRequestBuilder() {
        scriptRqeuest = new api_script_request();
    }

    /**
     * 添加脚本文件
     *
     * @param downloadPath
     * @param scriptFileName
     * @param scriptContent
     * @return
     */
    public ScriptRequestBuilder addScriptFile(String downloadPath, String scriptFileName, String scriptContent) {
        api_script_file scriptFile = new api_script_file();
        scriptFile.setDownload_path(downloadPath);
        scriptFile.setMd5("");
        scriptFile.setName(scriptFileName);
        scriptFile.setContent(scriptContent);
        scriptRqeuest.addToScripts(scriptFile);
        return this;
    }

    /**
     * 新增脚本任务
     *
     * @param agentList
     * @param downloadPath
     * @param scriptFileName
     * @param scriptParam
     * @param timeout
     * @return
     */
    public ScriptRequestBuilder addScriptTask(List<api_agent> agentList, String downloadPath, String scriptFileName,
                                              String scriptParam, int timeout) {
        api_task_request taskRequest = scriptRqeuest.getTasks();
        if (taskRequest == null) {
            taskRequest = new api_task_request();
            scriptRqeuest.setTasks(taskRequest);
        }

        taskRequest.setAtomic_task_num(taskRequest.getAtomic_task_num() + 1);
        taskRequest.setCrond("");

        api_auto_task autoTask = new api_auto_task();
        int atomicTaskId = taskRequest.getAtomic_task_num() - 1;
        if (atomicTaskId > 0) {
            //为了保证任务串行执行，需要设置relation参数
            List<Byte> dependencyTaskIdList = new ArrayList<>();
            int dependencyTaskId = 0;
            while (dependencyTaskId < atomicTaskId) {
                dependencyTaskIdList.add((byte) dependencyTaskId);
                dependencyTaskId++;
            }
            api_task_relation apiTaskRelation = new api_task_relation((byte) atomicTaskId, dependencyTaskIdList);
            taskRequest.addToRel_list(apiTaskRelation);
        } else {
            taskRequest.setRel_list(new ArrayList<>(0));
        }

        autoTask.setAtomic_task_id((byte) (taskRequest.getAtomic_task_num() - 1));
        String exeCmd = downloadPath + "/" + scriptFileName;
        if (StringUtils.isNotBlank(scriptParam)) {
            exeCmd += " " + scriptParam;
        }
        autoTask.setCmd(exeCmd);
        autoTask.setTimeout(timeout);
        taskRequest.addToAtomic_tasks(autoTask);
        taskRequest.setAgent_list(agentList);
        taskRequest.setVersion("1.0");
        return this;
    }

    public api_script_request build() {
        return scriptRqeuest;
    }
}
