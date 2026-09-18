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

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSection;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 把完整审批内容裁成渠道可展示的简要版。
 * <p>
 * 输入是渲染器产出的结构化章节，只做：量 UTF-8 字节 → 按 kind 整段丢弃 → 必要时截断。
 * 丢弃顺序：原始参数 → 脚本内容 → 全局变量 → 逐行展示字段。
 * 一级标题与操作概要整章不丢，仍超限才按 UTF-8 字节截断并追加 i18n 后缀。
 */
@Slf4j
@Component
public class ApprovalContentSimplifier {

    static final int DEFAULT_MAX_BYTES = 20480;
    static final String TRUNCATED_I18N_KEY = "task.approval.content.value.contentSimpleTruncated";
    private static final String TRUNCATED_SUFFIX_PREFIX = "\n\n";

    private final ApprovalProperties approvalProperties;
    private final MessageI18nService i18nService;

    public ApprovalContentSimplifier(ApprovalProperties approvalProperties,
                                     MessageI18nService i18nService) {
        this.approvalProperties = approvalProperties;
        this.i18nService = i18nService;
    }

    /**
     * 基于渲染产出的结构化章节生成简要内容。
     * <p>
     * {@code null} 原样返回 {@code null}；无章节且正文为空串时原样返回空串；未超限时与全文相同。
     */
    public String simplify(ApprovalContent content) {
        if (content == null) {
            return null;
        }
        List<ApprovalContentSection> sections = content.getSections();
        if (sections != null && !sections.isEmpty()) {
            return simplify(sections);
        }
        return simplifyPlain(content.getApprovalContent());
    }

    /**
     * 按已渲染片段整段丢弃；不解析 Markdown。
     */
    public String simplify(List<ApprovalContentSection> sections) {
        if (sections == null) {
            return null;
        }
        return simplifyJoined(ApprovalContent.joinMarkdown(sections), new ArrayList<>(sections));
    }

    private String simplifyPlain(String approvalContent) {
        if (approvalContent == null || approvalContent.isEmpty()) {
            return approvalContent;
        }
        return simplifyJoined(approvalContent, null);
    }

    private String simplifyJoined(String full, List<ApprovalContentSection> remaining) {
        if (full == null || full.isEmpty()) {
            return full;
        }
        int maxBytes = resolveMaxBytes();
        if (utf8Len(full) <= maxBytes) {
            return full;
        }
        String kept = full;
        if (remaining != null) {
            while (utf8Len(kept) > maxBytes) {
                int dropIndex = indexOfSectionToDrop(remaining);
                if (dropIndex < 0) {
                    break;
                }
                remaining.remove(dropIndex);
                kept = ApprovalContent.joinMarkdown(remaining);
            }
            if (utf8Len(kept) <= maxBytes) {
                return kept;
            }
        }
        return truncateWithSuffix(kept, maxBytes);
    }

    private int resolveMaxBytes() {
        Integer configured = approvalProperties.getContentSimpleMaxBytes();
        if (configured == null || configured <= 0) {
            log.warn("Invalid job.analysis.approval.contentSimpleMaxBytes={}, fallback to {}",
                configured, DEFAULT_MAX_BYTES);
            return DEFAULT_MAX_BYTES;
        }
        return configured;
    }

    /**
     * 从文档后往前，丢掉当前保留优先级最低的一章。TITLE / SUMMARY 整章不丢，交给截断。
     */
    private static int indexOfSectionToDrop(List<ApprovalContentSection> sections) {
        int bestIndex = -1;
        int bestDiscardOrder = 0;
        for (int i = sections.size() - 1; i >= 0; i--) {
            ApprovalContentSection section = sections.get(i);
            if (section == null || section.getKind() == null || !section.getKind().isDroppable()) {
                continue;
            }
            int discardOrder = section.getKind().getDiscardOrder();
            if (discardOrder > bestDiscardOrder) {
                bestDiscardOrder = discardOrder;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private String truncateWithSuffix(String kept, int maxBytes) {
        String suffix = TRUNCATED_SUFFIX_PREFIX + i18nService.getI18n(TRUNCATED_I18N_KEY);
        int suffixBytes = utf8Len(suffix);
        if (suffixBytes > maxBytes) {
            return truncateToUtf8Bytes(kept, maxBytes);
        }
        return truncateToUtf8Bytes(kept, maxBytes - suffixBytes) + suffix;
    }

    static int utf8Len(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * 截到不超过 {@code maxBytes} 的最长合法 UTF-8 前缀，不会切出半个字符。
     */
    static String truncateToUtf8Bytes(String text, int maxBytes) {
        if (text == null || maxBytes <= 0) {
            return "";
        }
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return text;
        }
        int endExclusive = maxBytes;
        while (endExclusive > 0 && (bytes[endExclusive] & 0xC0) == 0x80) {
            endExclusive--;
        }
        return new String(bytes, 0, endExclusive, StandardCharsets.UTF_8);
    }
}
