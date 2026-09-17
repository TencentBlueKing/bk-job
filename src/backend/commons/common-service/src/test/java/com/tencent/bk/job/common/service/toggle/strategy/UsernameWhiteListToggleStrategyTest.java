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

package com.tencent.bk.job.common.service.toggle.strategy;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("用户名白名单灰度策略测试")
class UsernameWhiteListToggleStrategyTest {

    private UsernameWhiteListToggleStrategy buildStrategy(String usernameList) {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(UsernameWhiteListToggleStrategy.INIT_PARAM_USERNAME_LIST, usernameList);
        return new UsernameWhiteListToggleStrategy(initParams);
    }

    private ToggleEvaluateContext ctxOfUser(String username) {
        return ToggleEvaluateContext.builder()
            .addContextParam(ToggleStrategyContextParams.CTX_PARAM_USERNAME, username);
    }

    @Test
    @DisplayName("命中白名单用户返回 true")
    void evaluateHitUsername() {
        UsernameWhiteListToggleStrategy strategy = buildStrategy("admin, alice ,bob");
        assertThat(strategy.evaluate("iamV4", ctxOfUser("admin"))).isTrue();
        assertThat(strategy.evaluate("iamV4", ctxOfUser("alice"))).isTrue();
        assertThat(strategy.evaluate("iamV4", ctxOfUser("bob"))).isTrue();
    }

    @Test
    @DisplayName("未命中白名单用户返回 false")
    void evaluateMissUsername() {
        UsernameWhiteListToggleStrategy strategy = buildStrategy("admin,alice");
        assertThat(strategy.evaluate("iamV4", ctxOfUser("carol"))).isFalse();
    }

    @Test
    @DisplayName("上下文缺少 username 返回 false")
    void evaluateWithoutUsernameContext() {
        UsernameWhiteListToggleStrategy strategy = buildStrategy("admin");
        assertThat(strategy.evaluate("iamV4", ToggleEvaluateContext.builder())).isFalse();
    }

    @Test
    @DisplayName("缺少 usernameList 初始化参数抛异常")
    void missingInitParamThrows() {
        assertThatThrownBy(() -> new UsernameWhiteListToggleStrategy(Collections.emptyMap()))
            .isInstanceOf(ToggleStrategyParseException.class);
    }

    @Test
    @DisplayName("空白名单不命中任何用户")
    void emptyWhiteListMatchesNobody() {
        UsernameWhiteListToggleStrategy strategy = buildStrategy("");
        assertThat(strategy.evaluate("iamV4", ctxOfUser("admin"))).isFalse();
    }
}
