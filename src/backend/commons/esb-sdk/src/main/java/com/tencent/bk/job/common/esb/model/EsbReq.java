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

package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.BasicHttpReq;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * ESB API 通用请求参数
 */
@Setter
@Getter
public class EsbReq extends BasicHttpReq {

    /**
     * 租户账号 - 除了 cmdb 之外，其他平台暂未使用
     */
    @JsonProperty("bk_supplier_account")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String bkSupplierAccount;

    /**
     * 构造请求
     *
     * @param reqClass          要构建返回的请求 class
     * @param bkSupplierAccount 开发商code
     * @return EsbReq
     */
    public static <T extends EsbReq> T buildRequest(Class<T> reqClass, String bkSupplierAccount) {
        T esbReq;
        try {
            esbReq = reqClass.newInstance();
            if (StringUtils.isEmpty(bkSupplierAccount)) {
                esbReq.setBkSupplierAccount("0");
            } else {
                esbReq.setBkSupplierAccount(bkSupplierAccount);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return esbReq;
    }

}
