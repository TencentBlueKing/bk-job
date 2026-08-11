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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 按操作类型逐字段加解密参数快照。
 * <p>
 * <b>敏感字段路径必须与 6 个 v4 Request DTO 保持同步</b>：新增任何密码/密钥/脚本明文类字段时，
 * 必须同步登记到 {@link #SENSITIVE_PATHS}，并按 ApprovalParamsSchemaVersion 的规则升版本号。
 * 漏登记不会有任何编译或运行期报错，只会安静地把明文写进库里 —— 这是本类唯一的漂移风险，
 * 评审时按 DTO 字段清单逐条核对。
 */
@Slf4j
@Service
public class ApprovalParamsCryptoServiceImpl implements ApprovalParamsCryptoService {

    /**
     * 数组通配段：匹配数组中的每一个元素
     */
    private static final String ARRAY_WILDCARD = "*";

    /**
     * 各操作类型的敏感字段路径（JSON 字段名，与 DTO 的 @JsonProperty 一致）。
     * <p>
     * FAST_TRANSFER_FILE 与 UPDATE_CRON_STATUS 的 v4 请求体不含任何密码类字段
     * （账号只以 account_id / account_alias 引用，密码不随请求传输），故为空。
     * <p>
     * 另外，{@code host_password_list} 只存在于 FAST_EXECUTE_SCRIPT 的 v4 请求体上：
     * 分发文件与启动执行方案的 v4 请求体没有这个字段，因此它们的路径里也不应出现，
     * 不是漏登记。这三处与方案 §8.1 的字段表有出入，以实际 DTO 为准。
     * <p>
     * 全局变量与执行方案变量的 value 一律加密，不区分变量类型：v4 请求体里没有变量类型字段，
     * 无法在此判断是否为 CIPHER 变量，宁可多加密（对称加解密后逐字段等值还原）也不能漏掉密码类变量。
     */
    private static final Map<ApprovalOperationTypeEnum, List<List<String>>> SENSITIVE_PATHS =
        new EnumMap<>(ApprovalOperationTypeEnum.class);

    static {
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, Arrays.asList(
            Collections.singletonList("script_content"),
            Collections.singletonList("script_param"),
            Arrays.asList("host_password_list", ARRAY_WILDCARD, "encrypted_password")
        ));
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE, Collections.emptyList());
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, Collections.singletonList(
            Arrays.asList("global_var_list", ARRAY_WILDCARD, "value")
        ));
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, Collections.singletonList(
            Arrays.asList("variables", ARRAY_WILDCARD, "value")
        ));
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.SAVE_CRON, Collections.singletonList(
            Arrays.asList("global_var_list", ARRAY_WILDCARD, "value")
        ));
        SENSITIVE_PATHS.put(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, Collections.emptyList());
    }

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
        List<List<String>> paths = SENSITIVE_PATHS.get(operationType);
        if (paths == null || paths.isEmpty()) {
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
        for (List<String> path : paths) {
            changed |= transformPath(root, path, 0, encrypt);
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
        if (ARRAY_WILDCARD.equals(segment)) {
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
