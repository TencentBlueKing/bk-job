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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.manage.dao.TaskFavoriteDAO;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * @since 16/10/2019 19:57
 */
@Slf4j
@Service("TaskTemplateFavoriteServiceImpl")
public class TaskTemplateFavoriteServiceImpl implements TaskFavoriteService {

    private TaskFavoriteDAO taskFavoriteDAO;

    @Autowired
    public TaskTemplateFavoriteServiceImpl(@Qualifier("TaskTemplateFavoriteDAOImpl") TaskFavoriteDAO taskFavoriteDAO) {
        this.taskFavoriteDAO = taskFavoriteDAO;
    }

    @Override
    public List<Long> listFavorites(Long appId, String username) {
        List<Long> favoriteTemplateList = taskFavoriteDAO.listFavoriteParentIdByUser(appId, username);
        if (CollectionUtils.isEmpty(favoriteTemplateList)) {
            log.debug("app_id|{}|username|{}|favorite_templates|empty", appId, username);
            return Collections.emptyList();
        }
        log.debug("app_id|{}|username|{}|favorite_templates|{}", appId, username, favoriteTemplateList);
        return favoriteTemplateList;
    }

    @Override
    public boolean addFavorite(Long appId, String username, Long id) {
        return taskFavoriteDAO.insertFavorite(appId, username, id);
    }

    @Override
    public boolean deleteFavorite(Long appId, String username, Long id) {
        return taskFavoriteDAO.deleteFavorite(appId, username, id);
    }
}
