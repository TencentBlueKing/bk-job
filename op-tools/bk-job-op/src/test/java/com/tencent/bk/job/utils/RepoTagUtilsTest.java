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
import com.tencent.bk.job.model.TagParseStatusEnum;
import com.tencent.bk.job.model.TagSeriesDTO;
import com.tencent.bk.job.model.TagTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("仓库Tag工具类测试")
class RepoTagUtilsTest {

    /**
     * 按Tag字面值构造结构化Tag，用于组装"库中已有Tag"的场景
     */
    private static List<RepoTagDTO> tagsOf(String... tags) {
        List<RepoTagDTO> list = new ArrayList<>();
        for (String tag : tags) {
            TagParseResult result = RepoTagUtils.parse(tag);
            assertEquals(TagParseStatusEnum.VALID, result.getStatus(), "测试数据必须是纳管Tag: " + tag);
            list.add(result.getTag());
        }
        return list;
    }

    private static NextTagResp nextTagOf(String series, String... existingTags) {
        TagSeriesDTO parsedSeries = RepoTagUtils.parseSeries(series, true);
        assertNotNull(parsedSeries, "测试数据必须是合法系列: " + series);
        return RepoTagUtils.nextTag(parsedSeries, tagsOf(existingTags));
    }

    @Nested
    @DisplayName("Tag解析与归一化")
    class ParseTest {

        @ParameterizedTest(name = "{0} 归一化为 v3.10.1")
        @ValueSource(strings = {"V3.10.1", "v3.10.1", "3.10.1", " v3.10.1 ", " V3.10.1"})
        @DisplayName("v前缀大小写不敏感，允许首尾空白")
        void testNormalizeStableTag(String rawTag) {
            TagParseResult result = RepoTagUtils.parse(rawTag);
            assertEquals(TagParseStatusEnum.VALID, result.getStatus());
            assertEquals("v3.10.1", result.getTag().getTag());
            assertEquals(3, result.getTag().getMajor());
            assertEquals(10, result.getTag().getMinor());
            assertEquals(1, result.getTag().getPatch());
            assertEquals("", result.getTag().getPreType());
            assertEquals(TagTypeEnum.STABLE.getRank(), result.getTag().getPreRank());
            assertEquals(0, result.getTag().getPreNum());
        }

        @Test
        @DisplayName("先行版归一化并拆出类型序与先行号")
        void testNormalizePreReleaseTag() {
            TagParseResult result = RepoTagUtils.parse("V3.10.3-alpha.11");
            assertEquals(TagParseStatusEnum.VALID, result.getStatus());
            assertEquals("v3.10.3-alpha.11", result.getTag().getTag());
            assertEquals("alpha", result.getTag().getPreType());
            assertEquals(TagTypeEnum.ALPHA.getRank(), result.getTag().getPreRank());
            assertEquals(11, result.getTag().getPreNum());
        }

        @ParameterizedTest(name = "{0} 判为非法")
        @ValueSource(strings = {
            "3.10.x", "v3.10.01", "v3.10", "3", "", "  ", "v3.10.1-alpha", "v3.10.1-alpha.", "vv3.10.1",
            "v3.10.1-alpha.01", "v3.10.1.alpha.1", "v-3.10.1", "3.10.1-", "release-3.10.1"
        })
        @DisplayName("格式非法的Tag判为INVALID")
        void testInvalidTags(String rawTag) {
            assertEquals(TagParseStatusEnum.INVALID, RepoTagUtils.parse(rawTag).getStatus());
        }

        @Test
        @DisplayName("null判为非法且不抛异常")
        void testNullTag() {
            TagParseResult result = RepoTagUtils.parse(null);
            assertEquals(TagParseStatusEnum.INVALID, result.getStatus());
            assertNull(result.getRawTag());
        }

        @ParameterizedTest(name = "{0} 判为非法：先行号必须从1开始")
        @ValueSource(strings = {"v3.10.1-alpha.0", "v3.10.1-beta.0", "v3.10.1-rc.0"})
        @DisplayName("先行号0不被接受，首次分配从1开始")
        void testZeroPreNumIsInvalid(String rawTag) {
            TagParseResult result = RepoTagUtils.parse(rawTag);
            assertEquals(TagParseStatusEnum.INVALID, result.getStatus());
            assertTrue(result.getReason().contains("先行版本号"));
        }

        @ParameterizedTest(name = "{0} 判为非法：先行标识大小写敏感")
        @ValueSource(strings = {"v3.10.1-Alpha.1", "v3.10.1-ALPHA.1", "V3.10.1-Beta.2", "3.10.1-RC.1"})
        @DisplayName("先行标识区分大小写，只有v前缀豁免大小写")
        void testUpperCasePreTypeIsInvalid(String rawTag) {
            TagParseResult result = RepoTagUtils.parse(rawTag);
            assertEquals(TagParseStatusEnum.INVALID, result.getStatus());
            assertTrue(result.getReason().contains("大小写"));
            assertEquals(rawTag, result.getRawTag(), "非法Tag应原样回传便于调用方定位");
        }

        @ParameterizedTest(name = "{0} 不纳管")
        @ValueSource(strings = {
            "v3.10.4-devgray.1", "v3.10.4-json.1", "v9.9.9-codev.10", "v3.5.0-stable.1", "v3.8.1-test.3"
        })
        @DisplayName("alpha/beta/rc之外的先行版属开发自测临时版本，不纳管")
        void testUnmanagedPreReleaseTags(String rawTag) {
            assertEquals(TagParseStatusEnum.IGNORED, RepoTagUtils.parse(rawTag).getStatus());
        }

        @ParameterizedTest(name = "{0} 归入不纳管而非非法")
        @ValueSource(strings = {"v3.3.4.1", "v3.4.1.0", "v3.5.0.21", "3.3.4.1"})
        @DisplayName("四段式历史Tag是已知遗留形态，归入IGNORED避免每次全量同步产生告警噪声")
        void testLegacyFourSegmentTags(String rawTag) {
            TagParseResult result = RepoTagUtils.parse(rawTag);
            assertEquals(TagParseStatusEnum.IGNORED, result.getStatus());
            assertTrue(result.getReason().contains("四段式"));
        }
    }

    @Nested
    @DisplayName("语义序比较")
    class ComparatorTest {

        @Test
        @DisplayName("同一修订号内 alpha < beta < rc < stable")
        void testTypeOrderWithinSamePatch() {
            List<RepoTagDTO> tags = tagsOf("v3.10.3", "v3.10.3-rc.1", "v3.10.3-alpha.1", "v3.10.3-beta.1");
            assertEquals(
                Arrays.asList("v3.10.3-alpha.1", "v3.10.3-beta.1", "v3.10.3-rc.1", "v3.10.3"),
                RepoTagUtils.toSortedTagNames(tags, true));
        }

        @Test
        @DisplayName("版本号按数值比较，v3.9.20 < v3.10.0（字典序会判反）")
        void testNumericVersionOrder() {
            List<RepoTagDTO> tags = tagsOf("v3.10.0", "v3.9.20", "v3.9.2");
            assertEquals(Arrays.asList("v3.9.2", "v3.9.20", "v3.10.0"),
                RepoTagUtils.toSortedTagNames(tags, true));
        }

        @Test
        @DisplayName("先行号按数值比较，alpha.2 < alpha.11 < alpha.64（字典序会判反）")
        void testNumericPreNumOrder() {
            List<RepoTagDTO> tags = tagsOf("v3.7.0-alpha.64", "v3.7.0-alpha.9", "v3.7.0-alpha.11",
                "v3.7.0-alpha.2");
            assertEquals(Arrays.asList("v3.7.0-alpha.2", "v3.7.0-alpha.9", "v3.7.0-alpha.11", "v3.7.0-alpha.64"),
                RepoTagUtils.toSortedTagNames(tags, true));
        }

        @Test
        @DisplayName("降序为默认展示顺序，与升序完全相反")
        void testDescOrder() {
            List<RepoTagDTO> tags = tagsOf("v3.10.3-alpha.1", "v3.10.3-beta.1", "v3.10.3");
            List<String> asc = RepoTagUtils.toSortedTagNames(tags, true);
            List<String> desc = RepoTagUtils.toSortedTagNames(tags, false);
            List<String> reversedDesc = new ArrayList<>(desc);
            Collections.reverse(reversedDesc);
            assertEquals(asc, reversedDesc);
            assertEquals("v3.10.3", desc.get(0));
        }
    }

    @Nested
    @DisplayName("版本前缀解析")
    class ParseSeriesTest {

        @Test
        @DisplayName("查询前缀支持x、x.y、x.y.z、x.y.z-{type}四种形态")
        void testFourPrefixForms() {
            TagSeriesDTO major = RepoTagUtils.parseSeries("3", false);
            assertEquals(3, major.getMajor());
            assertNull(major.getMinor());

            TagSeriesDTO minor = RepoTagUtils.parseSeries("3.10", false);
            assertEquals(10, minor.getMinor());
            assertNull(minor.getPatch());

            TagSeriesDTO patch = RepoTagUtils.parseSeries("V3.10.1", false);
            assertEquals(1, patch.getPatch());
            assertNull(patch.getType());

            TagSeriesDTO preRelease = RepoTagUtils.parseSeries("v3.10.1-alpha", false);
            assertEquals(TagTypeEnum.ALPHA, preRelease.getType());
            assertEquals("3.10.1-alpha", preRelease.toNormalizedString());
        }

        @Test
        @DisplayName("前缀3.1按结构化列匹配，不会命中3.10.x")
        void testPrefixDoesNotMatchLongerMinor() {
            TagSeriesDTO series = RepoTagUtils.parseSeries("3.1", false);
            assertEquals(1, series.getMinor());
            assertEquals("3.1", series.toNormalizedString());
        }

        @ParameterizedTest(name = "{0} 的三种写法等价")
        @ValueSource(strings = {"3.10.1-alpha", "v3.10.1-alpha", "V3.10.1-alpha"})
        @DisplayName("查询侧入参的v/V/无前缀三种写法等价")
        void testPrefixCaseEquivalence(String prefix) {
            assertEquals("3.10.1-alpha", RepoTagUtils.parseSeries(prefix, false).toNormalizedString());
        }

        @ParameterizedTest(name = "{0} 无法解析")
        @ValueSource(strings = {
            "", "  ", "3.10.x", "3.10.01", "3.10.1-devgray", "3-alpha", "3.10-alpha", "3.10.1-Alpha",
            "3.10.1.1", "abc"
        })
        @DisplayName("非法前缀返回null")
        void testInvalidPrefix(String prefix) {
            assertNull(RepoTagUtils.parseSeries(prefix, false));
        }

        @Test
        @DisplayName("求下一个Tag时必须给到修订号，两位版本号属真歧义")
        void testRequireFullVersion() {
            assertNull(RepoTagUtils.parseSeries("3.10", true));
            assertNull(RepoTagUtils.parseSeries("3", true));
            assertNull(RepoTagUtils.parseSeries(null, true));
            assertNotNull(RepoTagUtils.parseSeries("3.10.1", true));
            assertNotNull(RepoTagUtils.parseSeries("v3.10.1-rc", true));
        }
    }

    @Nested
    @DisplayName("求下一个Tag：先行版分支")
    class NextPreReleaseTagTest {

        @Test
        @DisplayName("先行号跨9不进位：alpha.9的下一个是alpha.10")
        void testPreNumAcrossTen() {
            assertEquals("v3.10.3-alpha.10",
                nextTagOf("3.10.3-alpha", "v3.10.3-alpha.8", "v3.10.3-alpha.9").getNextTag());
        }

        @Test
        @DisplayName("先行号按整数取最大：alpha.2与alpha.11共存时下一个是alpha.12")
        void testPreNumIntegerComparison() {
            NextTagResp resp = nextTagOf("3.10.3-alpha", "v3.10.3-alpha.2", "v3.10.3-alpha.11");
            assertEquals("v3.10.3-alpha.11", resp.getCurrentMaxTag());
            assertEquals("v3.10.3-alpha.12", resp.getNextTag());
        }

        @Test
        @DisplayName("真实数据中的alpha.64同样按整数取最大")
        void testLargePreNum() {
            assertEquals("v3.7.0-alpha.65",
                nextTagOf("3.7.0-alpha", "v3.7.0-alpha.9", "v3.7.0-alpha.64").getNextTag());
        }

        @Test
        @DisplayName("先行号不连续时取max+1，不补历史空洞")
        void testPreNumGapNotFilled() {
            assertEquals("v3.8.1-beta.10",
                nextTagOf("3.8.1-beta", "v3.8.1-beta.7", "v3.8.1-beta.9").getNextTag());
        }

        @Test
        @DisplayName("空系列首次分配为.1")
        void testEmptyPreReleaseSeries() {
            NextTagResp resp = nextTagOf("3.10.3-alpha");
            assertTrue(resp.isResult());
            assertEquals("3.10.3-alpha", resp.getSeries());
            assertNull(resp.getCurrentMaxTag());
            assertEquals("v3.10.3-alpha.1", resp.getNextTag());
        }

        @Test
        @DisplayName("只取同类型子集，不被同修订号的其他类型干扰")
        void testOnlySameTypeCounted() {
            NextTagResp resp = nextTagOf("3.10.3-beta", "v3.10.3-alpha.30", "v3.10.3-beta.2", "v3.10.3-rc.9");
            assertEquals("v3.10.3-beta.2", resp.getCurrentMaxTag());
            assertEquals("v3.10.3-beta.3", resp.getNextTag());
        }

        @Test
        @DisplayName("只取同修订号子集，不被同小版本的其他修订号干扰")
        void testOnlySamePatchCounted() {
            NextTagResp resp = nextTagOf("3.10.3-alpha", "v3.10.7-alpha.20", "v3.10.3-alpha.2");
            assertEquals("v3.10.3-alpha.3", resp.getNextTag());
        }
    }

    @Nested
    @DisplayName("求下一个Tag：稳定版分支")
    class NextStableTagTest {

        @Test
        @DisplayName("系列内完全无Tag时返回入参本身")
        void testEmptySeries() {
            NextTagResp resp = nextTagOf("3.10.3");
            assertEquals("3.10", resp.getSeries(), "稳定版分支的系列粒度是x.y");
            assertNull(resp.getCurrentMaxTag());
            assertEquals("v3.10.3", resp.getNextTag());
            assertNull(resp.getMessage());
        }

        @Test
        @DisplayName("只有rc没有稳定版时返回入参本身，rc灰度通过后转正为同号稳定版")
        void testOnlyRcExists() {
            NextTagResp resp = nextTagOf("3.10.3", "v3.10.3-rc.1", "v3.10.3-alpha.5");
            assertNull(resp.getCurrentMaxTag());
            assertEquals("v3.10.3", resp.getNextTag());
        }

        @Test
        @DisplayName("稳定版已存在时返回下一个修订号")
        void testStableExists() {
            NextTagResp resp = nextTagOf("3.10.3", "v3.10.3", "v3.10.3-rc.1");
            assertEquals("v3.10.3", resp.getCurrentMaxTag());
            assertEquals("v3.10.4", resp.getNextTag());
            assertNull(resp.getMessage(), "正常递增不应产生顺延提示");
        }

        @Test
        @DisplayName("入参修订号落后于系列现状时顺延，并在message中提示调用方")
        void testBumpToAvoidOccupiedTag() {
            NextTagResp resp = nextTagOf("3.10.3", "v3.10.3", "v3.10.4", "v3.10.7");
            assertEquals("v3.10.7", resp.getCurrentMaxTag());
            assertEquals("v3.10.8", resp.getNextTag());
            assertNotNull(resp.getMessage());
            assertTrue(resp.getMessage().contains("v3.10.8"));
        }

        @Test
        @DisplayName("入参修订号更大时尊重入参")
        void testRespectLargerInputPatch() {
            NextTagResp resp = nextTagOf("3.10.9", "v3.10.3");
            assertEquals("v3.10.9", resp.getNextTag());
            assertNull(resp.getMessage());
        }

        @Test
        @DisplayName("修订号无上限且不向小版本进位：v3.10.9的下一个是v3.10.10")
        void testPatchAcrossTenNoMinorCarry() {
            NextTagResp resp = nextTagOf("3.10.9", "v3.10.8", "v3.10.9");
            assertEquals("v3.10.10", resp.getNextTag());
        }

        @Test
        @DisplayName("稳定版取最大时忽略先行版")
        void testPreReleaseIgnoredInStableBranch() {
            NextTagResp resp = nextTagOf("3.10.1", "v3.10.1", "v3.10.9-alpha.1", "v3.10.9-rc.3");
            assertEquals("v3.10.1", resp.getCurrentMaxTag());
            assertEquals("v3.10.2", resp.getNextTag());
        }

        @Test
        @DisplayName("入参的v/V/无前缀三种写法结果一致")
        void testSeriesCaseEquivalence() {
            assertEquals("v3.10.4", nextTagOf("3.10.3", "v3.10.3").getNextTag());
            assertEquals("v3.10.4", nextTagOf("v3.10.3", "v3.10.3").getNextTag());
            assertEquals("v3.10.4", nextTagOf("V3.10.3", "v3.10.3").getNextTag());
        }

        @Test
        @DisplayName("传入空Tag列表不抛异常")
        void testNullTagList() {
            NextTagResp resp = RepoTagUtils.nextTag(RepoTagUtils.parseSeries("3.10.3", true), null);
            assertTrue(resp.isResult());
            assertEquals("v3.10.3", resp.getNextTag());
        }
    }
}
