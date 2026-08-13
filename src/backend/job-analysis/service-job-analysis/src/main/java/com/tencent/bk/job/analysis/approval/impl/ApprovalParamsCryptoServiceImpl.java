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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.ApprovalSensitiveFields;
import com.tencent.bk.job.analysis.approval.ApprovalSensitiveFields.SensitiveField;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按操作类型逐字段加解密参数快照。
 * <p>
 * 敏感字段路径取自 {@link ApprovalSensitiveFields}，与审批内容的脱敏共用同一份登记，
 * 避免"库里加密了、审批内容里明文展示"这类不会报错的漂移。
 */
@Slf4j
@Service
public class ApprovalParamsCryptoServiceImpl implements ApprovalParamsCryptoService {

    private final SymmetricCryptoService symmetricCryptoService;

    public ApprovalParamsCryptoServiceImpl(SymmetricCryptoService symmetricCryptoService) {
        this.symmetricCryptoService = symmetricCryptoService;
    }

    @Override
    public String encryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
        return transform(operationType, paramsJson, true);
    }

    @Override
    public String decryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
        return transform(operationType, paramsJson, false);
    }

    private String transform(ApprovalOperationTypeEnum operationType, String paramsJson, boolean encrypt) {
        if (StringUtils.isEmpty(paramsJson)) {
            return paramsJson;
        }
        List<SensitiveField> fields = ApprovalSensitiveFields.of(operationType);
        if (fields.isEmpty()) {
            return paramsJson;
        }
        JsonNode root = JsonUtils.toJsonNode(paramsJson);
        if (root == null) {
            // fail-closed：解析不了就不放行，绝不把未加密的内容原样落库、也不拿可疑内容去执行
            throw new InternalException(
                "Parse approval operation params failed, operationType=" + operationType.name(),
                ErrorCode.INTERNAL_ERROR
            );
        }
        boolean changed = false;
        for (SensitiveField field : fields) {
            changed |= transformPath(root, field.getPath(), 0, encrypt);
        }
        return changed ? JsonUtils.toJson(root) : paramsJson;
    }

    /**
     * 沿路径下钻，对末段的文本叶子做加解密
     *
     * @return 是否发生了改写
     */
    private boolean transformPath(JsonNode node, List<String> path, int depth, boolean encrypt) {
        if (node == null || node.isNull()) {
            return false;
        }
        String segment = path.get(depth);
        boolean lastSegment = depth == path.size() - 1;
        if (ApprovalSensitiveFields.ARRAY_WILDCARD.equals(segment)) {
            if (!(node instanceof ArrayNode)) {
                return false;
            }
            boolean changed = false;
            for (JsonNode element : node) {
                changed |= transformPath(element, path, depth + 1, encrypt);
            }
            return changed;
        }
        if (!(node instanceof ObjectNode)) {
            return false;
        }
        ObjectNode objectNode = (ObjectNode) node;
        if (!lastSegment) {
            return transformPath(objectNode.get(segment), path, depth + 1, encrypt);
        }
        JsonNode leaf = objectNode.get(segment);
        if (leaf == null || !leaf.isTextual()) {
            return false;
        }
        String value = leaf.asText();
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        objectNode.put(segment, encrypt ? encryptValue(value) : decryptValue(value));
        return true;
    }

    private String encryptValue(String plainText) {
        // 加密失败让异常向上传播：发起接口报错好过把明文写进库里
        return symmetricCryptoService.encryptToBase64Str(plainText, CryptoScenarioEnum.APPROVAL_PARAMS_SNAPSHOT);
    }

    private String decryptValue(String cipherText) {
        String algorithm = CryptorMetaUtil.getCryptorNameFromCipher(cipherText);
        if (StringUtils.isBlank(algorithm)) {
            algorithm = CryptorNames.NONE;
        }
        return symmetricCryptoService.decrypt(cipherText, algorithm);
    }
}
