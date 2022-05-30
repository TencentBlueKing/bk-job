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

import java.util.List;

/**
 * 滚动策略表达式部分
 */
@Getter
@Setter
@ToString
public abstract class RollingExprPart {
    /**
     * 滚动表达式部分
     */
    private String expr;

    /**
     * 解析滚动表达式
     *
     * @param expr 滚动表达式
     * @return 子表达式
     */
    public abstract RollingExprPart parseExpr(String expr) throws RollingExprParseException;

    /**
     * 根据滚动子表达式以及分批计算上下文，计算当前批次的主机
     *
     * @param context 解析上下文
     * @return 当前批次的主机
     */
    public abstract List<IpDTO> compute(RollingServerBatchContext context) throws RollingExprParseException;

}
