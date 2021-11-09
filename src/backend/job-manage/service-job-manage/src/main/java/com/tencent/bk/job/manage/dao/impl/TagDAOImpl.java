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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.dao.TagDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.generated.tables.Tag;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 29/9/2019 16:04
 */
@Slf4j
@Repository
public class TagDAOImpl implements TagDAO {
    private static final Tag TABLE = Tag.TAG;
    private static final TableField<?,?>[] ALL_FIELDS = {TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION,
        TABLE.CREATOR, TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME};
    private final DSLContext context;

    @Autowired
    public TagDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    private TagDTO getTagByConditions(Collection<Condition> conditions) {
        TagDTO result = null;
        Record record =
            context.select(ALL_FIELDS).from(Tag.TAG).where(conditions).fetchOne();
        if (record != null) {
            result = record.into(TagDTO.class);
        }
        return result;
    }

    @Override
    public TagDTO getTagById(long tagId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(tagId)));
        return getTagByConditions(conditions);
    }

    @Override
    public TagDTO getTagById(long appId, long tagId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.ID.eq(ULong.valueOf(tagId)));
        return getTagByConditions(conditions);
    }

    @Override
    public List<TagDTO> listTagsByIds(long appId, List<Long> tagIds) {
        // For fixed-size list
        tagIds = new ArrayList<>(tagIds);
        tagIds.removeIf(tagId -> tagId <= 0);
        List<Condition> conditions = buildBasicCondition(appId);
        conditions.add(TABLE.ID.in(tagIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listTagsByConditions(conditions);
    }

    @Override
    public List<TagDTO> listTagsByIds(List<Long> tagIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.in(tagIds));
        return listTagsByConditions(conditions);
    }

    @Override
    public List<TagDTO> listTagsByIds(long appId, Long... tagIds) {
        return this.listTagsByIds(appId, Arrays.asList(tagIds));
    }

    @Override
    public List<TagDTO> listTagsByAppId(long appId) {
        List<Condition> conditions = buildBasicCondition(appId);
        return listTagsByConditions(conditions);
    }

    @Override
    public Long insertTag(TagDTO tag) {
        if (StringUtils.isBlank(tag.getLastModifyUser())) {
            tag.setLastModifyUser(tag.getCreator());
        }
        if (tag.getCreateTime() == null) {
            tag.setCreateTime(DateUtils.currentTimeSeconds());
            tag.setLastModifyTime(tag.getCreateTime());
        }
        Record record =
            context.insertInto(Tag.TAG, TABLE.APP_ID, TABLE.NAME, TABLE.DESCRIPTION, TABLE.CREATOR,
                TABLE.CREATE_TIME, TABLE.LAST_MODIFY_USER, TABLE.LAST_MODIFY_TIME)
                .values(ULong.valueOf(tag.getAppId()), tag.getName(), tag.getDescription(), tag.getCreator(),
                    tag.getCreateTime(), tag.getLastModifyUser(), tag.getLastModifyTime())
                .onDuplicateKeyIgnore().returning(TABLE.ID).fetchOne();
        if (record == null) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TABLE.APP_ID.equal(ULong.valueOf(tag.getAppId())));
            conditions.add(TABLE.NAME.equal(tag.getName()));
            record = context.select(TABLE.ID).from(Tag.TAG).where(conditions).fetchOne();
        }
        return record != null ? record.get(TABLE.ID).longValue() : 0;
    }

    @Override
    public boolean updateTagById(TagDTO tag) {
        List<Condition> conditions = buildBasicCondition(tag.getAppId());
        conditions.add(TABLE.ID.eq(ULong.valueOf(tag.getId())));
        int affected = context.update(Tag.TAG)
            .set(TABLE.NAME, tag.getName())
            .set(TABLE.LAST_MODIFY_USER, tag.getLastModifyUser())
            .set(TABLE.DESCRIPTION, tag.getDescription())
            .where(conditions).limit(1).execute();
        if (affected == 1) {
            return true;
        } else if (affected <= 0) {
            log.error("Error while updating tag. Affected row lower than 1.|{}", tag);
            return false;
        } else {
            // affected >= 2
            log.error("Error while updating tag. Affected more than one row!|{}", tag);
            return false;
        }
    }

    private List<Condition> buildBasicCondition(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        return conditions;
    }

    private List<TagDTO> listTagsByConditions(List<Condition> conditions) {
        Result<Record> result =
            context.select(ALL_FIELDS).from(Tag.TAG).where(conditions).fetch();
        List<TagDTO> resultTags = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> resultTags.add(record.into(TagDTO.class)));
        }
        return resultTags;
    }

    @Override
    public List<TagDTO> listTags(TagDTO tagQuery) {
        List<Condition> conditions = buildSearchCondition(tagQuery);
        Result<Record> result =
            context.select(ALL_FIELDS).from(Tag.TAG)
                .where(conditions).orderBy(TABLE.ROW_UPDATE_TIME.desc()).fetch();
        List<TagDTO> tags = new ArrayList<>();
        if (result.size() > 0) {
            result.forEach(record -> tags.add(record.into(TagDTO.class)));
        }
        return tags;
    }

    @Override
    public PageData<TagDTO> listPageTags(TagDTO tagQuery, BaseSearchCondition baseSearchCondition) {
        long count = getTagsPageCount(tagQuery);
        if (count == 0) {
            return PageData.emptyPageData(baseSearchCondition.getStart(), baseSearchCondition.getLength());
        }

        List<Condition> conditions = buildSearchCondition(tagQuery);
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        List<OrderField<?>> orderFields = buildOrderFields(baseSearchCondition);
        Result<Record> result = context.select(ALL_FIELDS).from(Tag.TAG)
            .where(conditions).orderBy(orderFields)
            .limit(start, length).fetch();
        List<TagDTO> tags = result.size() > 0 ? result.map(this::extractRecord) : Collections.emptyList();
        return new PageData<>(start, length, count, tags);
    }

    private long getTagsPageCount(TagDTO tagQuery) {
        List<Condition> conditions = buildSearchCondition(tagQuery);
        Long count =  context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
        return count == null ? 0 : count;
    }

    private List<Condition> buildSearchCondition(TagDTO tagQuery) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(tagQuery.getAppId())));
        if (StringUtils.isNotBlank(tagQuery.getName())) {
            String likePattern = "%" + tagQuery.getName() + "%";
            conditions.add(TABLE.NAME.like(likePattern));
        }
        if (StringUtils.isNotEmpty(tagQuery.getCreator())) {
            conditions.add(TABLE.CREATOR.equal(tagQuery.getCreator()));
        }
        if (StringUtils.isNotEmpty(tagQuery.getLastModifyUser())) {
            conditions.add(TABLE.LAST_MODIFY_USER.equal(tagQuery.getLastModifyUser()));
        }
        return conditions;
    }

    private List<OrderField<?>> buildOrderFields(BaseSearchCondition baseSearchCondition) {
        List<OrderField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TABLE.LAST_MODIFY_TIME.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("name".equals(orderField)) {
                //升序
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TABLE.NAME.asc());
                } else {
                    orderFields.add(TABLE.NAME.desc());
                }
            } else if ("createTime".equals(orderField)) {
                //升序
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TABLE.CREATE_TIME.asc());
                } else {
                    orderFields.add(TABLE.CREATE_TIME.desc());
                }

            } else {
                // 默认按照last_modify_time排序
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TABLE.LAST_MODIFY_TIME.asc());
                } else {
                    orderFields.add(TABLE.LAST_MODIFY_TIME.desc());
                }
            }
        }
        return orderFields;
    }


    private TagDTO extractRecord(Record record) {
        if (record == null) {
            return null;
        }
        TagDTO tag = new TagDTO();
        tag.setAppId(record.get(TABLE.APP_ID).longValue());
        tag.setId(record.get(TABLE.ID).longValue());
        tag.setName(record.get(TABLE.NAME));
        tag.setDescription(record.get(TABLE.DESCRIPTION));
        tag.setCreator(record.get(TABLE.CREATOR));
        tag.setCreateTime(record.get(TABLE.CREATE_TIME));
        tag.setLastModifyUser(record.get(TABLE.LAST_MODIFY_USER));
        tag.setLastModifyTime(record.get(TABLE.LAST_MODIFY_TIME));
        return tag;
    }

    @Override
    public boolean deleteTagById(Long tagId) {
        return context.deleteFrom(TABLE).where(TABLE.ID.eq(ULong.valueOf(tagId))).execute() == 1;
    }

    @Override
    public boolean isExistDuplicateName(Long appId, String tagName) {
        return context.fetchExists(TABLE, TABLE.NAME.eq(tagName));
    }

    @Override
    public List<TagDTO> listAllTags() {
        Result<Record> result = context.select(ALL_FIELDS).from(Tag.TAG).fetch();
        return result.size() > 0 ? result.map(this::extractRecord) : Collections.emptyList();
    }
}
