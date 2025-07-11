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

package com.tencent.bk.job.common.util.check;

import com.tencent.bk.job.common.util.check.exception.StringCheckException;

public class MaxLengthChecker implements IStringCheckStrategy {

    private int maxLength;

    public MaxLengthChecker() {
        this.maxLength = 60;
    }

    public MaxLengthChecker(int maxLength) {
        this.maxLength = maxLength;
    }

    public static void main(String[] args) {
        IStringCheckStrategy checkStrategy = new MaxLengthChecker(5);
        System.out.println(checkStrategy.checkAndGetResult(""));
        System.out.println(checkStrategy.checkAndGetResult("aaa"));
        System.out.println(checkStrategy.checkAndGetResult("aaaaa"));
        System.out.println(checkStrategy.checkAndGetResult("aaaaaa"));
    }

    @Override
    public String checkAndGetResult(String rawStr) {
        if (rawStr.length() > maxLength) {
            String message = rawStr + " length can not beyond " + this.maxLength;
            throw new StringCheckException(message);
        } else {
            return rawStr;
        }
    }
}
