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

package com.tencent.bk.job.file_gateway.auth;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;

import java.util.List;

/**
 * 文件源相关操作鉴权接口
 */
public interface FileSourceAuthService {
    /**
     * 资源范围下创建文件源鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @return 鉴权结果
     */
    AuthResult authCreateFileSource(String username, AppResourceScope appResourceScope);

    /**
     * 资源范围下查看文件源鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param fileSourceId     文件源ID
     * @param fileSourceName   文件源名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authViewFileSource(String username,
                                  AppResourceScope appResourceScope,
                                  Integer fileSourceId,
                                  String fileSourceName);

    /**
     * 资源范围下管理文件源鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param fileSourceId     文件源ID
     * @param fileSourceName   文件源名称，如果传入为空，则会调用ResourceNameQueryService查询
     * @return 鉴权结果
     */
    AuthResult authManageFileSource(String username,
                                    AppResourceScope appResourceScope,
                                    Integer fileSourceId,
                                    String fileSourceName);

    /**
     * 资源范围下查看文件源批量鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param fileSourceIdList 文件源ID列表
     * @return 有权限的文件源ID
     */
    List<Integer> batchAuthViewFileSource(String username,
                                          AppResourceScope appResourceScope,
                                          List<Integer> fileSourceIdList);

    /**
     * 资源范围下管理文件源批量鉴权
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param fileSourceIdList 文件源ID列表
     * @return 有权限的文件源ID
     */
    List<Integer> batchAuthManageFileSource(String username,
                                            AppResourceScope appResourceScope,
                                            List<Integer> fileSourceIdList);

    /**
     * 注册文件源实例
     *
     * @param creator 资源实例创建者
     * @param id      资源实例 ID
     * @param name    资源实例名称
     * @return 是否注册成功
     */
    boolean registerFileSource(String creator, Integer id, String name);
}
