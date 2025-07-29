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

package com.tencent.bk.job.execute.engine.executor;

import com.google.common.collect.Maps;
import com.tencent.bk.job.common.gse.util.ScriptRequestBuilder;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
public class SQLScriptGseTaskStartCommand extends ScriptGseTaskStartCommand {
    private static final Map<Integer, String> sqlMap = Maps.newHashMap();

    static {
        try {
            log.info("Init sql template!");
            try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "sqltask/mysql_exec_template.sh")) {
                StringWriter stringWriter = new StringWriter();
                if (stream != null) {
                    IOUtils.copy(stream, stringWriter, StandardCharsets.UTF_8);
                    sqlMap.put(AccountTypeEnum.MYSQL.getType(), stringWriter.toString());
                    log.info("mysql stream shell:" + sqlMap.get(AccountTypeEnum.MYSQL.getType()));
                } else {
                    log.error("File mysql_exec_template.sh is not exist!");
                    throw new RuntimeException("Init sql task shell failed");
                }
            }
            try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("sqltask/oracle_exec_template.sh")) {
                StringWriter stringWriter = new StringWriter();
                if (stream != null) {
                    IOUtils.copy(stream, stringWriter, StandardCharsets.UTF_8);
                    sqlMap.put(AccountTypeEnum.ORACLE.getType(), stringWriter.toString());
                    log.info("oracle stream shell:" + sqlMap.get(AccountTypeEnum.ORACLE.getType()));
                } else {
                    log.error("File oracle_exec_template.sh is not exist!");
                    throw new RuntimeException("Init sql task shell failed");
                }
            }
            try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("sqltask/db2_exec_template.sh")) {
                StringWriter stringWriter = new StringWriter();
                if (stream != null) {
                    IOUtils.copy(stream, stringWriter, StandardCharsets.UTF_8);
                    sqlMap.put(AccountTypeEnum.DB2.getType(), stringWriter.toString());
                    log.info("db2 stream shell:" + sqlMap.get(AccountTypeEnum.DB2.getType()));
                } else {
                    log.error("File db2_exec_template.sh is not exist!");
                    throw new RuntimeException("Init sql task shell failed");
                }
            }
        } catch (Throwable e) {
            log.error("Can not find any sql task shell in sqltask directory!", e);
            throw new RuntimeException(e);
        }
    }

    public SQLScriptGseTaskStartCommand(EngineDependentServiceHolder engineDependentServiceHolder,
                                        ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                        JobExecuteConfig jobExecuteConfig,
                                        String requestId,
                                        TaskInstanceDTO taskInstance,
                                        StepInstanceDTO stepInstance,
                                        GseTaskDTO gseTask,
                                        CustomPasswordCache customPasswordCache) {
        super(
            engineDependentServiceHolder,
            scriptExecuteObjectTaskService,
            jobExecuteConfig,
            requestId,
            taskInstance,
            stepInstance,
            gseTask,
                customPasswordCache);
    }

    @Override
    protected ExecuteScriptRequest buildScriptRequest() {
        String sqlScriptContent = stepInstance.getScriptContent();
        String sqlScriptFileName = buildScriptFileName(stepInstance);

        String publicScriptContent = sqlMap.get(stepInstance.getDbType());
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());
        String publicScriptName = this.scriptFileNamePrefix + ScriptTypeEnum.SHELL.getExt();

        ScriptRequestBuilder builder = new ScriptRequestBuilder();
        builder.addScriptFile(scriptFilePath, publicScriptName, publicScriptContent);
        builder.addScriptFile(scriptFilePath, sqlScriptFileName, sqlScriptContent);

        List<Agent> agentList = buildTargetAgents();

        builder.addScriptTask(agentList, scriptFilePath, publicScriptName, buildRunSqlShellParams(sqlScriptFileName),
            timeout);

        return builder.build();
    }

    private String buildRunSqlShellParams(String sqlScriptFileName) {
        StringBuilder sb = new StringBuilder(255);
        sb.append(stepInstance.getDbPort());
        if (StringUtils.isNotBlank(stepInstance.getDbAccount()) && !StringUtils.equals("null",
            stepInstance.getDbAccount())) {
            sb.append(" ").append(stepInstance.getDbAccount());
        } else {
            sb.append(" EMPTY");
        }
        if (StringUtils.isNotBlank(stepInstance.getDbPass()) && !StringUtils.equals("null", stepInstance.getDbPass())) {
            sb.append(" ").append(stepInstance.getDbPass());
        } else {
            sb.append(" EMPTY");
        }
        return scriptFilePath + "/" + sqlScriptFileName + " " + sb.toString();
    }

}
