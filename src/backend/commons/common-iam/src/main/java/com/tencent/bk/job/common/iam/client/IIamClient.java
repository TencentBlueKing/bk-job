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

package com.tencent.bk.job.common.iam.client;

import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;

import java.util.List;

/**
 * @since 16/6/2020 21:37
 */
public interface IIamClient {
    /**
     * 获取申请权限链接
     *
     * @param actionList 需要申请的操作信息列表
     * @return 权限链接
     */
    String getApplyUrl(List<ActionDTO> actionList);

    /**
     * 注册新资源
     *
     * @param id        资源 ID
     * @param name      资源名称
     * @param type      资源类型
     * @param creator   资源创建者
     * @param ancestors 资源的祖先
     * @return 是否注册成功
     */
    boolean registerResource(String id, String name, String type, String creator, List<ResourceDTO> ancestors);

}
