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

package com.tencent.bk.job.manage.model.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collection;
import java.util.List;

/**
 * 主机查询
 */
@Getter
@Setter
@ToString
@Builder
public class HostQuery {
    /**
     * 主机所在的业务ID
     */
    private Collection<Long> bizIds;
    /**
     * 主机所在的业务模块ID
     */
    private Collection<Long> moduleIds;
    /**
     * 主机云区域ID
     */
    private Collection<Long> cloudAreaIds;
    /**
     * 模糊搜索key，匹配字段：IP、IP_V6、IP_DESC、OS
     */
    private List<String> searchContents;
    /**
     * 主机Agent状态
     */
    private Integer agentAlive;
    /**
     * 主机IP模糊搜索key列表，任意一个key匹配即命中
     */
    private List<String> ipKeyList;
    /**
     * 主机IPv6模糊搜索key列表，任意一个key匹配即命中
     */
    private List<String> ipv6KeyList;
    /**
     * 主机名称模糊搜索key列表，任意一个key匹配即命中
     */
    private List<String> hostNameKeyList;
    /**
     * 主机系统名称模糊搜索key列表，任意一个key匹配即命中
     */
    private List<String> osNameKeyList;
    /**
     * 起始位置
     */
    private Long start;
    /**
     * 记录数量
     */
    private Long limit;
}
