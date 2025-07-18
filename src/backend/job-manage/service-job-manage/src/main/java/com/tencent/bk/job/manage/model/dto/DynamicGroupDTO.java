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

package com.tencent.bk.job.manage.model.dto;

import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.manage.model.web.vo.DynamicGroupBasicVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DynamicGroupDTO {

    private Long bizId;

    private String id;

    private String name;

    private String lastTime;

    public static DynamicGroupDTO fromCcGroupDTO(CcDynamicGroupDTO ccGroupDTO) {
        if (ccGroupDTO == null) {
            return null;
        }
        DynamicGroupDTO dynamicGroupDTO = new DynamicGroupDTO();
        dynamicGroupDTO.setId(ccGroupDTO.getId());
        dynamicGroupDTO.setBizId(ccGroupDTO.getBizId());
        dynamicGroupDTO.setName(ccGroupDTO.getName());
        dynamicGroupDTO.setLastTime(ccGroupDTO.getLastTime());
        return dynamicGroupDTO;
    }

    public DynamicGroupBasicVO toBasicVO() {
        return new DynamicGroupBasicVO(id, name, lastTime, null);
    }

    public ZonedDateTime getParsedLastTime() {
        try {
            return ZonedDateTime.parse(lastTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.warn("Fail to parse ZonedDateTime from lastTime {}, format=ISO_DATE_TIME", lastTime);
            return null;
        }
    }
}
