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

import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.common.exception.RollingExprParseException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 滚动执行子表达式-指数递增
 */
@Getter
@Setter
@ToString
@Slf4j
public class ExponentIncrementRollingExprPart extends RollingExprPart {
    /**
     * 滚动表示式匹配正则
     */
    private static final String EXPR_REGEX = "^\\*(\\d+)$";
    /**
     * 每一批次递增的指数
     */
    private int exponent;

    @Override
    public RollingExprPart parseExpr(String expr) throws RollingExprParseException {
        Pattern pattern = Pattern.compile(EXPR_REGEX);
        Matcher matcher = pattern.matcher(expr);
        if (matcher.find()) {
            ExponentIncrementRollingExprPart rollingExprPart = new ExponentIncrementRollingExprPart();
            rollingExprPart.setExpr(expr);

            int exponent = Integer.parseInt(matcher.group(1));
            if (exponent <= 0) {
                log.warn("Invalid rolling expr part : {}", expr);
                throw new RollingExprParseException();
            }
            rollingExprPart.setExponent(exponent);

            return rollingExprPart;
        } else {
            return null;
        }
    }

    @Override
    public List<IpDTO> compute(RollingServerBatchContext context) throws RollingExprParseException {
        List<IpDTO> candidateServers = context.getRemainedServers();
        // 上一批次的主机
        RollingServerBatch preServerBatch = context.getServerBatches().get(context.getServerBatches().size() - 1);
        if (preServerBatch == null) {
            log.warn("Invalid rolling expr part : {}, no preServerBatch", getExpr());
            throw new RollingExprParseException();
        }

        int currentBatchSize = preServerBatch.getBatch() * exponent;
        return new ArrayList<>(candidateServers.subList(0, Math.min(currentBatchSize, candidateServers.size())));
    }
}
