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

package com.tencent.bk.job.manage.model.dto;

import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 引用脚本的作业执行方案DTO
 *
 * @date 2019/09/19
 */
@Getter
@Setter
@ToString
public class ScriptRelatedTaskPlanDTO {
    /**
     * 脚本ID
     */
    private String scriptId;
    /**
     * 脚本版本ID
     */
    private Long scriptVersionId;
    /**
     * 脚本版本号
     */
    private String scriptVersion;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 版本状态
     */
    private JobResourceStatusEnum scriptStatus;
    /**
     * 执行方案ID
     */
    private Long taskId;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 作业模板ID
     */
    private Long templateId;
    /**
     * 引用的执行方案名称
     */
    private String taskName;
}
