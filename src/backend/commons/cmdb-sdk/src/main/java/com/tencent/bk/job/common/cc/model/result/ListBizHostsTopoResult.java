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

public class ListBizHostsTopoResult extends AbstractCcSearchResult<ListBizHostsTopoResult.HostInfo> {
    @Getter
    @Setter
    @ToString
    public static class HostInfo {
        private HostProp host;
        private List<TopoProp> topo;
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
        private Long cloudId;
    }

    @Getter
    @Setter
    @ToString
    public static class TopoProp {
        @JsonProperty("bk_set_id")
        private Long setId;
        @JsonProperty("bk_set_name")
        private String setName;
        private List<ModuleProp> module;
    }

    @Getter
    @Setter
    @ToString
    public static class ModuleProp {
        @JsonProperty("bk_module_id")
        private Long moduleId;
        @JsonProperty("bk_module_name")
        private String moduleName;
    }
}
