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

package com.tencent.bk.job.manage.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OpenAPI V4 资源范围下按条件搜索主机请求体。
 * <p>
 * 支持业务(biz)、业务集(biz_set)、租户集(tenant_set)；拓扑节点仅在业务(biz)下生效。
 */
@Getter
@Setter
public class V4SearchScopeHostRequest extends EsbAppScopeReq {

    /**
     * 拓扑节点列表，仅业务(biz)生效，业务集/租户集忽略。
     */
    @JsonProperty("topo_node_list")
    @Valid
    private List<V4TopoNodeDTO> topoNodeList;

    /**
     * IPv4 关键字列表，模糊匹配。
     */
    @JsonProperty("ipv4_key_list")
    private List<String> ipv4KeyList;

    /**
     * IPv6 关键字列表，模糊匹配。
     */
    @JsonProperty("ipv6_key_list")
    private List<String> ipv6KeyList;

    /**
     * 主机名称关键字列表，模糊匹配。
     */
    @JsonProperty("host_name_key_list")
    private List<String> hostNameKeyList;

    /**
     * 操作系统名称关键字列表，模糊匹配。
     */
    @JsonProperty("os_name_key_list")
    private List<String> osNameKeyList;

    /**
     * Agent 状态过滤：0-异常，1-正常；不传则不过滤。
     */
    @JsonProperty("alive")
    private Integer alive;

    /**
     * 分页起始偏移，从 0 开始；不传默认为 0。
     */
    @JsonProperty("offset")
    @Min(value = 0L, message = "{validation.constraints.InvalidPageOffset.message}")
    private Integer offset = 0;

    /**
     * 单页返回条数；不传默认为 10；取值范围 1-200。
     */
    @JsonProperty("length")
    @Range(min = 1L, max = 200L, message = "{validation.constraints.InvalidPageLength.message}")
    private Integer length = 10;

    /**
     * 获取清洗后的 IPv4 关键字列表（去除 null 值与空字符串）。
     */
    @JsonIgnore
    public List<String> getCleanIpv4KeyList() {
        return getCleanStringList(ipv4KeyList);
    }

    /**
     * 获取清洗后的 IPv6 关键字列表（去除 null 值与空字符串）。
     */
    @JsonIgnore
    public List<String> getCleanIpv6KeyList() {
        return getCleanStringList(ipv6KeyList);
    }

    /**
     * 获取清洗后的主机名称关键字列表（去除 null 值与空字符串）。
     */
    @JsonIgnore
    public List<String> getCleanHostNameKeyList() {
        return getCleanStringList(hostNameKeyList);
    }

    /**
     * 获取清洗后的操作系统名称关键字列表（去除 null 值与空字符串）。
     */
    @JsonIgnore
    public List<String> getCleanOsNameKeyList() {
        return getCleanStringList(osNameKeyList);
    }

    private List<String> getCleanStringList(List<String> rawList) {
        if (CollectionUtils.isEmpty(rawList)) {
            return rawList;
        }
        return rawList.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }
}
