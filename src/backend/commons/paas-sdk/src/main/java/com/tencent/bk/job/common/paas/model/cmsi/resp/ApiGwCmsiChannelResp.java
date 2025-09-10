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

package com.tencent.bk.job.common.paas.model.cmsi.resp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.paas.model.NotifyChannelDTO;
import lombok.Data;

/**
 * 访问APIGW的CMSI获取通知渠道列表的响应，响应字段与ESB的CMSI不同，需要区分
 */
@Data
public class ApiGwCmsiChannelResp {
    /**
     * 渠道类型
     */
    private String type;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 该租户内是否支持
     */
    @JsonProperty("enable")
    private boolean enable;

    /**
     * 渠道图标Base64编码
     */
    private String icon;

    @JsonIgnore
    public NotifyChannelDTO toNotifyChannelDTO() {
        NotifyChannelDTO notifyChannelDTO = new NotifyChannelDTO();
        notifyChannelDTO.setType(type);
        notifyChannelDTO.setName(name);
        notifyChannelDTO.setIcon(icon);
        notifyChannelDTO.setEnabled(enable);
        return notifyChannelDTO;
    }
}
