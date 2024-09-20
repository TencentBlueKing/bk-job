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

package com.tencent.bk.job.manage.validation.provider;

import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileSourceInfoVO;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * TaskFileSourceInfoVO联合校验
 */
public class TaskFileSourceVOGroupSequenceProvider implements DefaultGroupSequenceProvider<TaskFileSourceInfoVO> {

    @Override
    public List<Class<?>> getValidationGroups(TaskFileSourceInfoVO fileSource) {
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(TaskFileSourceInfoVO.class);
        if (fileSource != null) {
            Integer fileType = fileSource.getFileType();
            if (fileType != null && TaskFileTypeEnum.SERVER.getType() == fileType) {
                defaultGroupSequence.add(ValidationGroups.FileSource.ServerFile.class);
            } else if (fileType != null && TaskFileTypeEnum.FILE_SOURCE.getType() == fileType) {
                defaultGroupSequence.add(ValidationGroups.FileSource.FileSourceFile.class);
                defaultGroupSequence.add(ValidationGroups.FileSource.FileSourceId.class);
            } else if (fileType != null && TaskFileTypeEnum.LOCAL.getType() == fileType) {
                defaultGroupSequence.add(ValidationGroups.FileSource.LocalFile.class);
            }
        }
        return defaultGroupSequence;
    }
}
