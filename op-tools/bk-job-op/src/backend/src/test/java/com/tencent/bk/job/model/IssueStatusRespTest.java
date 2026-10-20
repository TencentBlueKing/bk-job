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

package com.tencent.bk.job.model;

import com.tencent.bk.job.utils.json.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * updateTime 字段的序列化契约。API调用方迁移路径期间，响应体字段名与类型不得变化。
 */
class IssueStatusRespTest {

    @Test
    @DisplayName("存量数据（无updateTime字段）能正常反序列化，updateTime为null")
    void shouldDeserializeLegacyJsonWithoutUpdateTime() {
        String legacyJson = "{\"id\":\"issue_4523\",\"status\":\"issue_todo\",\"statusDescription\":\"待处理\"}";

        IssueStatusResp resp = JsonUtils.fromJson(legacyJson, IssueStatusResp.class);

        assertThat(resp.getId()).isEqualTo("issue_4523");
        assertThat(resp.getStatus()).isEqualTo("issue_todo");
        assertThat(resp.getStatusDescription()).isEqualTo("待处理");
        assertThat(resp.getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("回滚场景：含updateTime的数据被不认识该字段的旧代码读取时不应报错")
    void shouldIgnoreUnknownPropertyOnDeserialization() {
        String jsonWithExtraField =
            "{\"id\":\"issue_4523\",\"status\":\"issue_todo\",\"statusDescription\":\"待处理\","
                + "\"updateTime\":1758542400000,\"someFutureField\":\"x\"}";

        assertThatCode(() -> JsonUtils.fromJson(jsonWithExtraField, IssueStatusResp.class))
            .doesNotThrowAnyException();
        assertThat(JsonUtils.fromJson(jsonWithExtraField, IssueStatusResp.class).getUpdateTime())
            .isEqualTo(1758542400000L);
    }

    @Test
    @DisplayName("updateTime为null时序列化输出显式null，前端据此展示为--")
    void shouldSerializeNullUpdateTimeExplicitly() {
        IssueStatusResp resp = new IssueStatusResp("issue_4523", "issue_todo", "待处理", null);

        String json = JsonUtils.toJson(resp);

        assertThat(json).contains("\"updateTime\":null");
        assertThat(JsonUtils.fromJson(json, IssueStatusResp.class).getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("updateTime有值时序列化为毫秒时间戳数字")
    void shouldSerializeUpdateTimeAsEpochMillis() {
        IssueStatusResp resp = new IssueStatusResp("issue_4523", "issue_done", "已完成", 1758542400000L);

        String json = JsonUtils.toJson(resp);

        assertThat(json).contains("\"updateTime\":1758542400000");
        assertThat(JsonUtils.fromJson(json, IssueStatusResp.class)).isEqualTo(resp);
    }
}
