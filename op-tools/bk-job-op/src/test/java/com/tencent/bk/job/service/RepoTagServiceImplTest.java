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

package com.tencent.bk.job.service;

import com.tencent.bk.job.dao.RepoTagMapper;
import com.tencent.bk.job.model.BatchAddTagResp;
import com.tencent.bk.job.model.BatchDeleteTagResp;
import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.RepoTagDTO;
import com.tencent.bk.job.model.TagListResp;
import com.tencent.bk.job.model.TagParseResult;
import com.tencent.bk.job.model.TagParseStatusEnum;
import com.tencent.bk.job.service.impl.RepoTagServiceImpl;
import com.tencent.bk.job.utils.RepoTagUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("仓库Tag管理服务测试")
class RepoTagServiceImplTest {

    private RepoTagMapper repoTagMapper;

    private RepoTagServiceImpl repoTagService;

    @BeforeEach
    void setUp() {
        repoTagMapper = mock(RepoTagMapper.class);
        repoTagService = new RepoTagServiceImpl(repoTagMapper);
    }

    private static List<RepoTagDTO> tagsOf(String... tags) {
        List<RepoTagDTO> list = new ArrayList<>();
        for (String tag : tags) {
            TagParseResult result = RepoTagUtils.parse(tag);
            assertEquals(TagParseStatusEnum.VALID, result.getStatus(), "测试数据必须是纳管Tag: " + tag);
            list.add(result.getTag());
        }
        return list;
    }

    @Nested
    @DisplayName("批量写入Tag")
    class BatchAddTest {

        @Test
        @DisplayName("混合输入按新增/已存在/不纳管/非法四个桶归类，允许部分成功")
        void testBucketing() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.singletonList("v3.10.1"));

            BatchAddTagResp resp = repoTagService.batchAddTags(Arrays.asList(
                "v3.10.1", "V3.10.2", "v3.10.1-alpha.2",
                "v3.10.4-devgray.1", "v3.3.4.1",
                "v3.10.1-Alpha.1", "3.10.x"));

            assertTrue(resp.isResult());
            assertEquals(7, resp.getTotalCount());
            assertEquals(2, resp.getAddedCount());
            assertEquals(Arrays.asList("v3.10.2", "v3.10.1-alpha.2"), resp.getAddedTags());
            assertEquals(Collections.singletonList("v3.10.1"), resp.getExistedTags());
            assertEquals(Arrays.asList("v3.10.4-devgray.1", "v3.3.4.1"), resp.getIgnoredTags());
            assertEquals(Arrays.asList("v3.10.1-Alpha.1", "3.10.x"), resp.getInvalidTags());
        }

        @Test
        @DisplayName("只把合法Tag交给DAO，结构化列随Tag一并落库")
        void testOnlyValidTagsInserted() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.emptyList());

            repoTagService.batchAddTags(Arrays.asList("V3.10.3-alpha.11", "v3.3.4.1"));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RepoTagDTO>> captor = ArgumentCaptor.forClass(List.class);
            verify(repoTagMapper).batchInsert(captor.capture(), anyLong());
            assertEquals(1, captor.getValue().size());
            RepoTagDTO inserted = captor.getValue().get(0);
            assertEquals("v3.10.3-alpha.11", inserted.getTag());
            assertEquals(3, inserted.getMajor());
            assertEquals(10, inserted.getMinor());
            assertEquals(3, inserted.getPatch());
            assertEquals("alpha", inserted.getPreType());
            assertEquals(1, inserted.getPreRank());
            assertEquals(11, inserted.getPreNum());
        }

        @Test
        @DisplayName("批内大小写重复归一化后只写一条，重复项计入existedTags")
        void testInBatchDuplicate() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.emptyList());

            BatchAddTagResp resp = repoTagService.batchAddTags(Arrays.asList("V3.10.1", "v3.10.1", " 3.10.1 "));

            assertEquals(3, resp.getTotalCount());
            assertEquals(1, resp.getAddedCount());
            assertEquals(Collections.singletonList("v3.10.1"), resp.getAddedTags());
            assertEquals(Arrays.asList("v3.10.1", "v3.10.1"), resp.getExistedTags());
        }

        @Test
        @DisplayName("重复写入幂等：库中已有时不再新增")
        void testIdempotent() {
            when(repoTagMapper.selectExistingTags(anyList()))
                .thenReturn(Arrays.asList("v3.10.1", "v3.10.1-rc.1"));

            BatchAddTagResp resp = repoTagService.batchAddTags(Arrays.asList("v3.10.1", "v3.10.1-rc.1"));

            assertEquals(0, resp.getAddedCount());
            assertEquals(Arrays.asList("v3.10.1", "v3.10.1-rc.1"), resp.getExistedTags());
            verify(repoTagMapper, never()).batchInsert(anyList(), anyLong());
        }

        @Test
        @DisplayName("全部Tag都不纳管时不访问DB")
        void testAllUnmanaged() {
            BatchAddTagResp resp = repoTagService.batchAddTags(Arrays.asList("v3.3.4.1", "3.10.x"));

            assertTrue(resp.isResult());
            assertEquals(0, resp.getAddedCount());
            verifyNoInteractions(repoTagMapper);
        }

        @Test
        @DisplayName("超过单批上限直接拒绝，不截断也不访问DB")
        void testExceedBatchLimit() {
            List<String> tagList = new ArrayList<>();
            for (int i = 0; i <= RepoTagUtils.MAX_BATCH_SIZE; i++) {
                tagList.add("v3.10." + i);
            }

            BatchAddTagResp resp = repoTagService.batchAddTags(tagList);

            assertFalse(resp.isResult());
            assertTrue(resp.getMessage().contains(String.valueOf(tagList.size())));
            assertTrue(resp.getMessage().contains(String.valueOf(RepoTagUtils.MAX_BATCH_SIZE)));
            verifyNoInteractions(repoTagMapper);
        }

        @Test
        @DisplayName("空列表拒绝，不访问DB")
        void testEmptyList() {
            assertFalse(repoTagService.batchAddTags(null).isResult());
            assertFalse(repoTagService.batchAddTags(Collections.emptyList()).isResult());
            verifyNoInteractions(repoTagMapper);
        }
    }

    @Nested
    @DisplayName("批量删除Tag")
    class BatchDeleteTest {

        @Test
        @DisplayName("混合输入按已删除/库中不存在/不纳管/非法四个桶归类")
        void testBucketing() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.singletonList("v3.10.1"));

            BatchDeleteTagResp resp = repoTagService.batchDeleteTags(Arrays.asList(
                "V3.10.1", "v3.10.2", "v9.9.9-codev.10", "v3.10.1-Alpha.1"));

            assertTrue(resp.isResult());
            assertEquals(4, resp.getTotalCount());
            assertEquals(1, resp.getDeletedCount());
            assertEquals(Collections.singletonList("v3.10.1"), resp.getDeletedTags());
            assertEquals(Collections.singletonList("v3.10.2"), resp.getNotFoundTags());
            assertEquals(Collections.singletonList("v9.9.9-codev.10"), resp.getIgnoredTags());
            assertEquals(Collections.singletonList("v3.10.1-Alpha.1"), resp.getInvalidTags());
            verify(repoTagMapper).batchDelete(Collections.singletonList("v3.10.1"));
        }

        @Test
        @DisplayName("重复删除幂等：库中不存在时进notFoundTags而不报错")
        void testDeleteIdempotent() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.emptyList());

            BatchDeleteTagResp resp = repoTagService.batchDeleteTags(Collections.singletonList("v3.10.1"));

            assertTrue(resp.isResult());
            assertEquals(0, resp.getDeletedCount());
            assertEquals(Collections.singletonList("v3.10.1"), resp.getNotFoundTags());
            verify(repoTagMapper, never()).batchDelete(anyList());
        }

        @Test
        @DisplayName("超过单批上限直接拒绝，空列表同样拒绝，均不访问DB")
        void testInvalidBatchSize() {
            List<String> tagList = new ArrayList<>();
            for (int i = 0; i <= RepoTagUtils.MAX_BATCH_SIZE; i++) {
                tagList.add("v3.10." + i);
            }

            assertFalse(repoTagService.batchDeleteTags(tagList).isResult());
            assertFalse(repoTagService.batchDeleteTags(Collections.emptyList()).isResult());
            verifyNoInteractions(repoTagMapper);
        }
    }

    @Nested
    @DisplayName("按版本前缀查询Tag")
    class ListTagsTest {

        @Test
        @DisplayName("缺省按降序返回，order=asc时顺序相反")
        void testOrder() {
            when(repoTagMapper.selectSeries(eq(3), eq(10), eq(3), isNull()))
                .thenReturn(tagsOf("v3.10.3-beta.1", "v3.10.3", "v3.10.3-alpha.11", "v3.10.3-alpha.2"));

            List<String> desc = repoTagService.listTags("3.10.3", null).getTagList();
            List<String> asc = repoTagService.listTags("3.10.3", "asc").getTagList();

            assertEquals(Arrays.asList("v3.10.3", "v3.10.3-beta.1", "v3.10.3-alpha.11", "v3.10.3-alpha.2"), desc);
            List<String> reversedAsc = new ArrayList<>(asc);
            Collections.reverse(reversedAsc);
            assertEquals(desc, reversedAsc);
        }

        @Test
        @DisplayName("带先行标识的前缀按类型序收窄查询条件")
        void testPreReleasePrefix() {
            when(repoTagMapper.selectSeries(eq(3), eq(10), eq(3), eq(1)))
                .thenReturn(tagsOf("v3.10.3-alpha.1"));

            TagListResp resp = repoTagService.listTags("V3.10.3-alpha", "asc");

            assertTrue(resp.isResult());
            assertEquals("3.10.3-alpha", resp.getPrefix());
            assertEquals(1, resp.getTotal());
            assertFalse(resp.isTruncated());
        }

        @Test
        @DisplayName("命中数超过上限时截断返回并置truncated，而不是拒绝")
        void testTruncated() {
            List<RepoTagDTO> tags = new ArrayList<>();
            for (int patch = 1; patch <= RepoTagUtils.MAX_QUERY_RESULT_SIZE + 1; patch++) {
                tags.addAll(tagsOf("v3.10." + patch));
            }
            when(repoTagMapper.selectSeries(eq(3), eq(10), isNull(), isNull())).thenReturn(tags);

            TagListResp resp = repoTagService.listTags("3.10", "asc");

            assertTrue(resp.isResult());
            assertTrue(resp.isTruncated());
            assertEquals(RepoTagUtils.MAX_QUERY_RESULT_SIZE + 1, resp.getTotal(),
                "total是截断前的真实命中数");
            assertEquals(RepoTagUtils.MAX_QUERY_RESULT_SIZE, resp.getTagList().size());
            assertNotNull(resp.getMessage());
        }

        @Test
        @DisplayName("前缀非法时返回result=false且不访问DB")
        void testInvalidPrefix() {
            for (String prefix : Arrays.asList(null, "", "3.10.x", "3.10.1-Alpha")) {
                TagListResp resp = repoTagService.listTags(prefix, null);
                assertFalse(resp.isResult(), "非法前缀应被拒绝: " + prefix);
                assertTrue(resp.getTagList().isEmpty());
            }
            verifyNoInteractions(repoTagMapper);
        }
    }

    @Nested
    @DisplayName("求下一个Tag")
    class NextTagTest {

        @Test
        @DisplayName("稳定版分支按x.y小版本取数，系列回显为x.y")
        void testStableBranchQueriesMinorSeries() {
            when(repoTagMapper.selectSeries(eq(3), eq(10), isNull(), isNull()))
                .thenReturn(tagsOf("v3.10.3", "v3.10.7", "v3.10.8-rc.1"));

            NextTagResp resp = repoTagService.getNextTag("V3.10.3");

            assertTrue(resp.isResult());
            assertEquals("3.10", resp.getSeries());
            assertEquals("v3.10.7", resp.getCurrentMaxTag());
            assertEquals("v3.10.8", resp.getNextTag());
            assertNotNull(resp.getMessage(), "发生顺延时须提示调用方");
        }

        @Test
        @DisplayName("先行版分支在同一修订号的同类型子集内取最大先行号")
        void testPreReleaseBranch() {
            when(repoTagMapper.selectSeries(eq(3), eq(10), isNull(), isNull()))
                .thenReturn(tagsOf("v3.10.3-alpha.2", "v3.10.3-alpha.11", "v3.10.7-alpha.30"));

            NextTagResp resp = repoTagService.getNextTag("3.10.3-alpha");

            assertEquals("3.10.3-alpha", resp.getSeries());
            assertEquals("v3.10.3-alpha.11", resp.getCurrentMaxTag());
            assertEquals("v3.10.3-alpha.12", resp.getNextTag());
        }

        @Test
        @DisplayName("系列非法时返回result=false且不访问DB")
        void testInvalidSeries() {
            for (String series : Arrays.asList(null, "", "3.10", "3", "3.10.1-devgray")) {
                NextTagResp resp = repoTagService.getNextTag(series);
                assertFalse(resp.isResult(), "非法系列应被拒绝: " + series);
                assertTrue(resp.getMessage().contains("invalid series"));
            }
            verifyNoInteractions(repoTagMapper);
        }

        @Test
        @DisplayName("DAO返回空结果时先行版首次分配为.1")
        void testEmptySeries() {
            when(repoTagMapper.selectSeries(any(), any(), any(), any())).thenReturn(Collections.emptyList());

            assertEquals("v3.10.3-alpha.1", repoTagService.getNextTag("3.10.3-alpha").getNextTag());
            assertEquals("v3.10.3", repoTagService.getNextTag("3.10.3").getNextTag());
        }
    }

    @Nested
    @DisplayName("查询与写入共用同一套语义序")
    class ConsistencyTest {

        @Test
        @DisplayName("list_tags的最大值与next_tag的推导依据一致")
        void testMaxTagConsistency() {
            List<RepoTagDTO> tags = tagsOf("v3.10.3-alpha.2", "v3.10.3-alpha.11", "v3.10.3-beta.1", "v3.10.3");
            when(repoTagMapper.selectSeries(eq(3), eq(10), eq(3), isNull())).thenReturn(tags);
            when(repoTagMapper.selectSeries(eq(3), eq(10), isNull(), isNull())).thenReturn(tags);

            String maxInList = repoTagService.listTags("3.10.3", "desc").getTagList().get(0);
            String maxInNextTag = repoTagService.getNextTag("3.10.3").getCurrentMaxTag();

            assertEquals(maxInList, maxInNextTag);
        }

        @Test
        @DisplayName("写入时归一化的Tag可被同前缀查询原样命中")
        void testNormalizedTagIsQueryable() {
            when(repoTagMapper.selectExistingTags(anyList())).thenReturn(Collections.emptyList());
            BatchAddTagResp addResp = repoTagService.batchAddTags(Collections.singletonList(" V3.10.3-alpha.1 "));

            when(repoTagMapper.selectSeries(eq(3), eq(10), eq(3), eq(1)))
                .thenReturn(tagsOf(addResp.getAddedTags().toArray(new String[0])));
            List<String> queried = repoTagService.listTags("3.10.3-alpha", "asc").getTagList();

            assertEquals(Collections.singletonList("v3.10.3-alpha.1"), addResp.getAddedTags());
            assertEquals(addResp.getAddedTags(), queried);
        }
    }
}
