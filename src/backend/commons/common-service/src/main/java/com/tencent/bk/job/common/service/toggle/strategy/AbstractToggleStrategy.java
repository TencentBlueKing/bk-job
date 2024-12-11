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

package com.tencent.bk.job.common.service.toggle.strategy;

import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashMap;
import java.util.Map;

/**
 * 特性开关开启策略基础实现抽象类
 */
@Slf4j
public abstract class AbstractToggleStrategy implements ToggleStrategy {

    /**
     * 策略 ID
     */
    protected final String id;

    /**
     * 策略初始化参数
     */
    protected final Map<String, String> initParams;

    /**
     * 构造策略
     *
     * @param strategyId 策略ID
     * @param initParams 初始化参数
     */
    public AbstractToggleStrategy(String strategyId,
                                  Map<String, String> initParams) {
        this.id = strategyId;
        if (initParams != null) {
            this.initParams = initParams;
        } else {
            this.initParams = new HashMap<>();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, String> getInitParams() {
        return this.initParams;
    }

    public void assertRequiredInitParam(String paramName) {
        if (!initParams.containsKey(paramName)) {
            String msg = MessageFormatter.format(
                "Parameter {} is required for this ToggleStrategy", paramName).getMessage();
            log.error(msg);
            throw new ToggleStrategyParseException(msg);
        }
    }


    public boolean checkRequiredContextParam(ToggleEvaluateContext context, String paramName) {
        boolean checkResult = true;
        if (context.getParam(paramName) == null) {
            log.info("Context param {} is required for evaluate", paramName);
            checkResult = false;
        }
        return checkResult;
    }

}
