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

package com.tencent.bk.job.manage.service.token;

import com.tencent.bk.job.manage.model.dto.PersonalAccessTokenDTO;

/**
 * 个人访问凭证生成策略。根据当前登录方式（自定义登录 bk_ticket / 标准登录 bk_token）装配不同实现。
 */
public interface PersonalAccessTokenProvider {

    /**
     * 使用公共应用凭证 + 当前用户登录态，向蓝鲸换取用户个人 access_token。
     *
     * @param username 当前登录用户名（网关注入）
     * @param bkTicket 自定义登录票据（内部版 auth_api 使用），可能为空
     * @param bkToken  标准登录票据（社区版 bkssm 使用），可能为空
     * @return 归一化后的个人访问凭证
     */
    PersonalAccessTokenDTO generate(String username, String bkTicket, String bkToken);
}
