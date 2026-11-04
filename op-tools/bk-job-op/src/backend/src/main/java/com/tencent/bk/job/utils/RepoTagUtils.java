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

package com.tencent.bk.job.utils;

import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.RepoTagDTO;
import com.tencent.bk.job.model.TagParseResult;
import com.tencent.bk.job.model.TagSeriesDTO;
import com.tencent.bk.job.model.TagTypeEnum;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 仓库Tag工具类，承载Tag的归一化、解析、分类、语义比较与求下一个Tag的全部纯逻辑
 * <p>
 * 纳管范围：{@code v{major}.{minor}.{patch}} 稳定版，以及 {@code -alpha.N}/{@code -beta.N}/{@code -rc.N} 三类先行版。
 * 其余先行版（如-devgray.N、-json.N）是开发自测的临时版本，不纳入Tag管理。
 * <p>
 * 大小写规则：只有v前缀不区分大小写（{@code V3.10.1}与{@code v3.10.1}等价），
 * 先行标识区分大小写，{@code v3.10.1-Alpha.1} 判为非法。
 */
public class RepoTagUtils {

    /**
     * 批量写入/删除接口的单批Tag数量上限，超限直接拒绝而不截断，避免调用方误以为写完了
     */
    public static final int MAX_BATCH_SIZE = 1000;

    /**
     * 查询接口单次返回的Tag数量上限。命中数超限时截断返回并置truncated标记，
     * 查询侧截断不会造成数据丢失，而拒绝会让宽前缀（如prefix=3）直接不可用
     */
    public static final int MAX_QUERY_RESULT_SIZE = 1000;

    /**
     * 单个Tag的最大长度
     */
    private static final int MAX_TAG_LENGTH = 64;

    /**
     * 系列内首个先行版本号，先行号从1开始分配，不接受0
     */
    private static final int FIRST_PRE_NUM = 1;

    /**
     * 版本号单段的取值：不含前导零，最长9位（避免Integer溢出）
     */
    private static final String NUM = "(0|[1-9]\\d{0,8})";

    /**
     * 合法Tag：先行标识必须小写，先行号必须是从1开始、不含前导零的正整数
     */
    private static final Pattern TAG_PATTERN = Pattern.compile(
        "^[vV]?" + NUM + "\\." + NUM + "\\." + NUM + "(?:-([a-z]+)\\.([1-9]\\d{0,8}))?$");

    /**
     * 3.3~3.5时代遗留的四段式历史Tag，如v3.3.4.1，识别为已知遗留形态而非写错
     */
    private static final Pattern LEGACY_FOUR_SEGMENT_PATTERN = Pattern.compile(
        "^[vV]?" + NUM + "\\." + NUM + "\\." + NUM + "\\." + NUM + "$");

    /**
     * 形似先行版但未通过校验的Tag，用于给出更具体的非法原因
     */
    private static final Pattern PRE_RELEASE_DIAGNOSTIC_PATTERN = Pattern.compile(
        "^[vV]?" + NUM + "\\." + NUM + "\\." + NUM + "-([A-Za-z]+)\\.(\\d{1,9})$");

    /**
     * 版本前缀：支持x、x.y、x.y.z、x.y.z-{type}四种形态
     */
    private static final Pattern SERIES_PATTERN = Pattern.compile(
        "^[vV]?" + NUM + "(?:\\." + NUM + ")?(?:\\." + NUM + ")?(?:-([a-z]+))?$");

    /**
     * Tag语义序比较器：先按主/小/修订版本号数值比较，再按类型序（alpha&lt;beta&lt;rc&lt;stable），
     * 最后按先行号数值比较。全程整数比较，字典序会把alpha.9判为大于alpha.11、把v3.9.20判为大于v3.10.0
     */
    public static final Comparator<RepoTagDTO> SEMANTIC_COMPARATOR = Comparator
        .comparingInt(RepoTagDTO::getMajor)
        .thenComparingInt(RepoTagDTO::getMinor)
        .thenComparingInt(RepoTagDTO::getPatch)
        .thenComparingInt(RepoTagDTO::getPreRank)
        .thenComparingInt(RepoTagDTO::getPreNum);

    private RepoTagUtils() {
    }

    /**
     * 解析单个Tag
     *
     * @param rawTag 调用方传入的原始Tag，可带v/V前缀，允许首尾空白
     * @return 解析结果，包含状态、原始值与结构化Tag
     */
    public static TagParseResult parse(String rawTag) {
        if (rawTag == null) {
            return TagParseResult.invalid(null, "Tag不能为空");
        }
        String trimmed = rawTag.trim();
        if (trimmed.isEmpty()) {
            return TagParseResult.invalid(rawTag, "Tag不能为空");
        }
        if (trimmed.length() > MAX_TAG_LENGTH) {
            return TagParseResult.invalid(rawTag, "Tag长度超过" + MAX_TAG_LENGTH);
        }
        Matcher matcher = TAG_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            if (LEGACY_FOUR_SEGMENT_PATTERN.matcher(trimmed).matches()) {
                return TagParseResult.ignored(rawTag, "四段式历史Tag，不纳入Tag管理");
            }
            return TagParseResult.invalid(rawTag, diagnose(trimmed));
        }
        TagTypeEnum type = TagTypeEnum.ofPreType(matcher.group(4));
        if (type == null) {
            return TagParseResult.ignored(rawTag, "先行版本类型" + matcher.group(4) + "不在纳管范围内，仅纳管alpha/beta/rc");
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        int preNum = type == TagTypeEnum.STABLE ? 0 : Integer.parseInt(matcher.group(5));
        RepoTagDTO tag = new RepoTagDTO(buildTag(major, minor, patch, type, preNum),
            major, minor, patch, type.getPreType(), type.getRank(), preNum);
        return TagParseResult.valid(rawTag, tag);
    }

    /**
     * 拼装归一化Tag，统一使用小写v前缀
     *
     * @param major  主版本号
     * @param minor  小版本号
     * @param patch  修订号
     * @param type   Tag类型
     * @param preNum 先行版本号，稳定版忽略该参数
     * @return 归一化Tag
     */
    public static String buildTag(int major, int minor, int patch, TagTypeEnum type, int preNum) {
        String base = "v" + major + "." + minor + "." + patch;
        if (type == null || type == TagTypeEnum.STABLE) {
            return base;
        }
        return base + "-" + type.getPreType() + "." + preNum;
    }

    /**
     * 解析版本前缀
     *
     * @param input              版本前缀，可带v/V前缀，允许首尾空白
     * @param requireFullVersion 是否必须给到修订号。求下一个Tag时必须为true：
     *                           两位版本号（如3.10）的"下一个"无法判定是下一个修订号还是下一个小版本，属真歧义
     * @return 解析出的版本前缀；无法解析时返回null
     */
    public static TagSeriesDTO parseSeries(String input, boolean requireFullVersion) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_TAG_LENGTH) {
            return null;
        }
        Matcher matcher = SERIES_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return null;
        }
        Integer major = Integer.parseInt(matcher.group(1));
        Integer minor = matcher.group(2) == null ? null : Integer.parseInt(matcher.group(2));
        Integer patch = matcher.group(3) == null ? null : Integer.parseInt(matcher.group(3));
        String preType = matcher.group(4);
        TagTypeEnum type = null;
        if (preType != null) {
            // 先行标识只能出现在完整的三段版本号之后，且必须是纳管类型
            type = TagTypeEnum.ofPreType(preType);
            if (type == null || type == TagTypeEnum.STABLE || patch == null) {
                return null;
            }
        }
        if (requireFullVersion && patch == null) {
            return null;
        }
        return new TagSeriesDTO(major, minor, patch, type);
    }

    /**
     * 求某版本系列的下一个Tag
     * <p>
     * "系列"在两个分支下的粒度不同，这是本接口最容易被误解的地方：
     * <ul>
     *     <li>先行版分支：系列 = x.y.z-{type}，在同一个修订号内取最大先行号+1，空系列首次分配为.1</li>
     *     <li>稳定版分支：系列 = x.y，取该小版本下全部稳定版的最大修订号，返回
     *     {@code v{x}.{y}.{max(入参修订号, 最大稳定版修订号+1)}}；该小版本尚无稳定版时返回入参本身</li>
     * </ul>
     * 稳定版分支之所以按x.y而不是x.y.z取最大，是为了不返回一个已被占用的Tag：已有v3.10.7时传3.10.3
     * 会返回v3.10.8而不是v3.10.4。修订号无上限也不向小版本进位，v3.10.9的下一个是v3.10.10而非v3.11.0，
     * 因为切小版本是拉新版本分支的人工决策，不应由算法隐式触发。
     * <p>
     * 本方法只计算建议值，不做预占，也不加锁：Tag的权威唯一性由Git打Tag那一步保证，加锁挡不住
     * 有人绕过接口直接git tag。repo_tag表只是Git Tag的事后镜像，不承担冲突保护职责。
     *
     * @param series            已解析的版本系列，修订号必须非空
     * @param tagsInMinorSeries 与series同属一个x.y小版本的全部Tag，两个分支所需的子集都从中筛出
     * @return 下一个Tag的计算结果
     */
    public static NextTagResp nextTag(TagSeriesDTO series, List<RepoTagDTO> tagsInMinorSeries) {
        int major = series.getMajor();
        int minor = series.getMinor();
        int patch = series.getPatch();
        List<RepoTagDTO> tags = tagsInMinorSeries == null ? Collections.emptyList() : tagsInMinorSeries;
        NextTagResp resp = new NextTagResp();
        resp.setResult(true);

        TagTypeEnum type = series.getType();
        if (type != null) {
            resp.setSeries(series.toNormalizedString());
            RepoTagDTO maxTag = tags.stream()
                .filter(tag -> Objects.equals(tag.getPatch(), patch)
                    && Objects.equals(tag.getPreRank(), type.getRank()))
                .max(SEMANTIC_COMPARATOR)
                .orElse(null);
            int nextPreNum = maxTag == null ? FIRST_PRE_NUM : maxTag.getPreNum() + 1;
            resp.setCurrentMaxTag(maxTag == null ? null : maxTag.getTag());
            resp.setNextTag(buildTag(major, minor, patch, type, nextPreNum));
            return resp;
        }

        resp.setSeries(major + "." + minor);
        RepoTagDTO maxStableTag = tags.stream()
            .filter(tag -> Objects.equals(tag.getPreRank(), TagTypeEnum.STABLE.getRank()))
            .max(SEMANTIC_COMPARATOR)
            .orElse(null);
        if (maxStableTag == null) {
            resp.setNextTag(buildTag(major, minor, patch, TagTypeEnum.STABLE, 0));
            return resp;
        }
        resp.setCurrentMaxTag(maxStableTag.getTag());
        int nextPatch = Math.max(patch, maxStableTag.getPatch() + 1);
        resp.setNextTag(buildTag(major, minor, nextPatch, TagTypeEnum.STABLE, 0));
        if (nextPatch > patch + 1) {
            // 入参修订号已落后于系列现状，明确提示调用方，避免它默默拿到一个意料之外的号
            resp.setMessage(String.format("input patch %d is behind current max stable tag %s, bumped to %s",
                patch, maxStableTag.getTag(), resp.getNextTag()));
        }
        return resp;
    }

    /**
     * 按语义序排列并取出Tag名称
     *
     * @param tags 结构化Tag列表
     * @param asc  true为升序（最旧在前），false为降序（最新在前）
     * @return 排序后的归一化Tag名称列表
     */
    public static List<String> toSortedTagNames(List<RepoTagDTO> tags, boolean asc) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        Comparator<RepoTagDTO> comparator = asc ? SEMANTIC_COMPARATOR : SEMANTIC_COMPARATOR.reversed();
        return tags.stream().sorted(comparator).map(RepoTagDTO::getTag).collect(Collectors.toList());
    }

    /**
     * 给出更具体的非法原因，便于调用方自助排查
     */
    private static String diagnose(String trimmed) {
        Matcher matcher = PRE_RELEASE_DIAGNOSTIC_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String preType = matcher.group(4);
            if (!preType.equals(preType.toLowerCase())) {
                return "先行标识大小写敏感，必须小写：" + preType;
            }
            return "先行版本号必须是从1开始、不含前导零的正整数：" + matcher.group(5);
        }
        return "Tag格式非法，期望v{major}.{minor}.{patch}[-{alpha|beta|rc}.{N}]";
    }
}
