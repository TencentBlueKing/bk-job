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

package com.tencent.bk.job.execute.model.op.req;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 批量新增 callback URL 白名单请求
 */
@Data
@ApiModel("批量新增回调地址白名单请求")
public class BatchAddCallbackUrlWhitelistReq {

    /**
     * 单条新增项；单条增删通过传入仅含 1 个元素的列表实现。
     */
    @ApiModelProperty(value = "待新增的白名单条目列表，单条新增通过传入单元素列表实现", required = true)
    private List<Item> items;

    @Data
    @ApiModel("回调地址白名单条目")
    public static class Item {

        /**
         * 允许的回调地址 baseUrl 前缀
         */
        @ApiModelProperty(value = "允许的回调地址 baseUrl 前缀，必须以 http:// 或 https:// 开头", required = true)
        private String baseUrl;

        /**
         * 备注说明
         */
        @ApiModelProperty("备注说明")
        private String description;
    }
}
