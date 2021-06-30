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

package com.tencent.bk.job.manage.service;

import java.util.List;

/**
 * @since 16/10/2019 19:42
 */
public interface TaskFavoriteService {

    /**
     * 获取收藏列表
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @return 收藏的 ID 列表
     */
    List<Long> listFavorites(Long appId, String username);

    /**
     * 新增收藏
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @param id       要收藏的 ID
     * @return 是否成功
     */
    boolean addFavorite(Long appId, String username, Long id);

    /**
     * 删除收藏
     *
     * @param appId    业务 ID
     * @param username 用户名
     * @param id       要删除的 ID
     * @return 是否成功
     */
    boolean deleteFavorite(Long appId, String username, Long id);
}
