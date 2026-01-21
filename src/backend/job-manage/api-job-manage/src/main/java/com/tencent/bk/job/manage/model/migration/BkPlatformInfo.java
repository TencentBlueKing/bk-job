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

package com.tencent.bk.job.manage.model.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BkPlatformInfo {

    @JsonProperty("bkAppCode")
    private String bkAppCode = "bk_job";

    @JsonProperty("name")
    private String name = "蓝鲸作业平台";

    @JsonProperty("nameEn")
    private String nameEn = "BlueKing JOB";

    @JsonProperty("appLogo")
    private String appLogo = "/static/images/logo.png";

    @JsonProperty("favicon")
    private String favicon = "/static/images/favicon.icon";

    @JsonProperty("helperText")
    private String helperText = "联系BK助手";

    @JsonProperty("helperTextEn")
    private String helperTextEn = "Contact BK Assistant";

    @JsonProperty("helperLink")
    private String helperLink = "wxwork://message/?username=BK%E5%8A%A9%E6%89%8B";

    @JsonProperty("brandImg")
    private String brandImg = "brand_img.png";

    @JsonProperty("brandImgEn")
    private String brandImgEn = "brand_img.png";

    @JsonProperty("brandName")
    private String brandName = "腾讯蓝鲸智云";

    @JsonProperty("brandNameEn")
    private String brandNameEn = "BlueKing";

    @JsonProperty("footerInfo")
    private String footerInfo = "[技术支持](https://wpa1.qq.com/KziXGWJs?_type=wpa&qidian=true) | [社区论坛](https://bk" +
        ".tencent.com/s-mart/community/) | [产品官网](https://bk.tencent.com/index/)";

    @JsonProperty("footerInfoEn")
    private String footerInfoEn = "[Support](https://wpa1.qq.com/KziXGWJs?_type=wpa&qidian=true) | [Forum](https://bk" +
        ".tencent.com/s-mart/community/) | [Official](https://bk.tencent.com/index/)";

    @JsonProperty("footerCopyright")
    private String footerCopyright = "Copyright © 2012 Tencent BlueKing. All Rights Reserved. {{version}}";
}
