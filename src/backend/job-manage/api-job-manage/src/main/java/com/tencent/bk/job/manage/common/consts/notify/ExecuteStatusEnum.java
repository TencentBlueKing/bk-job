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

package com.tencent.bk.job.manage.common.consts.notify;

import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.manage.model.web.vo.notify.ExecuteStatusVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ExecuteStatusEnum {
    /**
     * 执行成功
     */
    SUCCESS(1, "job.manage.execute.status.success"),

    /**
     * 执行失败
     */
    FAIL(2, "job.manage.execute.status.fail"),

    /**
     * 等待执行
     */
    READY(3, "job.manage.execute.status.ready");

    private final int status;
    private final String i18nCode;

    public static ExecuteStatusEnum get(int status) {
        val values = ExecuteStatusEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].status == status) {
                return values[i];
            }
        }
        return null;
    }

    public static String getName(int status) {
        val values = ExecuteStatusEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].status == status) {
                return values[i].name();
            }
        }
        return null;
    }

    public static List<ExecuteStatusVO> getVOList() {
        List<ExecuteStatusVO> resultList = new ArrayList<ExecuteStatusVO>();
        val values = ExecuteStatusEnum.values();
        for (int i = 0; i < values.length; i++) {
            resultList.add(new ExecuteStatusVO(values[i].name(), I18nUtil.getI18nMessage(values[i].getI18nCode())));
        }
        return resultList;
    }
}
