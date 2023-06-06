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
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

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
        DangerousRuleDTO dangerousRule = new DangerousRuleDTO();
        dangerousRule.setScriptType(1);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(8);
        dangerousRule.setScriptType(2);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(5);
        dangerousRule.setScriptType(3);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(4);
        dangerousRule.setScriptType(4);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(5);
        dangerousRule.setScriptType(5);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(2);
        dangerousRule.setScriptType(6);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(3);
        dangerousRule.setScriptType(7);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(0);
        dangerousRule.setScriptType(8);
        assertThat(dangerousRuleDAO.listDangerousRules(dslContext, dangerousRule)).hasSize(0);
    }

    @Test
    @DisplayName("测试高危语句规则条件查询")
    void listDangerousRulesByQuery() {
        // 按sriptType模糊查询，略，因H2数据库不支持msyql原生的位运算

        // 语法检测表达式模糊查询
        DangerousRuleQuery query = DangerousRuleQuery
            .builder()
            .expression("sql")
            .build();
        List<DangerousRuleDTO> dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(3);
        query.setExpression("shell");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(8);
        query.setExpression("per");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(4);
        query.setExpression("l");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(9);
        query.setExpression("python");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(5);
        query.setExpression("java");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(0);

        // 规则说明模式查询
        query.setExpression(null);
        query.setDescription("Bat");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(5);
        query.setDescription("test");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(0);
        query.setDescription("pow");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(0);
        query.setDescription(",SQL-6");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(2);
        query.setDescription("Shell-1,Python-4");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(1);

        // 拦截动作查询
        query.setDescription(null);
        query.setAction(Arrays.asList(new Byte[]{1}));
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(7);
        query.setAction(Arrays.asList(new Byte[]{0}));
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(3);
        query.setAction(Arrays.asList(new Byte[]{0,1}));
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(10);

        // 多添加查询
        query.setAction(Arrays.asList(new Byte[]{0}));
        query.setExpression("shell");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(2);
        query.setAction(Arrays.asList(new Byte[]{0}));
        query.setExpression("sql");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(0);
        query.setAction(Arrays.asList(new Byte[]{1}));
        query.setExpression("sql");
        query.setDescription("SQL");
        dangerousRuleDTOS = dangerousRuleDAO.listDangerousRules(dslContext, query);
        assertThat(dangerousRuleDTOS).hasSize(3);
    }
}
