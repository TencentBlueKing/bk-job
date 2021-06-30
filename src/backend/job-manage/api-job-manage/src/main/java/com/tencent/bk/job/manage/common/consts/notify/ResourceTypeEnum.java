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

package com.tencent.bk.job.manage.common.consts.notify;

import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.manage.model.web.vo.notify.ResourceTypeVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ResourceTypeEnum {
    /**
     * 快速执行脚本
     */
    SCRIPT(1, "job.manage.resource.type.script"),
    /**
     * 快速分发文件
     */
    FILE(3, "job.manage.resource.type.file"),

    /**
     * 执行方案
     */
    JOB(2, "job.manage.resource.type.job");

    private final int type;
    private final String defaultName;

    public static ResourceTypeEnum get(int type) {
        val values = ResourceTypeEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].type == type) {
                return values[i];
            }
        }
        return null;
    }

    public static String getName(int type) {
        val values = ResourceTypeEnum.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].type == type) {
                return values[i].name();
            }
        }
        return null;
    }

    public static List<ResourceTypeVO> getVOList() {
        List<ResourceTypeVO> resultList = new ArrayList<ResourceTypeVO>();
        val values = ResourceTypeEnum.values();
        for (int i = 0; i < values.length; i++) {
            resultList.add(new ResourceTypeVO(values[i].name(), I18nUtil.getI18nMessage(values[i].getDefaultName())));
        }
        return resultList;
    }

    public static ResourceTypeVO getVO(int type) {
        val resourceType = ResourceTypeEnum.get(type);
        if (resourceType == null) return null;
        return new ResourceTypeVO(resourceType.name(), I18nUtil.getI18nMessage(resourceType.getDefaultName()));
    }
}
