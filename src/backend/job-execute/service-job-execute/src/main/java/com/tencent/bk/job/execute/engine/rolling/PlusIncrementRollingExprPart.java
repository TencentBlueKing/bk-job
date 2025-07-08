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

package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.execute.common.exception.RollingExprParseException;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 滚动执行子表达式-加法递增
 */
@Getter
@Setter
@ToString
@Slf4j
public class PlusIncrementRollingExprPart extends RollingExprPart {
    /**
     * 滚动表示式匹配正则
     */
    private static final String EXPR_REGEX = "^\\+(\\d+)$";
    /**
     * 每一批次递增的执行对象数量
     */
    private int addend;

    @Override
    public RollingExprPart parseExpr(String expr) throws RollingExprParseException {
        Pattern pattern = Pattern.compile(EXPR_REGEX);
        Matcher matcher = pattern.matcher(expr);
        if (matcher.find()) {
            PlusIncrementRollingExprPart rollingExprPart = new PlusIncrementRollingExprPart();
            rollingExprPart.setExpr(expr);

            int addend = Integer.parseInt(matcher.group(1));
            if (addend <= 0) {
                log.warn("Invalid rolling expr part : {}", expr);
                throw new RollingExprParseException();
            }
            rollingExprPart.setAddend(addend);

            return rollingExprPart;
        } else {
            return null;
        }
    }

    @Override
    public List<ExecuteObject> compute(RollingExecuteObjectBatchContext context) throws RollingExprParseException {
        List<ExecuteObject> candidateExecuteObjects = context.getRemainedExecuteObjects();
        // 上一批次的执行对象
        RollingExecuteObjectBatch preBatch = CollectionUtils.isEmpty(context.getExecuteObjectBatches()) ?
            null : context.getExecuteObjectBatches().get(context.getExecuteObjectBatches().size() - 1);

        int currentBatchSize = preBatch == null ? addend : preBatch.getExecuteObjects().size() + addend;
        return new ArrayList<>(candidateExecuteObjects.subList(
            0, Math.min(currentBatchSize, candidateExecuteObjects.size())));
    }
}
