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

package com.tencent.bk.job.manage.model.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "凭证")
@Data
public class CredentialVO {
    /**
     * 主键Id
     */
    @Schema(description = "主键Id")
    private String id;
    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 类型
     */
    @Schema(description = "类型")
    private String type;
    /**
     * 描述
     */
    @Schema(description = "描述")
    private String description;
    /**
     * 值1
     */
    @Schema(description = "值1")
    private String value1;
    /**
     * 值2
     */
    @Schema(description = "值2")
    private String value2;
    /**
     * 值3
     */
    @Schema(description = "值3")
    private String value3;
    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String creator;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间，单位毫秒")
    private Long createTime;
    /**
     * 最后修改人
     */
    @Schema(description = "最后修改人")
    private String lastModifyUser;
    /**
     * 最后修改时间
     */
    @Schema(description = "最后修改时间，单位毫秒")
    private Long lastModifyTime;

    @Schema(description = "是否可以管理")
    private Boolean canManage;

    @Schema(description = "是否可以使用")
    private Boolean canUse;
}
