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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.manage.dao.TaskFavoriteDAO;
import com.tencent.bk.job.manage.dao.template.impl.TaskTemplateFavoriteDAOImpl;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 17/10/2019 10:41
 */
class TaskTemplateFavoriteServiceImplTest {

    private static TaskFavoriteDAO taskFavoriteDAO;

    private static TaskFavoriteService taskFavoriteService;

    @BeforeAll
    static void init() {
        taskFavoriteDAO = mock(TaskTemplateFavoriteDAOImpl.class);
        taskFavoriteService = new TaskTemplateFavoriteServiceImpl(taskFavoriteDAO);
    }

    @Test
    void giveUsernameAppIdReturnFavoriteList() {
        long appId = RandomUtil.getRandomPositiveLong();
        String username = UUID.randomUUID().toString();
        taskFavoriteService.listFavorites(appId, username);
        verify(taskFavoriteDAO).listFavoriteParentIdByUser(appId, username);
    }

    @Test
    void giveUsernameAppIdTemplateIdReturnAddSuccess() {
        long appId = RandomUtil.getRandomPositiveLong();
        String username = UUID.randomUUID().toString();
        long templateId = RandomUtil.getRandomPositiveLong();
        taskFavoriteService.addFavorite(appId, username, templateId);
        verify(taskFavoriteDAO).insertFavorite(appId, username, templateId);
    }

    @Test
    void giveUsernameAppIdTemplateIdReturnDeleteSuccess() {
        long appId = RandomUtil.getRandomPositiveLong();
        String username = UUID.randomUUID().toString();
        long templateId = RandomUtil.getRandomPositiveLong();
        taskFavoriteService.deleteFavorite(appId, username, templateId);
        verify(taskFavoriteDAO).deleteFavorite(appId, username, templateId);
    }
}
