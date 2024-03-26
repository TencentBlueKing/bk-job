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

package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ServiceScriptDTO {
    /**
     * 脚本版本ID，对应某个版本的脚本的ID
     */
    private Long scriptVersionId;

    /**
     * 脚本ID，一个脚本包含多个版本的脚本
     */
    private String id;

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
    private boolean publicScript;

    /**
     * 业务ID
     */
    private Long appId;

    /**
     * 脚本内容
     */
    private String content;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 最后修改人
     */
    private String lastModifyUser;

    /**
     * 最后修改时间
     */
    private Long lastModifyTime;

    /**
     * 脚本大种类: 0 系统的执行脚本(如shell,bat,python等), 1 SQL执行脚本
     */
    private Integer category;

    /**
     * 脚本的版本号
     */
    private String version;

    /**
     * 脚本状态
     *
     * @see JobResourceStatusEnum
     */
    private Integer status;

    /**
     * 脚本版本描述
     */
    private String versionDesc;

    /**
     * 脚本描述
     */
    private String description;
}
