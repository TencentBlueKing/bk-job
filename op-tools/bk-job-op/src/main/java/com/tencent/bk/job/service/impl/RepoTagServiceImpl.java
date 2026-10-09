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

package com.tencent.bk.job.service.impl;

import com.tencent.bk.job.dao.RepoTagMapper;
import com.tencent.bk.job.model.BatchAddTagResp;
import com.tencent.bk.job.model.BatchDeleteTagResp;
import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.RepoTagDTO;
import com.tencent.bk.job.model.TagListResp;
import com.tencent.bk.job.model.TagParseResult;
import com.tencent.bk.job.model.TagSeriesDTO;
import com.tencent.bk.job.service.RepoTagService;
import com.tencent.bk.job.utils.RepoTagUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 仓库Tag管理服务实现
 * <p>
 * repo_tag表是Git仓库Tag的派生镜像：数据源头在Git，误删后重跑一次批量写入即可完全恢复，
 * 因此不像migrate_biz那样配套审计表，删除操作的追溯诉求由本类的INFO操作日志覆盖。
 * <p>
 * 单个Tag的处理结果分桶返回、允许部分成功：全量同步场景下必然混入大量不纳管/非法Tag
 * （仓库现有348个Tag中约17.5%不符合纳管规范），整批拒绝会让接口直接不可用。
 * 也正因为允许部分成功，这里不引入事务语义。
 * <p>
 * 所有可预期的业务错误都以HTTP 200 + result=false + message返回，不抛异常：
 * 项目已有ApiExceptionHandler（@RestControllerAdvice，仅作用于controller包）可把IllegalArgumentException
 * 映射为400，本实现仍选择分桶返回而非抛异常，是因为批量接口允许部分成功——单个Tag的结果需要按
 * 新增/已存在/批内重复/不纳管/非法分桶逐条回传，抛异常只能表达"整批失败"，无法表达部分成功。
 */
@Slf4j
@Service
public class RepoTagServiceImpl implements RepoTagService {

    private final RepoTagMapper repoTagMapper;

    public RepoTagServiceImpl(RepoTagMapper repoTagMapper) {
        this.repoTagMapper = repoTagMapper;
    }

    @Override
    public BatchAddTagResp batchAddTags(List<String> tagList) {
        if (CollectionUtils.isEmpty(tagList)) {
            return BatchAddTagResp.fail("tagList can not be empty");
        }
        if (tagList.size() > RepoTagUtils.MAX_BATCH_SIZE) {
            return BatchAddTagResp.fail(String.format("tagList size %d exceeds limit %d",
                tagList.size(), RepoTagUtils.MAX_BATCH_SIZE));
        }

        BatchAddTagResp resp = new BatchAddTagResp();
        resp.setResult(true);
        resp.setTotalCount(tagList.size());
        Map<String, RepoTagDTO> candidates = classify(tagList, resp.getIgnoredTags(), resp.getInvalidTags(),
            resp.getDuplicatedTags());
        if (candidates.isEmpty()) {
            log.info("Add repo tags: totalCount={}, addedCount=0, duplicated={}, ignored={}, invalid={}",
                resp.getTotalCount(), resp.getDuplicatedTags(), resp.getIgnoredTags(), resp.getInvalidTags());
            return resp;
        }

        Set<String> existedInDb = new HashSet<>(repoTagMapper.selectExistingTags(new ArrayList<>(candidates.keySet())));
        List<RepoTagDTO> toInsert = new ArrayList<>();
        for (Map.Entry<String, RepoTagDTO> entry : candidates.entrySet()) {
            if (existedInDb.contains(entry.getKey())) {
                resp.getExistedTags().add(entry.getKey());
            } else {
                toInsert.add(entry.getValue());
                resp.getAddedTags().add(entry.getKey());
            }
        }
        if (!toInsert.isEmpty()) {
            repoTagMapper.batchInsert(toInsert, System.currentTimeMillis());
        }
        resp.setAddedCount(resp.getAddedTags().size());
        log.info("Add repo tags: totalCount={}, addedCount={}, added={}, existed={}, duplicated={}, ignored={}, "
                + "invalid={}",
            resp.getTotalCount(), resp.getAddedCount(), resp.getAddedTags(), resp.getExistedTags(),
            resp.getDuplicatedTags(), resp.getIgnoredTags(), resp.getInvalidTags());
        return resp;
    }

    @Override
    public BatchDeleteTagResp batchDeleteTags(List<String> tagList) {
        if (CollectionUtils.isEmpty(tagList)) {
            return BatchDeleteTagResp.fail("tagList can not be empty");
        }
        if (tagList.size() > RepoTagUtils.MAX_BATCH_SIZE) {
            return BatchDeleteTagResp.fail(String.format("tagList size %d exceeds limit %d",
                tagList.size(), RepoTagUtils.MAX_BATCH_SIZE));
        }

        BatchDeleteTagResp resp = new BatchDeleteTagResp();
        resp.setResult(true);
        resp.setTotalCount(tagList.size());
        Map<String, RepoTagDTO> candidates = classify(tagList, resp.getIgnoredTags(), resp.getInvalidTags(),
            resp.getDuplicatedTags());
        if (candidates.isEmpty()) {
            log.info("Delete repo tags: totalCount={}, deletedCount=0, duplicated={}, ignored={}, invalid={}",
                resp.getTotalCount(), resp.getDuplicatedTags(), resp.getIgnoredTags(), resp.getInvalidTags());
            return resp;
        }

        Set<String> existedInDb = new HashSet<>(repoTagMapper.selectExistingTags(new ArrayList<>(candidates.keySet())));
        for (String tag : candidates.keySet()) {
            if (existedInDb.contains(tag)) {
                resp.getDeletedTags().add(tag);
            } else {
                resp.getNotFoundTags().add(tag);
            }
        }
        if (!resp.getDeletedTags().isEmpty()) {
            repoTagMapper.batchDelete(resp.getDeletedTags());
        }
        resp.setDeletedCount(resp.getDeletedTags().size());
        log.info("Delete repo tags: totalCount={}, deletedCount={}, deleted={}, notFound={}, duplicated={}, "
                + "ignored={}, invalid={}",
            resp.getTotalCount(), resp.getDeletedCount(), resp.getDeletedTags(), resp.getNotFoundTags(),
            resp.getDuplicatedTags(), resp.getIgnoredTags(), resp.getInvalidTags());
        return resp;
    }

    @Override
    public TagListResp listTags(String prefix, String order) {
        TagSeriesDTO series = RepoTagUtils.parseSeries(prefix, false);
        if (series == null) {
            return TagListResp.fail("invalid prefix: " + prefix
                + ", expect x / x.y / x.y.z / x.y.z-{alpha|beta|rc}");
        }
        Integer preRank = series.getType() == null ? null : series.getType().getRank();
        List<RepoTagDTO> tags = repoTagMapper.selectSeries(series.getMajor(), series.getMinor(),
            series.getPatch(), preRank);

        TagListResp resp = new TagListResp();
        resp.setResult(true);
        resp.setPrefix(series.toNormalizedString());
        resp.setTotal(tags == null ? 0 : tags.size());
        List<String> tagNames = RepoTagUtils.toSortedTagNames(tags, "asc".equalsIgnoreCase(order));
        if (tagNames.size() > RepoTagUtils.MAX_QUERY_RESULT_SIZE) {
            // 查询侧截断不会造成数据丢失，比直接拒绝更可用；调用方可据truncated标记收窄prefix再查
            resp.setTruncated(true);
            resp.setTagList(new ArrayList<>(tagNames.subList(0, RepoTagUtils.MAX_QUERY_RESULT_SIZE)));
            resp.setMessage(String.format("matched %d tags, truncated to %d, please use a more specific prefix",
                resp.getTotal(), RepoTagUtils.MAX_QUERY_RESULT_SIZE));
        } else {
            resp.setTagList(new ArrayList<>(tagNames));
        }
        return resp;
    }

    @Override
    public NextTagResp getNextTag(String series) {
        TagSeriesDTO parsedSeries = RepoTagUtils.parseSeries(series, true);
        if (parsedSeries == null) {
            return NextTagResp.fail("invalid series: " + series
                + ", expect x.y.z or x.y.z-{alpha|beta|rc}");
        }
        // 两个分支都只需要x.y范围内的Tag：先行版分支再按修订号与类型收窄，稳定版分支要看整个x.y的稳定版
        List<RepoTagDTO> tagsInMinorSeries = repoTagMapper.selectSeries(parsedSeries.getMajor(),
            parsedSeries.getMinor(), null, null);
        return RepoTagUtils.nextTag(parsedSeries, tagsInMinorSeries);
    }

    /**
     * 解析并分桶，返回批内去重后的待处理Tag
     *
     * @param tagList     原始Tag列表
     * @param ignoredTags 承接不纳管Tag的桶
     * @param invalidTags 承接非法Tag的桶
     * @param duplicated  承接批内重复Tag的桶
     * @return 归一化Tag到结构化Tag的映射，保持输入顺序
     */
    private Map<String, RepoTagDTO> classify(List<String> tagList, List<String> ignoredTags,
                                             List<String> invalidTags, List<String> duplicated) {
        Map<String, RepoTagDTO> candidates = new LinkedHashMap<>();
        for (String rawTag : tagList) {
            TagParseResult parseResult = RepoTagUtils.parse(rawTag);
            switch (parseResult.getStatus()) {
                case VALID:
                    RepoTagDTO tag = parseResult.getTag();
                    if (candidates.put(tag.getTag(), tag) != null) {
                        duplicated.add(tag.getTag());
                    }
                    break;
                case IGNORED:
                    ignoredTags.add(parseResult.getRawTag());
                    log.info("Ignore repo tag {}: {}", parseResult.getRawTag(), parseResult.getReason());
                    break;
                default:
                    invalidTags.add(parseResult.getRawTag());
                    log.info("Invalid repo tag {}: {}", parseResult.getRawTag(), parseResult.getReason());
                    break;
            }
        }
        return candidates;
    }
}
