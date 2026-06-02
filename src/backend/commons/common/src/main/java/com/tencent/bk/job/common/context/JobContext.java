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

package com.tencent.bk.job.common.context;

import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.User;
import io.micrometer.core.instrument.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.time.ZoneId;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * Job http 请求上下文
 */
@Data
public class JobContext {

    private Long startTime;

    private BasicApp app;

    private String requestId;

    private String userLang;

    private String requestFrom;

    private List<String> debugMessage;

    private ZoneId timeZone;

    private Boolean allowMigration = false;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private Map<String, Pair<String, AbstractList<Tag>>> metricTagsMap;

    private AbstractList<Tag> httpMetricTags;

    /**
     * 控制器类名，用于缓存控制器信息，避免在序列化阶段访问被回收的请求对象
     */
    private String controllerClassName;

    /**
     * 用户
     */
    private User user;

    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }

    public String getTenantId() {
        return user != null ? user.getTenantId() : null;
    }

    /**
     * 为子线程创建一份隔离的副本，避免多个工作线程通过 {@link io.micrometer.context.ContextSnapshot}
     * 共享同一份父线程 JobContext，进而并发读写其内部可变集合(典型如 {@link #metricTagsMap}
     * 中的 ArrayList) 引发 {@link ArrayIndexOutOfBoundsException}。
     *
     * <p>克隆策略：</p>
     * <ul>
     *     <li><b>不可变 / 只读引用字段</b>(startTime、app、requestId、userLang、requestFrom、
     *     timeZone、allowMigration、request、response、httpMetricName、controllerClassName、user)
     *     直接复用父线程的引用，避免不必要的拷贝开销；这些字段在子线程中通常只读，即便写入也是
     *     替换整个引用，不会影响父线程对象内部状态。</li>
     *     <li><b>可变集合字段</b>(metricTagsMap、debugMessage、httpMetricTags) 一律重置为
     *     {@code null}：子线程的 HTTP 指标采集、debug 信息均按需自行初始化，父线程的中间状态
     *     对子线程没有语义价值，重置后可彻底避免共享同一份 ArrayList 引发的并发问题。</li>
     * </ul>
     *
     * @return 子线程专用的 JobContext 副本
     */
    public JobContext copyForChildThread() {
        JobContext copy = new JobContext();
        copy.startTime = this.startTime;
        copy.app = this.app;
        copy.requestId = this.requestId;
        copy.userLang = this.userLang;
        copy.requestFrom = this.requestFrom;
        copy.timeZone = this.timeZone;
        copy.allowMigration = this.allowMigration;
        copy.request = this.request;
        copy.response = this.response;
        copy.controllerClassName = this.controllerClassName;
        copy.user = this.user;
        // 可变集合字段不复用父线程的引用，留给子线程按需懒初始化，避免并发读写父线程集合
        copy.metricTagsMap = null;
        copy.debugMessage = null;
        copy.httpMetricTags = null;
        return copy;
    }

}
