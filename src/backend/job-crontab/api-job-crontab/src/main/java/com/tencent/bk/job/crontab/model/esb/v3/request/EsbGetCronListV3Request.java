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

package com.tencent.bk.job.crontab.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 26/2/2020 16:29
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EsbGetCronListV3Request extends EsbReq {

    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 定时作业名称
     */
    private String name;

    /**
     * 定时任务ID
     * <p>
     * 如果存在则忽略其他筛选条件，只查询这个指定的作业信息
     */
    private Long id;

    /**
     * 定时作业状态
     * <p>
     * 1.已启动、2.已暂停
     */
    private Integer status;

    /**
     * 定时作业创建人帐号
     */
    private String creator;

    /**
     * 创建起始时间，毫秒时间戳
     */
    @JsonProperty("create_time_start")
    private Long createTimeStart;

    /**
     * 创建结束时间，毫秒时间戳
     */
    @JsonProperty("create_time_end")
    private Long createTimeEnd;

    /**
     * 作业修改人帐号
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;

    /**
     * 最后修改起始时间，毫秒时间戳
     */
    @JsonProperty("last_modify_time_start")
    private Long lastModifyTimeStart;

    /**
     * 最后修改结束时间，毫秒时间戳
     */
    @JsonProperty("last_modify_time_end")
    private Long lastModifyTimeEnd;

    /**
     * 默认 0 表示从第 1 条记录开始返回
     */
    private Integer start;

    /**
     * 返回记录数量，不传此参数默认返回 20 条
     */
    private Integer length;

    public boolean validate() {
        if (appId == null || appId < 0) {
            return false;
        }

        if (StringUtils.isEmpty(name)) {
            name = null;
        }

        if (status != null) {
            if (status != 0 && status != 1) {
                status = null;
            }
        }

        if (StringUtils.isEmpty(creator)) {
            creator = null;
        }

        if (createTimeStart == null || createTimeEnd == null) {
            createTimeStart = null;
            createTimeEnd = null;
        }

        if (StringUtils.isEmpty(lastModifyUser)) {
            lastModifyUser = null;
        }

        if (lastModifyTimeStart == null || lastModifyTimeEnd == null) {
            lastModifyTimeStart = null;
            lastModifyTimeEnd = null;
        }

        if (start == null || start < 0) {
            start = 0;
        }

        if (length == null || length < 0) {
            length = 20;
        }

        if (id != null && id > 0) {
            return true;
        } else {
            id = null;
        }

        return true;
    }
}
