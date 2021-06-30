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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.dao.DangerousRecordDAO;
import com.tencent.bk.job.execute.model.DangerousRecordDTO;
import com.tencent.bk.job.execute.model.ScriptCheckItemDTO;
import com.tencent.bk.job.execute.model.ScriptCheckResultDTO;
import com.tencent.bk.job.manage.common.consts.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_dangerous_record_data.sql"})
public class DangerousRecordDAOImplIntegrationTest {
    @Autowired
    private DangerousRecordDAO dangerousRecordDAO;

    @Test
    public void testListPageDangerousRecord() {
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);
        baseSearchCondition.setCreateTimeStart(1619747000000L);
        baseSearchCondition.setCreateTimeEnd(1619838000000L);

        DangerousRecordDTO query = new DangerousRecordDTO();
        PageData<DangerousRecordDTO> pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(2);

        query = new DangerousRecordDTO();
        query.setId(1L);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getRuleId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getRuleExpression()).isEqualTo("rm -rf");
        assertThat(pageData.getData().get(0).getAppId()).isEqualTo(2L);
        assertThat(pageData.getData().get(0).getAppName()).isEqualTo("BlueKing");
        assertThat(pageData.getData().get(0).getClient()).isEqualTo("app1");
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getLine()).isEqualTo(2);
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getLevel()).isEqualTo(3);
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getDescription()).isEqualTo("rm -rf" +
            " forbidden");
        assertThat(pageData.getData().get(0).getCreateTime()).isEqualTo(1619748000000L);
        assertThat(pageData.getData().get(0).getAction()).isEqualTo(1);
        assertThat(pageData.getData().get(0).getStartupMode()).isEqualTo(2);
        assertThat(pageData.getData().get(0).getOperator()).isEqualTo("admin");
        assertThat(pageData.getData().get(0).getScriptLanguage()).isEqualTo(1);
        assertThat(pageData.getData().get(0).getScriptContent()).isEqualTo("#!/bin/bash\\nrm -rf *");
        assertThat(pageData.getData().get(0).getExtData()).containsKeys("request_param");

        query = new DangerousRecordDTO();
        query.setAction(1);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);

        query = new DangerousRecordDTO();
        query.setStartupMode(2);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);

        query = new DangerousRecordDTO();
        query.setAppId(2L);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getAppId()).isEqualTo(2L);

        query = new DangerousRecordDTO();
        query.setClient("app1");
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getClient()).isEqualTo("app1");

        query = new DangerousRecordDTO();
        query.setOperator("admin");
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getOperator()).isEqualTo("admin");

        query = new DangerousRecordDTO();
        query.setRuleExpression("rm");
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
        assertThat(pageData.getData().get(0).getRuleExpression()).isEqualTo("rm -rf");

        query = new DangerousRecordDTO();
        baseSearchCondition.setCreateTimeStart(1619747000000L);
        baseSearchCondition.setCreateTimeEnd(1619749000000L);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(1L);
    }

    @Test
    public void testSaveDangerousRecord() {
        DangerousRecordDTO record = new DangerousRecordDTO();
        record.setRuleId(100L);
        record.setRuleExpression("shutdown");
        record.setOperator("userT");
        record.setScriptLanguage(1);
        record.setScriptContent("shutdown now");
        record.setClient("app2");
        record.setStartupMode(TaskStartupModeEnum.API.getValue());
        record.setAction(RuleMatchHandleActionEnum.INTERCEPT.getValue());
        record.setCreateTime(1619831994000L);
        record.setAppId(2L);
        record.setAppName("BlueKing");
        ScriptCheckItemDTO checkItem = new ScriptCheckItemDTO(1, "shutdown now", "shutdown",
            ScriptCheckErrorLevelEnum.FATAL, "shutdown is forbidden!");
        ScriptCheckResultDTO checkResult = new ScriptCheckResultDTO(Collections.singletonList(checkItem));
        record.setCheckResult(checkResult);
        Map<String, String> extData = new HashMap<>();
        extData.put("request_id", "aaaaaaaa");
        record.setExtData(extData);

        dangerousRecordDAO.saveDangerousRecord(record);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(10);
        baseSearchCondition.setCreateTimeStart(1619747000000L);
        baseSearchCondition.setCreateTimeEnd(1619884800000L);
        DangerousRecordDTO query = new DangerousRecordDTO();
        query.setId(3L);
        PageData<DangerousRecordDTO> pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData()).hasSize(1);
        pageData = dangerousRecordDAO.listPageDangerousRecord(query, baseSearchCondition);
        assertThat(pageData.getData().get(0).getId()).isEqualTo(3L);
        assertThat(pageData.getData().get(0).getRuleId()).isEqualTo(100L);
        assertThat(pageData.getData().get(0).getRuleExpression()).isEqualTo("shutdown");
        assertThat(pageData.getData().get(0).getAppId()).isEqualTo(2L);
        assertThat(pageData.getData().get(0).getAppName()).isEqualTo("BlueKing");
        assertThat(pageData.getData().get(0).getClient()).isEqualTo("app2");
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getLine()).isEqualTo(1);
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getLineContent()).isEqualTo(
            "shutdown now");
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getMatchContent()).isEqualTo(
            "shutdown");
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getLevel()).isEqualTo(3);
        assertThat(pageData.getData().get(0).getCheckResult().getResults().get(0).getDescription()).isEqualTo(
            "shutdown is forbidden!");
        assertThat(pageData.getData().get(0).getCreateTime()).isEqualTo(1619831994000L);
        assertThat(pageData.getData().get(0).getAction()).isEqualTo(RuleMatchHandleActionEnum.INTERCEPT.getValue());
        assertThat(pageData.getData().get(0).getStartupMode()).isEqualTo(TaskStartupModeEnum.API.getValue());
        assertThat(pageData.getData().get(0).getOperator()).isEqualTo("userT");
        assertThat(pageData.getData().get(0).getScriptLanguage()).isEqualTo(1);
        assertThat(pageData.getData().get(0).getScriptContent()).isEqualTo("shutdown now");
        assertThat(pageData.getData().get(0).getExtData()).containsKeys("request_id");
    }
}
