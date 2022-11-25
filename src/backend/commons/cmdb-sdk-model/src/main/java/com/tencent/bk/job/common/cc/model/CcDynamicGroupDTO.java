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

package com.tencent.bk.job.common.cc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.dto.DynamicGroupWithHost;
import lombok.Data;

import java.util.List;

/**
 * CC动态分组
 */
@Data
public class CcDynamicGroupDTO {
    /**
     * cmdb业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long bizId;

    /**
     * cmdb动态分组ID
     */
    private String id;

    /**
     * cmdb动态分组名称
     */
    private String name;

    @JsonProperty("bk_obj_id")
    private String objId;

    private DynamicGroupInfo info;

    @Data
    static class DynamicGroupCondition {
        private String field;
        private String operator;
        private Object value;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }

    @Data
    static class DynamicGroupObjCondition {
        @JsonProperty("bk_obj_id")
        private String objId;

        private List<DynamicGroupCondition> condition;

        public String getObjId() {
            return objId;
        }

        public void setObjId(String objId) {
            this.objId = objId;
        }

        public List<DynamicGroupCondition> getCondition() {
            return condition;
        }

        public void setCondition(List<DynamicGroupCondition> condition) {
            this.condition = condition;
        }
    }

    @Data
    static class DynamicGroupInfo {
        private List<DynamicGroupObjCondition> condition;

        public List<DynamicGroupObjCondition> getCondition() {
            return condition;
        }

        public void setCondition(List<DynamicGroupObjCondition> condition) {
            this.condition = condition;
        }
    }

    public DynamicGroupWithHost toDynamicGroupWithHost() {
        DynamicGroupWithHost dynamicGroupWithHost = new DynamicGroupWithHost();
        dynamicGroupWithHost.setId(this.getId());
        dynamicGroupWithHost.setBizId(this.getBizId());
        dynamicGroupWithHost.setName(this.getName());
        return dynamicGroupWithHost;
    }
}
