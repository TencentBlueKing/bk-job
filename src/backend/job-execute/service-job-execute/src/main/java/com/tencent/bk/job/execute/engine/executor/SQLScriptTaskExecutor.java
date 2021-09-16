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

package com.tencent.bk.job.execute.engine.executor;

import com.google.common.collect.Maps;
import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_script_request;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.model.RunSQLScriptFile;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL执行
 */
@Slf4j
public class SQLScriptTaskExecutor extends ScriptTaskExecutor {
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

    /**
     * SQLScriptTaskExecutor Constructor
     *
     * @param requestId                请求ID
     * @param gseTasksExceptionCounter GSE任务异常计数
     * @param taskInstance             作业实例
     * @param stepInstance             步骤实例
     * @param executeIps               目标IP
     */
    public SQLScriptTaskExecutor(String requestId, GseTasksExceptionCounter gseTasksExceptionCounter,
                                 TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, Set<String> executeIps) {
        super(requestId, gseTasksExceptionCounter, taskInstance, stepInstance, executeIps, null);
    }

    protected api_script_request getScriptRequest(StepInstanceDTO stepInstance) {
        String sqlScriptContent = stepInstance.getScriptContent();
        String fileNamePre = buildScriptFileNamePrefix(stepInstance);
        String sqlScriptFileName = fileNamePre + ScriptTypeEnum.getExtByValue(stepInstance.getScriptType());

        String publicScriptContent = sqlMap.get(stepInstance.getDbType());
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());
        String publicScriptName = fileNamePre + ScriptTypeEnum.getExtByValue(ScriptTypeEnum.SHELL.getValue());

        RunSQLScriptFile param = new RunSQLScriptFile();
        param.setTimeout(timeout);
        param.setDownloadPath(scriptFilePath);
        param.setPublicScriptContent(publicScriptContent);
        param.setPublicScriptName(publicScriptName);
        param.setSqlScriptContent(sqlScriptContent);
        param.setSqlScriptFileName(sqlScriptFileName);
        StringBuilder sqlParam = new StringBuilder(255);
        sqlParam.append(stepInstance.getDbPort());
        if (StringUtils.isNotBlank(stepInstance.getDbAccount()) && !StringUtils.equals("null",
            stepInstance.getDbAccount())) {
            sqlParam.append(" ").append(stepInstance.getDbAccount());
        } else {
            sqlParam.append(" EMPTY");
        }
        if (StringUtils.isNotBlank(stepInstance.getDbPass()) && !StringUtils.equals("null", stepInstance.getDbPass())) {
            String dbPassword;
            try {
                dbPassword = AESUtils.decryptToPlainText(Base64Util.decodeContentToByte(stepInstance.getDbPass()),
                    jobExecuteConfig.getEncryptPassword());
            } catch (Exception e) {
                log.error("Decrypt db password failed!", e);
                throw new ServiceException(ErrorCode.SERVICE_INTERNAL_ERROR);
            }
            sqlParam.append(" ").append(dbPassword);
        } else {
            sqlParam.append(" EMPTY");
        }
        param.setParamForDBInfo(sqlParam.toString());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());

        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());

        return GseRequestUtils.buildScriptRequestWithSQL(agentList, param);
    }
}
