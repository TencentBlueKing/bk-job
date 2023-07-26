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

package com.tencent.bk.job.crontab.model.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.crypto.scenario.CipherVariableCryptoService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 可加密变量
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EncryptEnableVariables {

    /**
     * 变量值列表
     */
    protected List<CronJobVariableDTO> variableValue;

    public String getEncryptedVariableValue() {
        CipherVariableCryptoService cipherVariableCryptoService =
            ApplicationContextRegister.getBean(CipherVariableCryptoService.class);
        if (CollectionUtils.isEmpty(this.variableValue)) {
            return JsonUtils.toJson(this.variableValue);
        }
        List<CronJobVariableDTO> cloneVariableList = new ArrayList<>(this.variableValue.size());
        for (CronJobVariableDTO cronJobVariableDTO : this.variableValue) {
            CronJobVariableDTO cloneCronJobVariable = cronJobVariableDTO.clone();
            String encryptedValue = cipherVariableCryptoService.encryptTaskVariableIfNeeded(
                cloneCronJobVariable.getType(),
                cloneCronJobVariable.getValue()
            );
            cloneCronJobVariable.setValue(encryptedValue);
            cloneVariableList.add(cloneCronJobVariable);
        }
        return JsonUtils.toJson(cloneVariableList);
    }

    public void decryptAndSetVariableValue(String encryptedVariableValue) {
        CipherVariableCryptoService cipherVariableCryptoService =
            ApplicationContextRegister.getBean(CipherVariableCryptoService.class);
        if (StringUtils.isBlank(encryptedVariableValue)) {
            this.variableValue = new ArrayList<>();
        }
        this.variableValue = JsonUtils.fromJson(
            encryptedVariableValue,
            new TypeReference<List<CronJobVariableDTO>>() {
            }
        );
        if (CollectionUtils.isEmpty(this.variableValue)) {
            return;
        }
        for (CronJobVariableDTO cronJobVariableDTO : this.variableValue) {
            String decryptedValue = cipherVariableCryptoService.decryptTaskVariableIfNeeded(
                cronJobVariableDTO.getType(),
                cronJobVariableDTO.getValue()
            );
            cronJobVariableDTO.setValue(decryptedValue);
        }
    }
}
