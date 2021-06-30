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

import com.tencent.bk.job.manage.model.dto.ScriptSyncTemplateStepDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_script_relate_task_template_data.sql"})
@DisplayName("脚本关联的作业模板DAO集成测试")
public class ScriptRelateTemplateDAOImplIntegrationTest {

    @Autowired
    private ScriptRelateJobTemplateDAO scriptRelateJobTemplateDAO;

    @Test
    public void testListScriptRelatedJobTemplateSteps() {
        List<ScriptSyncTemplateStepDTO> relatedTemplateSteps =
            scriptRelateJobTemplateDAO.listScriptRelatedJobTemplateSteps("scriptid1");
        assertThat(relatedTemplateSteps).hasSize(2);
        ScriptSyncTemplateStepDTO step1 = relatedTemplateSteps.get(0);
        assertThat(step1.getScriptId()).isEqualTo("scriptid1");
        assertThat(step1.getScriptVersionId()).isEqualTo(2L);
        assertThat(step1.getTemplateId()).isEqualTo(2L);
        assertThat(step1.getStepId()).isEqualTo(2L);
        assertThat(step1.getTemplateName()).isEqualTo("job_template_2");
        assertThat(step1.getStepName()).isEqualTo("job_template_2_step_1");
        assertThat(step1.getAppId()).isEqualTo(2L);

        ScriptSyncTemplateStepDTO step2 = relatedTemplateSteps.get(1);
        assertThat(step2.getScriptId()).isEqualTo("scriptid1");
        assertThat(step2.getScriptVersionId()).isEqualTo(1L);
        assertThat(step2.getTemplateId()).isEqualTo(1L);
        assertThat(step2.getStepId()).isEqualTo(1L);
        assertThat(step2.getTemplateName()).isEqualTo("job_template_1");
        assertThat(step2.getStepName()).isEqualTo("job_template_1_step_1");
        assertThat(step2.getAppId()).isEqualTo(2L);
    }

}
