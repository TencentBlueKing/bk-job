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

package com.tencent.bk.job.manage.dao.globalsetting.impl;

import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 8/12/2020 11:56
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@Sql(value = {"/init_dangerous_rule_data.sql"})
@SqlConfig(encoding = "utf-8")
class DangerousRuleDAOImplIntegrationTest {

    @Autowired
    private DangerousRuleDAO dangerousRuleDAO;

    @Autowired
    private DSLContext dslContext;

    @Test
    void listDangerousRulesByScriptType() {
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 1)).hasSize(5);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 2)).hasSize(4);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 3)).hasSize(3);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 4)).hasSize(2);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 5)).hasSize(1);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 6)).hasSize(0);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 7)).hasSize(0);
        assertThat(dangerousRuleDAO.listDangerousRulesByScriptType(dslContext, 8)).hasSize(0);
    }
}
