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

package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * ESB业务通知策略DTO
 */
@Data
public class EsbNotifyPolicyV3DTO {

    @JsonProperty("trigger_type")
    @JsonPropertyDescription("触发方式")
    private String triggerType;

    @JsonProperty("resource_type_list")
    @JsonPropertyDescription("操作（任务）类型列表")
    private List<String> resourceTypeList;

    @JsonProperty("role_list")
    @JsonPropertyDescription("任务角色（通知对象）列表")
    private List<String> roleList;

    @JsonProperty("extra_observer_list")
    @JsonPropertyDescription("额外通知人列表")
    private List<String> extraObserverList;

    @JsonProperty("resource_status_channel_map")
    @JsonPropertyDescription("状态通知渠道列表，key:执行状态(SUCCESS,FAIL,READY),value:通知渠道列表")
    private Map<String, List<String>> resourceStatusChannelMap;

}
