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
 * 滚动执行子表达式-按照服务器数量解析
 */
@Getter
@Setter
@ToString
@Slf4j
public class QuantityRollingExprPart extends RollingExprPart {
    private static final String QUANTITY_EXPR_REGEX = "^(\\d+)$";
    private int quantity;

    @Override
    public RollingExprPart parseExpr(String expr) {

        Pattern pattern = Pattern.compile(QUANTITY_EXPR_REGEX);
        Matcher matcher = pattern.matcher(expr);
        if (matcher.find()) {
            QuantityRollingExprPart rollingExprPart = new QuantityRollingExprPart();
            rollingExprPart.setExpr(expr);

            int quantity = Integer.parseInt(matcher.group(1));
            if (quantity <= 0) {
                log.warn("Invalid rolling expr part : {}", expr);
                throw new RollingExprParseException();
            }
            rollingExprPart.setQuantity(quantity);

            return rollingExprPart;
        } else {
            return null;
        }
    }

    @Override
    public List<IpDTO> compute(int total, List<IpDTO> candidateServers) {
        return new ArrayList<>(candidateServers.subList(0, Math.min(this.quantity, candidateServers.size())));
    }
}
