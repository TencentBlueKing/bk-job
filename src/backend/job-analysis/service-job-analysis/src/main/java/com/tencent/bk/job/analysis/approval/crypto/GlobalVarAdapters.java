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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.analysis.approval.crypto.GlobalVarCryptor.GlobalVar;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把各操作类型的全局变量结构适配为 {@link GlobalVar} 视图。
 * <p>
 * 适配器直接读写原对象，加解密的结果会落回请求体本身，不产生副本。
 */
public final class GlobalVarAdapters {

    private GlobalVarAdapters() {
    }

    /**
     * 启动执行方案、保存定时任务用的全局变量结构，可按 ID 或名称定位变量
     */
    public static List<GlobalVar> ofGlobalVars(List<V4GlobalVarDTO> globalVars) {
        if (CollectionUtils.isEmpty(globalVars)) {
            return Collections.emptyList();
        }
        List<GlobalVar> adapters = new ArrayList<>(globalVars.size());
        for (V4GlobalVarDTO globalVar : globalVars) {
            if (globalVar != null) {
                adapters.add(new GlobalVarDtoAdapter(globalVar));
            }
        }
        return adapters;
    }

    /**
     * 创建执行方案用的变量覆盖项，只能按名称定位模板变量
     */
    public static List<GlobalVar> ofPlanVariables(List<V4JobPlanVariableItem> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            return Collections.emptyList();
        }
        List<GlobalVar> adapters = new ArrayList<>(variables.size());
        for (V4JobPlanVariableItem variable : variables) {
            if (variable != null) {
                adapters.add(new PlanVariableAdapter(variable));
            }
        }
        return adapters;
    }

    private static class GlobalVarDtoAdapter implements GlobalVar {

        private final V4GlobalVarDTO globalVar;

        GlobalVarDtoAdapter(V4GlobalVarDTO globalVar) {
            this.globalVar = globalVar;
        }

        @Override
        public Long getVarId() {
            return globalVar.getId();
        }

        @Override
        public String getVarName() {
            return globalVar.getName();
        }

        @Override
        public String getValue() {
            return globalVar.getValue();
        }

        @Override
        public void setValue(String value) {
            globalVar.setValue(value);
        }
    }

    private static class PlanVariableAdapter implements GlobalVar {

        private final V4JobPlanVariableItem variable;

        PlanVariableAdapter(V4JobPlanVariableItem variable) {
            this.variable = variable;
        }

        @Override
        public Long getVarId() {
            return null;
        }

        @Override
        public String getVarName() {
            return variable.getName();
        }

        @Override
        public String getValue() {
            return variable.getValue();
        }

        @Override
        public void setValue(String value) {
            variable.setValue(value);
        }
    }
}
