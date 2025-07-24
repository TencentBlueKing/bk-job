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

package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbAgentInfoV3DTO;
import com.tencent.bk.job.api.v3.model.EsbQueryAgentInfoV3Resp;
import com.tencent.bk.job.api.v3.model.request.EsbQueryAgentInfoV3Req;
import io.restassured.common.mapper.TypeRef;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 获取步骤详情 API 测试
 */
@DisplayName("v3.EsbAgentInfoV3ResourceAPITest")
public class EsbAgentInfoV3ResourceAPITest extends BaseTest {

    @AfterAll
    static void tearDown() {

    }

    @Nested
    class QueryAgentInfoTest {
        @Test
        @DisplayName("测试正常查询Agent信息")
        void testQueryAgentInfo() {
            EsbQueryAgentInfoV3Req req = new EsbQueryAgentInfoV3Req();
            req.setScopeType("biz");
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setHostIdList(Arrays.asList(
                TestProps.HOST_1_DEFAULT_BIZ.getHostId(),
                TestProps.HOST_2_DEFAULT_BIZ.getHostId()
            ));
            EsbQueryAgentInfoV3Resp esbQueryAgentInfoV3Resp = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.QUERY_AGENT_INFO)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbQueryAgentInfoV3Resp>>() {
                })
                .getData();
            assertThat(esbQueryAgentInfoV3Resp).isNotNull();
            List<EsbAgentInfoV3DTO> agentInfoList = esbQueryAgentInfoV3Resp.getAgentInfoList();
            assertThat(agentInfoList).isNotNull();
            assertThat(agentInfoList).hasSize(2);
            EsbAgentInfoV3DTO esbAgentInfoV3DTO = agentInfoList.get(0);
            assertThat(esbAgentInfoV3DTO.getHostId()).isGreaterThan(0);
            assertThat(esbAgentInfoV3DTO.getStatus()).isGreaterThanOrEqualTo(0);
            assertThat(esbAgentInfoV3DTO.getVersion()).isNotBlank();
        }

        @Test
        @DisplayName("测试查询Agent信息参数长度超限1")
        void testQueryAgentInfoWithMaxHostIds() {
            EsbQueryAgentInfoV3Req req = new EsbQueryAgentInfoV3Req();
            req.setScopeType("biz");
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            List<Long> hostIdList = new ArrayList<>();
            int maxHostIdNum = 5000;
            for (int i = 0; i < maxHostIdNum; i++) {
                hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            }
            req.setHostIdList(hostIdList);
            EsbQueryAgentInfoV3Resp esbQueryAgentInfoV3Resp = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.QUERY_AGENT_INFO)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbQueryAgentInfoV3Resp>>() {
                })
                .getData();
            AssertionsForClassTypes.assertThat(esbQueryAgentInfoV3Resp).isNotNull();
            List<EsbAgentInfoV3DTO> agentInfoList = esbQueryAgentInfoV3Resp.getAgentInfoList();
            assertThat(agentInfoList).isNotNull();
            assertThat(agentInfoList).hasSize(1);
            EsbAgentInfoV3DTO esbAgentInfoV3DTO = agentInfoList.get(0);
            assertThat(esbAgentInfoV3DTO.getHostId()).isGreaterThan(0);
            assertThat(esbAgentInfoV3DTO.getStatus()).isGreaterThanOrEqualTo(0);
            assertThat(esbAgentInfoV3DTO.getVersion()).isNotBlank();
        }

        @Test
        @DisplayName("测试查询Agent信息参数长度超限2")
        void testQueryAgentInfoWithTooManyHostIds() {
            EsbQueryAgentInfoV3Req req = new EsbQueryAgentInfoV3Req();
            req.setScopeType("biz");
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            List<Long> hostIdList = new ArrayList<>();
            int maxHostIdNum = 5000;
            for (int i = 0; i < maxHostIdNum + 1; i++) {
                hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            }
            req.setHostIdList(hostIdList);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.QUERY_AGENT_INFO)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
