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

package com.tencent.bk.job.common.kms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.kms.decrypt.Decrypt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用 bk-kms-sdk 解密信封，并把 JSON 展平为配置项。
 */
final class BkKmsDecryptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private BkKmsDecryptor() {
    }

    static Map<String, String> decrypt(String privateKey, String envelope) {
        String plainJson;
        try {
            plainJson = Decrypt.decrypt(envelope, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("bk-kms-sdk decrypt failed", e);
        }
        return flatten(plainJson);
    }

    /**
     * 对象按点号展开，数组按下标展开为 key[0]、key[1]，与 Spring 读 YAML 列表的结果一致；null 跳过。
     */
    static Map<String, String> flatten(String plainJson) {
        if (plainJson == null || plainJson.isBlank() || "null".equals(plainJson.trim())) {
            return Map.of();
        }
        final JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(plainJson);
        } catch (Exception e) {
            throw new IllegalStateException("BK-KMS plaintext is not valid JSON", e);
        }
        if (!root.isObject()) {
            throw new IllegalStateException(
                "BK-KMS plaintext must be a JSON object, actual type: " + root.getNodeType());
        }
        Map<String, String> flattened = new LinkedHashMap<>();
        flattenNode("", root, flattened);
        return flattened;
    }

    private static void flattenNode(String prefix, JsonNode node, Map<String, String> out) {
        if (node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String nextKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenNode(nextKey, entry.getValue(), out);
            });
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flattenNode(prefix + "[" + i + "]", node.get(i), out);
            }
            return;
        }
        out.put(prefix, node.asText());
    }
}
