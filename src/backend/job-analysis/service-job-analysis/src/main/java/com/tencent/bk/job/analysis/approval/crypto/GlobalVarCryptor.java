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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 全局变量取值的加解密与展示脱敏。
 * <p>
 * 只有密文类型（{@code TaskVariableTypeEnum.CIPHER}）的变量取值需要保护，普通变量的取值原样落库、
 * 原样展示：审批人看得到实际取值才能判断这次操作的影响面。变量类型由 {@link CipherVarMatcher} 给出。
 * <p>
 * 启动执行方案、保存定时任务、创建执行方案三种操作的全局变量结构不同，这里通过 {@link GlobalVar}
 * 适配为统一的读写视图，加解密规则只维护一份。
 */
@Slf4j
@Service
public class GlobalVarCryptor {

    private final SensitiveValueCryptor valueCryptor;
    private final ApprovalDisplayMasker displayMasker;

    public GlobalVarCryptor(SensitiveValueCryptor valueCryptor, ApprovalDisplayMasker displayMasker) {
        this.valueCryptor = valueCryptor;
        this.displayMasker = displayMasker;
    }

    public void encrypt(List<? extends GlobalVar> globalVars, CipherVarMatcher matcher) {
        if (CollectionUtils.isEmpty(globalVars)) {
            return;
        }
        for (GlobalVar globalVar : globalVars) {
            if (StringUtils.isEmpty(globalVar.getValue())) {
                continue;
            }
            if (matcher.needEncrypt(globalVar.getVarId(), globalVar.getVarName())) {
                globalVar.setValue(valueCryptor.encrypt(globalVar.getValue()));
            }
        }
    }

    /**
     * 还原取值。
     * <p>
     * 值本身是不是密文以 {@link SensitiveValueCryptor#isCipherText} 为准，而不是以当前的变量类型为准：
     * 类型可能在审批期间被人改过，此时按类型判断会把一串密文当作取值下发执行，或对一段明文做解密而直接失败。
     * 类型与实际不符时记 WARN，正常流程下不会出现。
     */
    public void decrypt(List<? extends GlobalVar> globalVars, CipherVarMatcher matcher) {
        if (CollectionUtils.isEmpty(globalVars)) {
            return;
        }
        for (GlobalVar globalVar : globalVars) {
            String value = globalVar.getValue();
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            boolean isCipher = valueCryptor.isCipherText(value);
            if (matcher.needEncrypt(globalVar.getVarId(), globalVar.getVarName()) != isCipher) {
                log.warn("Global var type changed during approval, varId={}, varName={}, valueIsCipher={}",
                    globalVar.getVarId(), globalVar.getVarName(), isCipher);
            }
            if (isCipher) {
                globalVar.setValue(valueCryptor.decrypt(value));
            }
        }
    }

    /**
     * 展示脱敏：<b>落库时加密过的取值一律打码</b>，其余原样展示，让审批人看得到这次操作的实际影响面。
     * <p>
     * 只看值本身是不是密文，不需要回查变量类型：这样渲染审批内容不依赖 job-manage 可用，
     * 且"加密过就一定打码"这个不变式不会因为类型被改而失效。
     */
    public void desensitize(List<? extends GlobalVar> globalVars) {
        if (CollectionUtils.isEmpty(globalVars)) {
            return;
        }
        for (GlobalVar globalVar : globalVars) {
            if (valueCryptor.isCipherText(globalVar.getValue())) {
                globalVar.setValue(displayMasker.mask());
            }
        }
    }

    /**
     * 各操作类型的全局变量结构在加解密视角下的统一视图
     */
    public interface GlobalVar {

        Long getVarId();

        String getVarName();

        String getValue();

        void setValue(String value);
    }
}
