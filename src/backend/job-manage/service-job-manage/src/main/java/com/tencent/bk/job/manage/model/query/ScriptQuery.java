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

package com.tencent.bk.job.manage.model.query;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 脚本查询
 */
@Getter
@Setter
@ToString
public class ScriptQuery {
    /**
     * 脚本版本ID，对应某个版本的脚本的ID
     */
    private Long scriptVersionId;
    /**
     * 脚本ID，一个脚本包含多个版本的脚本
     */
    private String id;
    /**
     * 脚本id列表
     */
    private List<String> ids;
    /**
     * 脚本名称
     */
    private String name;
    /**
     * 脚本类型
     */
    private Integer type;

    /**
     * 是否公共脚本
     */
    private Boolean publicScript;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 脚本标签ID列表
     */
    private List<Long> tagIds;
    /**
     * 是否过滤未标记的脚本
     */
    private boolean untaggedScript;

    /**
     * 脚本状态
     *
     * @see com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum
     */
    private Integer status;

}
