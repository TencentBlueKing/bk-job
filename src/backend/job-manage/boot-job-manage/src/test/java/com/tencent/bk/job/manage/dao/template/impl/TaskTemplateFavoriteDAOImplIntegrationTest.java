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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.common.util.RandomUtil;
import com.tencent.bk.job.manage.dao.TaskFavoriteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 9/10/2019 18:06
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/template/init_template_favorite_data.sql"})
@SqlConfig(encoding = "utf-8")
class TaskTemplateFavoriteDAOImplIntegrationTest {

    private static final List<Long> USER_C_1000_FAVORITE_LIST = new ArrayList<>();
    private static final List<Long> USER_T_1000_FAVORITE_LIST = new ArrayList<>();

    @Autowired
    @Qualifier("TaskTemplateFavoriteDAOImpl")
    private TaskFavoriteDAO taskFavoriteDAO;

    @BeforeEach
    void initTest() {
        USER_C_1000_FAVORITE_LIST.clear();
        USER_C_1000_FAVORITE_LIST.add(10000L);
        USER_C_1000_FAVORITE_LIST.add(20000L);
        USER_C_1000_FAVORITE_LIST.add(30000L);

        USER_T_1000_FAVORITE_LIST.clear();
        USER_T_1000_FAVORITE_LIST.add(10000L);
        USER_T_1000_FAVORITE_LIST.add(20000L);
    }

    @Test
    void givenNormalAppIdAndUsernameReturnTemplateIdList() {
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userC"))
            .isEqualTo(USER_C_1000_FAVORITE_LIST);
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userT"))
            .isEqualTo(USER_T_1000_FAVORITE_LIST);
    }

    @Test
    void givenNotExistAppIdOrUsernameReturnEmptyList() {
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(99999L, "userC")).isEmpty();
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "not_exist")).isEmpty();
    }

    @Test
    void givenNormalAppIdUsernameAndTemplateIdReturnNewFavoriteId() {
        Long newTemplateId = RandomUtil.getRandomPositiveLong();
        USER_C_1000_FAVORITE_LIST.add(newTemplateId);
        USER_C_1000_FAVORITE_LIST.sort(Long::compareTo);
        assertThat(taskFavoriteDAO.insertFavorite(1000L, "userC", newTemplateId)).isTrue();
        List<Long> result = taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userC");
        result.sort(Long::compareTo);
        assertThat(result).isEqualTo(USER_C_1000_FAVORITE_LIST);
    }

    @Test
    void givenNormalAppIdUsernameAndTemplateIdReturnDeleteSuccess() {
        Long deleteTemplateId = USER_C_1000_FAVORITE_LIST.remove(1);
        assertThat(taskFavoriteDAO.deleteFavorite(1000L, "userC", deleteTemplateId)).isTrue();
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userC"))
            .isEqualTo(USER_C_1000_FAVORITE_LIST);

        assertThat(taskFavoriteDAO.insertFavorite(1000L, "userC", deleteTemplateId)).isTrue();
        USER_C_1000_FAVORITE_LIST.add(deleteTemplateId);
        USER_C_1000_FAVORITE_LIST.sort(Long::compareTo);
        List<Long> result = taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userC");
        result.sort(Long::compareTo);
        assertThat(result).isEqualTo(USER_C_1000_FAVORITE_LIST);
    }

    @Test
    void givenNotExistAppIdOrUsernameOrTemplateIdReturnDeleteFailed() {
        assertThat(taskFavoriteDAO.deleteFavorite(1000L, "userC", RandomUtil.getRandomPositiveLong())).isFalse();
        assertThat(taskFavoriteDAO.deleteFavorite(9999999L, "userC", USER_C_1000_FAVORITE_LIST.get(1))).isFalse();
        assertThat(
            taskFavoriteDAO.deleteFavorite(1000L, UUID.randomUUID().toString(), USER_C_1000_FAVORITE_LIST.get(1)))
            .isFalse();
        assertThat(taskFavoriteDAO.listFavoriteParentIdByUser(1000L, "userC"))
            .isEqualTo(USER_C_1000_FAVORITE_LIST);
    }

}
