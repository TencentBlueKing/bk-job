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

package com.tencent.bk.job.crontab.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.NoXss;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import java.util.List;

/**
 * v4 保存（新建/更新）定时任务请求。
 * <p>
 * 与 v3 的 EsbSaveCronV3Request 的协议差异：
 * <ul>
 *     <li>不再提供 bk_biz_id 兼容字段，业务范围只用 bk_scope_type + bk_scope_id；</li>
 *     <li>主机类全局变量的取值用 v4 的执行目标结构，支持容器执行对象。</li>
 * </ul>
 * 字段间的联合校验（新建必填项、更新至少改一项、表达式与单次执行时间不可同时为空）不放在
 * 分组校验里，而是由 V4SaveCronRequestConverter 统一执行，这样审批预检链路（不经网关、
 * 不走 Bean Validation）与直接执行链路使用同一份校验实现。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class V4SaveCronRequest extends EsbAppScopeReq {

    /**
     * 定时任务 ID。更新时必填，新建时不填
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 要定时执行的执行方案 ID。新建时必填，更新时选填
     */
    @JsonProperty("job_plan_id")
    private Long planId;

    /**
     * 定时任务名称。新建时必填，更新时选填
     */
    @JsonProperty("name")
    @Length(max = 60, message = "{validation.constraints.InvalidCronJobName_outOfLength.message}")
    @NoXss(fieldName = "name")
    private String name;

    /**
     * 周期执行的 cron 表达式，各字段含义为：分 时 日 月 周，如 0/5 * * * ? 表示每 5 分钟执行一次。
     * 与 executeTime 二者至少填一个
     */
    @JsonProperty("expression")
    private String cronExpression;

    /**
     * 单次执行的指定执行时间，Unix 时间戳，单位秒。与 cronExpression 二者至少填一个
     */
    @JsonProperty("execute_time")
    private Long executeTime;

    /**
     * 定时任务启动执行方案时的全局变量取值
     */
    @JsonProperty("global_var_list")
    @Valid
    private List<V4GlobalVarDTO> globalVarList;

    /**
     * 定时任务触发时间所在的时区，如 Asia/Shanghai。不传时取业务时区，业务时区为空时取服务器时区
     */
    @JsonProperty("execute_time_zone")
    private String executeTimeZone;
}
