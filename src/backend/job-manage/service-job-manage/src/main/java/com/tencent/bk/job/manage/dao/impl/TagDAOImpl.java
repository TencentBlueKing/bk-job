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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.DuplicateEntryException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.dao.TagDAO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.exception.DataAccessException;
import org.jooq.generated.tables.Tag;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 29/9/2019 16:04
 */
@Slf4j
@Repository
public class TagDAOImpl implements TagDAO {
    private static final Tag TABLE = Tag.TAG;
    private final DSLContext context;

    @Autowired
    public TagDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    private TagDTO getTagByConditions(Collection<Condition> conditions) {
        TagDTO result = null;
        Record record =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.LAST_MODIFY_USER)
                .from(Tag.TAG).where(conditions).fetchOne();
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
    public List<TagDTO> listTagsByIds(long appId, Long... tagIds) {
        return this.listTagsByIds(appId, Arrays.asList(tagIds));
    }

    @Override
    public List<TagDTO> listTagsByAppId(long appId) {
        List<Condition> conditions = buildBasicCondition(appId);
        return listTagsByConditions(conditions);
    }

    @Override
    public Long insertTag(TagDTO tag) throws DuplicateEntryException {
        if (StringUtils.isBlank(tag.getLastModifyUser())) {
            tag.setLastModifyUser(tag.getCreator());
        }
        try {
            Record record =
                context.insertInto(Tag.TAG, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.LAST_MODIFY_USER)
                    .values(ULong.valueOf(tag.getAppId()), tag.getName(), tag.getCreator(), tag.getLastModifyUser())
                    .onDuplicateKeyIgnore().returning(TABLE.ID).fetchOne();
            if (record == null) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(TABLE.APP_ID.equal(ULong.valueOf(tag.getAppId())));
                conditions.add(TABLE.NAME.equal(tag.getName()));
                record = context.select(TABLE.ID).from(Tag.TAG).where(conditions).fetchOne();
            }
            return record.get(TABLE.ID).longValue();
        } catch (DataAccessException e) {
            log.error("Error while inserting tag, maybe duplicate name!|{}", tag, e);
            throw new DuplicateEntryException(ErrorCode.TAG_ALREADY_EXIST,
                "Duplicate name on tag|app_id|" + tag.getAppId() + "|name|" + tag.getName(), e);
        }
    }

    @Override
    public Boolean updateTagById(TagDTO tag) {
        List<Condition> conditions = buildBasicCondition(tag.getAppId());
        conditions.add(TABLE.ID.eq(ULong.valueOf(tag.getId())));
        int affected = context.update(Tag.TAG).set(TABLE.NAME, tag.getName())
            .set(TABLE.LAST_MODIFY_USER, tag.getLastModifyUser()).where(conditions).limit(1).execute();
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
        Result<Record5<ULong, ULong, String, String, String>> records =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.LAST_MODIFY_USER)
                .from(Tag.TAG).where(conditions).fetch();
        List<TagDTO> result = new ArrayList<>();
        if (records.size() > 0) {
            records.forEach(record -> result.add(record.into(TagDTO.class)));
        }
        return result;
    }

    @Override
    public List<TagDTO> listTags(TagDTO searchCondition) {
        List<Condition> conditions = buildTagCondition(searchCondition);
        Result<Record5<ULong, ULong, String, String, String>> records =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.LAST_MODIFY_USER).from(Tag.TAG)
                .where(conditions).orderBy(TABLE.ROW_UPDATE_TIME.desc()).fetch();
        List<TagDTO> tags = new ArrayList<>();
        if (records.size() > 0) {
            records.forEach(record -> tags.add(record.into(TagDTO.class)));
        }
        return tags;
    }

    private List<Condition> buildTagCondition(
        TagDTO tagCondition,
        BaseSearchCondition baseSearchCondition
    ) {
        List<Condition> conditions = buildTagCondition(tagCondition);
        conditions.addAll(buildBaseSearchCondition(baseSearchCondition));
        return conditions;
    }

    private long getTagCount(
        TagDTO tagCondition,
        BaseSearchCondition baseSearchCondition
    ) {
        List<Condition> conditions = buildTagCondition(tagCondition, baseSearchCondition);
        Long count = context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
        if (count == null) {
            throw new RuntimeException("Get null query count result");
        }
        return count;
    }

    @Override
    public PageData<TagDTO> listTags(
        TagDTO tagCondition,
        BaseSearchCondition baseSearchCondition
    ) {
        long count = getTagCount(tagCondition, baseSearchCondition);

        Collection<SortField<?>> orderFields = new ArrayList<>();
        if (org.apache.commons.lang3.StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TABLE.ID.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("name".equals(orderField)) {
                //正序
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TABLE.NAME.asc());
                } else {
                    orderFields.add(TABLE.NAME.desc());
                }
            } else {
                String msg = String.format("orderField %s not supported", orderField);
                throw new RuntimeException(msg);
            }
        }
        int start = baseSearchCondition.getStartOrDefault(PageUtil.DEFAULT_START);
        int length = baseSearchCondition.getLengthOrDefault(PageUtil.DEFAULT_POSITIVE_LENGTH);
        List<Condition> conditions = buildTagCondition(tagCondition);

        SelectSeekStepN<Record5<ULong, ULong, String, String, String>> selectSeekStep = context.select(TABLE.ID, TABLE.APP_ID, TABLE.NAME, TABLE.CREATOR, TABLE.LAST_MODIFY_USER).from(Tag.TAG)
            .where(conditions).orderBy(orderFields);
        if (log.isDebugEnabled()) {
            log.debug("SQL=" + selectSeekStep.getSQL(ParamType.INLINED));
        }
        Result<Record5<ULong, ULong, String, String, String>> records;
        if (length > 0) {
            records = selectSeekStep.limit(start, length).fetch();
        } else {
            records = selectSeekStep.offset(start).fetch();
        }
        List<TagDTO> tags = new ArrayList<>();
        if (records.size() > 0) {
            records.forEach(record -> tags.add(record.into(TagDTO.class)));
        }
        return new PageData<>(start, length, count, tags);
    }

    private List<Condition> buildTagCondition(TagDTO searchCondition) {
        List<Condition> conditions = new ArrayList<>();
        if (searchCondition.getId() != null) {
            conditions.add(TABLE.ID.eq(ULong.valueOf(searchCondition.getId())));
        }
        if (searchCondition.getAppId() != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(searchCondition.getAppId())));
        }
        if (StringUtils.isNotBlank(searchCondition.getName())) {
            String likePattern = "%" + searchCondition.getName() + "%";
            conditions.add(TABLE.NAME.like(likePattern));
        }
        if (StringUtils.isNotBlank(searchCondition.getCreator())) {
            conditions.add(TABLE.CREATOR.eq(searchCondition.getCreator()));
        }
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        return conditions;
    }

    private List<Condition> buildBaseSearchCondition(
        BaseSearchCondition baseSearchCondition
    ) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
            conditions.add(TABLE.CREATOR.eq(baseSearchCondition.getCreator()));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
            conditions.add(TABLE.LAST_MODIFY_USER.eq(baseSearchCondition.getLastModifyUser()));
        }
        return conditions;
    }

}
