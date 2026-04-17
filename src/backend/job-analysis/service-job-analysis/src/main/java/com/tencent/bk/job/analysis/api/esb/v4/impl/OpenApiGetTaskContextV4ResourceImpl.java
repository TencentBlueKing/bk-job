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

package com.tencent.bk.job.analysis.api.esb.v4.impl;

import com.tencent.bk.job.analysis.api.esb.v4.OpenApiGetTaskContextV4Resource;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4GetFileTaskContextRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4GetScriptTaskContextRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.TaskContextField;
import com.tencent.bk.job.analysis.model.esb.v4.resp.TaskContextForSingleExecuteObjectDTO;
import com.tencent.bk.job.analysis.service.ai.context.TaskContextService;
import com.tencent.bk.job.analysis.service.ai.context.model.FileTaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.ScriptTaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContext;
import com.tencent.bk.job.analysis.service.ai.context.model.TaskContextQuery;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.logsvr.api.ServiceLogResource;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.logsvr.util.LogFieldUtil;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class OpenApiGetTaskContextV4ResourceImpl implements OpenApiGetTaskContextV4Resource {

    private final AppScopeMappingService appScopeMappingService;
    private final TaskContextService taskContextService;
    private final ServiceLogResource logResource;
    private final MessageI18nService messageI18nService;

    public OpenApiGetTaskContextV4ResourceImpl(AppScopeMappingService appScopeMappingService,
                                               TaskContextService taskContextService,
                                               ServiceLogResource logResource,
                                               MessageI18nService messageI18nService) {
        this.appScopeMappingService = appScopeMappingService;
        this.taskContextService = taskContextService;
        this.logResource = logResource;
        this.messageI18nService = messageI18nService;
    }

    @Override
    public EsbV4Response<TaskContextForSingleExecuteObjectDTO> getScriptTaskContext(
        String username,
        String appCode,
        V4GetScriptTaskContextRequest request
    ) {
        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        TaskContextQuery contextQuery = TaskContextQuery.builder()
            .appId(appId)
            .taskInstanceId(request.getTaskInstanceId())
            .stepInstanceId(request.getStepInstanceId())
            .executeCount(request.getExecuteCount())
            .batch(request.getBatch())
            .executeObjectType(request.getExecuteObjectType())
            .executeObjectResourceId(request.getExecuteObjectResourceId())
            .build();
        TaskContext taskContext = taskContextService.getTaskContext(username, contextQuery);
        ScriptTaskContext scriptTaskContext = taskContext.getScriptTaskContext();

        List<TaskContextField> fieldList = new ArrayList<>();
        fieldList.add(new TaskContextField(
            "scriptType",
            ScriptTypeEnum.getName(scriptTaskContext.getScriptType()),
            "脚本类型"
        ));
        fieldList.add(new TaskContextField(
            "scriptContent",
            scriptTaskContext.getScriptContent(),
            "脚本内容"
        ));
        fieldList.add(new TaskContextField(
            "secureParam",
            String.valueOf(scriptTaskContext.isSecureParam()),
            "脚本参数是否为敏感参数"
        ));
        fieldList.add(new TaskContextField(
            "scriptParams",
            scriptTaskContext.getInsensitiveScriptParamsStr(),
            "脚本参数"
        ));

        String errorLog = resolveScriptErrorLog(request.getContent(), taskContext, contextQuery);
        fieldList.add(new TaskContextField(
            "errorLog",
            errorLog,
            "报错信息"
        ));

        TaskContextForSingleExecuteObjectDTO context = new TaskContextForSingleExecuteObjectDTO();
        context.setFieldList(fieldList);
        return EsbV4Response.success(context);
    }

    /**
     * 返回给调用方的日志最大字符数，取末尾部分
     */
    private static final int MAX_ERROR_LOG_CHARS = 50_000;

    /**
     * 获取脚本报错信息：优先使用调用方传入的 content，为空时从日志服务查询
     */
    private String resolveScriptErrorLog(String content,
                                         TaskContext taskContext,
                                         TaskContextQuery contextQuery) {
        if (StringUtils.isNotBlank(content)) {
            return LogUtil.tailLog(content, MAX_ERROR_LOG_CHARS);
        }
        try {
            String jobCreateDate = LogFieldUtil.buildJobCreateDate(taskContext.getStepCreateTime());
            String executeObjectId = contextQuery.getExecuteObjectId();
            InternalResponse<ServiceExecuteObjectLogDTO> resp = logResource.getScriptLogByExecuteObjectId(
                jobCreateDate,
                contextQuery.getStepInstanceId(),
                contextQuery.getExecuteCount(),
                executeObjectId,
                contextQuery.getBatch()
            );
            if (resp.isSuccess() && resp.getData() != null) {
                ServiceExecuteObjectScriptLogDTO scriptLog = resp.getData().getScriptLog();
                if (scriptLog != null && StringUtils.isNotBlank(scriptLog.getContent())) {
                    return LogUtil.tailLog(scriptLog.getContent(), MAX_ERROR_LOG_CHARS);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch script log for stepInstanceId={}, executeCount={}",
                contextQuery.getStepInstanceId(), contextQuery.getExecuteCount(), e);
        }
        return "";
    }

    @Override
    public EsbV4Response<TaskContextForSingleExecuteObjectDTO> getFileTaskContext(
        String username,
        String appCode,
        V4GetFileTaskContextRequest request
    ) {
        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        TaskContextQuery contextQuery = TaskContextQuery.builder()
            .appId(appId)
            .taskInstanceId(request.getTaskInstanceId())
            .stepInstanceId(request.getStepInstanceId())
            .executeCount(request.getExecuteCount())
            .batch(request.getBatch())
            .executeObjectType(request.getExecuteObjectType())
            .executeObjectResourceId(request.getExecuteObjectResourceId())
            .mode(request.getMode())
            .build();
        TaskContext taskContext = taskContextService.getTaskContext(username, contextQuery);
        FileTaskContext fileTaskContext = taskContext.getFileTaskContext();

        List<TaskContextField> fieldList = new ArrayList<>();
        fieldList.add(new TaskContextField(
            "errorSource",
            messageI18nService.getI18n(fileTaskContext.getFileTaskErrorSourceI18nKey()),
            "文件任务错误根源"
        ));
        fieldList.add(new TaskContextField(
            "uploadFileErrorData",
            fileTaskContext.getUploadFileErrorData(),
            "源文件上传失败的机器与报错信息"
        ));
        fieldList.add(new TaskContextField(
            "downloadFileErrorData",
            fileTaskContext.getDownloadFileErrorData(),
            "目标机器下载失败的机器与报错信息"
        ));

        TaskContextForSingleExecuteObjectDTO context = new TaskContextForSingleExecuteObjectDTO();
        context.setFieldList(fieldList);
        return EsbV4Response.success(context);
    }
}
