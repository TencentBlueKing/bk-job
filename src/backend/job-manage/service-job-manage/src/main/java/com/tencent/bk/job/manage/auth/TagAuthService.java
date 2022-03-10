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

package com.tencent.bk.job.manage.auth;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 标签相关操作鉴权接口
 */
public interface TagAuthService {
    /**
     * 资源范围下创建标签鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateTag(String username, AppResourceScope appResourceScope);

    /**
     * 资源范围下管理标签鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param tagId         标签ID
     * @param tagName       标签名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authManageTag(String username,
                             AppResourceScope appResourceScope,
                             Long tagId,
                             String tagName);

    /**
     * 资源范围下管理标签批量鉴权
     *
     * @param username      用户名
     * @param appResourceScope 资源范围
     * @param tagIdList     标签ID列表
     * @return 有权限的标签ID
     */
    List<Long> batchAuthManageTag(String username,
                                  AppResourceScope appResourceScope,
                                  List<Long> tagIdList);

    /**
     * 注册标签实例
     *
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @param creator 资源实例创建者
     * @return 是否注册成功
     */
    boolean registerTag(Long id, String name, String creator);
}
