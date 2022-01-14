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

package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.execute.common.exception.RollingExprParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动策略表达式
 */
public class RollingExpr {
    /**
     * 滚动策略子表达式解析器
     */
    private static final List<RollingExprPart> ROLLING_EXPR_PART_PARSER = new ArrayList<>();

    static {
        ROLLING_EXPR_PART_PARSER.add(new PercentRollingExprPart());
        ROLLING_EXPR_PART_PARSER.add(new QuantityRollingExprPart());
    }

    /**
     * 滚动策略表达式
     */
    private final String rollingExpr;
    /**
     * 解析之后的滚动策略子表达式列表
     */
    private final List<RollingExprPart> rollingExprParts;

    /**
     * Constructor
     *
     * @param rollingExprStr 滚动策略表达式
     */
    public RollingExpr(String rollingExprStr) {
        this.rollingExpr = rollingExprStr;
        this.rollingExprParts = parseRollingExpr(rollingExprStr);
    }

    private List<RollingExprPart> parseRollingExpr(String rollingExprStr) {
        String[] rollingExprParts = rollingExprStr.split(" ");
        List<RollingExprPart> rollingExprPartList = new ArrayList<>();
        for (String exprPart : rollingExprParts) {
            String rollingExprPartStr = exprPart.trim();
            RollingExprPart rollingExprPart = parseRollingExprPart(rollingExprPartStr);
            rollingExprPartList.add(rollingExprPart);
        }
        return rollingExprPartList;
    }

    private RollingExprPart parseRollingExprPart(String rollingExprPartStr) {
        for (RollingExprPart rollingExprPart : ROLLING_EXPR_PART_PARSER) {
            RollingExprPart parseResult = rollingExprPart.parseExpr(rollingExprPartStr);
            if (parseResult != null) {
                return parseResult;
            }
        }
        throw new RollingExprParseException();
    }

    /**
     * 获取滚动策略表达式
     */
    public String getRollingExpr() {
        return rollingExpr;
    }

    /**
     * 根据滚动执行批次获取滚动策略子表达式
     *
     * @param batch 滚动执行批次
     */
    public RollingExprPart nextRollingExprPart(int batch) {
        return this.rollingExprParts.get(Math.min(this.rollingExprParts.size(), batch) - 1);
    }
}
