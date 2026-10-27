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

package com.tencent.bk.job.manage.model.esb.v4.req.validator;

import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileSourceReq;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 按源文件类型启用校验：服务器文件要求执行目标与账号，文件源文件要求文件源 ID。
 */
public class V4JobTemplateFileSourceReqGroupSequenceProvider
    implements DefaultGroupSequenceProvider<V4JobTemplateFileSourceReq> {

    @Override
    public List<Class<?>> getValidationGroups(V4JobTemplateFileSourceReq fileSource) {
        List<Class<?>> groups = new ArrayList<>();
        groups.add(V4JobTemplateFileSourceReq.class);
        if (fileSource == null || !TaskFileTypeEnum.isValid(fileSource.getFileType())) {
            return groups;
        }
        switch (TaskFileTypeEnum.valueOf(fileSource.getFileType())) {
            case SERVER:
                groups.add(V4JobTemplateValidationGroups.FileType.Server.class);
                break;
            case FILE_SOURCE:
                groups.add(V4JobTemplateValidationGroups.FileType.FileSource.class);
                break;
            default:
                break;
        }
        return groups;
    }
}
