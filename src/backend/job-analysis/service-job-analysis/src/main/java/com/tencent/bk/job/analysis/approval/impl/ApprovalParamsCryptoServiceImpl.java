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

package com.tencent.bk.job.analysis.approval.impl;

import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptorRegistry;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 按操作类型把参数快照与请求对象互转，转换过程中完成敏感字段的加密与还原。
 * <p>
 * 加密作用在<b>反序列化出来的强类型请求对象</b>上，而不是序列化后的 JSON 文本：
 * 哪个字段敏感由字段本身决定，不依赖对 JSON 树的路径匹配，新增字段时不会出现"忘了登记路径"的静默漏加密。
 */
@Slf4j
@Service
public class ApprovalParamsCryptoServiceImpl implements ApprovalParamsCryptoService {

    private final ApprovalParamsCryptorRegistry cryptorRegistry;

    public ApprovalParamsCryptoServiceImpl(ApprovalParamsCryptorRegistry cryptorRegistry) {
        this.cryptorRegistry = cryptorRegistry;
    }

    @Override
    public String encryptToSnapshot(ApprovalOperationTypeEnum operationType, Object params) {
        if (params == null) {
            return null;
        }
        // 加密改写的是请求对象本身，先深拷贝一份，避免污染调用方后续要用的对象
        ApprovalParamsCryptor<?> cryptor = cryptorRegistry.getCryptor(operationType);
        Object copy = copy(params, cryptor.getParamsClass(), operationType);
        // 加密失败让异常向上传播，绝不降级为明文落库
        encrypt(cryptor, copy);
        return JsonUtils.toJson(copy);
    }

    @Override
    public Object decryptFromSnapshot(ApprovalOperationTypeEnum operationType, String snapshot) {
        ApprovalParamsCryptor<?> cryptor = cryptorRegistry.getCryptor(operationType);
        Object params = parse(snapshot, cryptor.getParamsClass(), operationType);
        decrypt(cryptor, params);
        return params;
    }

    @Override
    public ApprovalDisplayParams desensitizeFromSnapshot(ApprovalOperationTypeEnum operationType, String snapshot) {
        ApprovalParamsCryptor<?> cryptor = cryptorRegistry.getCryptor(operationType);
        Object params = parse(snapshot, cryptor.getParamsClass(), operationType);
        return desensitize(cryptor, params);
    }

    @SuppressWarnings("unchecked")
    private <T> void encrypt(ApprovalParamsCryptor<T> cryptor, Object params) {
        cryptor.encrypt((T) params);
    }

    @SuppressWarnings("unchecked")
    private <T> void decrypt(ApprovalParamsCryptor<T> cryptor, Object params) {
        cryptor.decrypt((T) params);
    }

    @SuppressWarnings("unchecked")
    private <T> ApprovalDisplayParams desensitize(ApprovalParamsCryptor<T> cryptor, Object params) {
        return cryptor.desensitize((T) params);
    }

    private <T> T copy(Object params, Class<T> paramsClass, ApprovalOperationTypeEnum operationType) {
        if (!paramsClass.isInstance(params)) {
            throw new IllegalArgumentException("Params type mismatch for operationType " + operationType
                + ", expect " + paramsClass.getName() + " but got " + params.getClass().getName());
        }
        return parse(JsonUtils.toJson(params), paramsClass, operationType);
    }

    private <T> T parse(String snapshot, Class<T> paramsClass, ApprovalOperationTypeEnum operationType) {
        if (StringUtils.isEmpty(snapshot)) {
            throw new InternalException(
                "Empty approval operation params, operationType=" + operationType, ErrorCode.INTERNAL_ERROR);
        }
        T params = JsonUtils.fromJson(snapshot, paramsClass);
        if (params == null) {
            // fail-closed：解析不了就不放行，绝不把未加密的内容原样落库、也不拿可疑内容去执行
            throw new InternalException(
                "Parse approval operation params failed, operationType=" + operationType, ErrorCode.INTERNAL_ERROR);
        }
        return params;
    }
}
