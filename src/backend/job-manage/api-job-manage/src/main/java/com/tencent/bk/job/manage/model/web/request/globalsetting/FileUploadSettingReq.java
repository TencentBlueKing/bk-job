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

package com.tencent.bk.job.manage.model.web.request.globalsetting;

import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.manage.common.consts.globalsetting.RestrictModeEnum;
import com.tencent.bk.job.manage.common.consts.globalsetting.StorageUnitEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import java.util.List;

/**
 * @Description
 * @Date 2020/12/8
 * @Version 1.0
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel("文件上传参数")
public class FileUploadSettingReq {
    @ApiModelProperty("数量")
    @NotNull(message = "{validation.constraints.InvalidFileUploadSettingAmount.message}")
    @Positive(message = "{validation.constraints.InvalidFileUploadSettingAmount.message}")
    private Float amount;

    @ApiModelProperty("单位:可选B/KB/MB/GB/TB/PB")
    private StorageUnitEnum unit;

    @ApiModelProperty("限制模式，0:禁止范围，1：允许范围，-1：不限制")
    @CheckEnum(enumClass = RestrictModeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.InvalidUploadFileRestrictMode.message}")
    private Integer restrictMode;

    @ApiModelProperty("后缀列表")
    private List<@Pattern(regexp = "^\\.[A-Za-z0-9_-]{1,24}$",
        message = "{validation.constraints.InvalidUploadFileSuffix.message}") String> suffixList;

}
