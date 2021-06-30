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

package com.tencent.bk.job.common.cc.model.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

public class FindHostByModuleResult extends AbstractCcSearchResult<FindHostByModuleResult.HostWithTopoInfo> {
    @Getter
    @Setter
    @ToString
    public static class HostWithTopoInfo {
        private List<BizProp> biz;
        private HostProp host;
        private List<ModuleProp> module;
        private List<SetProp> set;
    }

    @Getter
    @Setter
    @ToString
    public static class BizProp {
        @JsonProperty("bk_biz_id")
        private Long appId;
        @JsonProperty("bk_biz_name")
        private String appName;
        @JsonProperty("bk_biz_maintainer")
        private String appMaintainer;
    }

    @Getter
    @Setter
    @ToString
    public static class HostProp {
        @JsonProperty("bk_host_id")
        private Long hostId;
        @JsonProperty("bk_host_innerip")
        private String ip;
        @JsonProperty("bk_host_name")
        private String hostName;
        @JsonProperty("bk_os_name")
        private String os;
        @JsonProperty("bk_cloud_id")
        private List<CloudAreaProp> cloudAreaList;
    }

    @Getter
    @Setter
    @ToString
    public static class CloudAreaProp {
        @JsonProperty("bk_inst_id")
        private Long cloudId;
        @JsonProperty("bk_inst_name")
        private String cloudAreaName;
    }

    @Getter
    @Setter
    @ToString
    public static class ModuleProp {
        @JsonProperty("bk_module_id")
        private Long moduleId;
        @JsonProperty("bk_set_id")
        private Long setId;
        @JsonProperty("bk_module_name")
        private String moduleName;
        @JsonProperty("bk_module_type")
        private String moduleType;
    }

    @Getter
    @Setter
    @ToString
    public static class SetProp {
        @JsonProperty("bk_biz_id")
        private Long appId;
        @JsonProperty("bk_set_id")
        private Long setId;
        @JsonProperty("bk_set_name")
        private String setName;
    }
}
