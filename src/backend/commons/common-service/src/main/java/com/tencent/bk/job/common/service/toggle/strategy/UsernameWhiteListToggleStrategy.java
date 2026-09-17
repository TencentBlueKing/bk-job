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
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 根据用户名白名单灰度策略。命中白名单内的用户名则开关开启，主要用于 IAM v3/v4 按用户灰度切换。
 */
public class UsernameWhiteListToggleStrategy extends AbstractToggleStrategy {
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "UsernameWhiteListToggleStrategy";

    /**
     * 策略初始化参数-用户名列表（逗号分隔）
     */
    public static final String INIT_PARAM_USERNAME_LIST = "usernameList";

    /*
     * 灰度用户列表
     */
    private final Set<String> usernames = new HashSet<>();

    public UsernameWhiteListToggleStrategy(Map<String, String> initParams) {
        super(STRATEGY_ID, initParams);
        assertRequiredInitParam(INIT_PARAM_USERNAME_LIST);
        String usernameListValue = initParams.get(INIT_PARAM_USERNAME_LIST);
        if (StringUtils.isNotBlank(usernameListValue)) {
            for (String username : usernameListValue.split(",")) {
                if (StringUtils.isNotBlank(username)) {
                    usernames.add(username.trim());
                }
            }
        }
    }

    @Override
    public boolean evaluate(String toggleName, ToggleEvaluateContext ctx) {
        if (!checkRequiredContextParam(ctx, ToggleStrategyContextParams.CTX_PARAM_USERNAME)) {
            return false;
        }
        String username = (String) ctx.getParam(ToggleStrategyContextParams.CTX_PARAM_USERNAME);
        return usernames.contains(username);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UsernameWhiteListToggleStrategy.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("initParams=" + initParams)
            .add("usernames=" + usernames)
            .toString();
    }
}
