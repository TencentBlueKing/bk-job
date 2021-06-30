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

package com.tencent.bk.job.manage.dao;

import java.util.List;

/**
 * @since 3/10/2019 20:14
 */
public interface TaskFavoriteDAO {

    /**
     * 按用户列出收藏的实例 ID 列表
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 用户收藏的实例 ID 列表
     */
    List<Long> listFavoriteParentIdByUser(long appId, String username);

    /**
     * 新增收藏
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @param parentId 实例 ID
     * @return 是否收藏成功
     */
    boolean insertFavorite(long appId, String username, long parentId);

    /**
     * 取消收藏
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @param parentId 实例 ID
     * @return 是否取消成功
     */
    boolean deleteFavorite(long appId, String username, long parentId);
}
