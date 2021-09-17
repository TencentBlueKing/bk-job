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

import com.tencent.bk.job.common.exception.DAOException;
import com.tencent.bk.job.manage.dao.ResourceTagDAO;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.generated.tables.ResourceTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class ResourceTagDAOImpl implements ResourceTagDAO {
    private static final ResourceTag TABLE = ResourceTag.RESOURCE_TAG;
    private static final TableField<?, ?>[] ALL_FIELDS = {TABLE.ID, TABLE.TAG_ID, TABLE.RESOURCE_ID,
        TABLE.RESOURCE_TYPE};
    private final DSLContext context;

    @Autowired
    public ResourceTagDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(Long tagId) {
        Result<? extends Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.TAG_ID.eq(tagId))
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(Long tagId, Integer resourceType) {
        Result<? extends Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.TAG_ID.eq(tagId))
            .and(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(List<Long> tagIds, Integer resourceType) {
        Result<? extends Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.TAG_ID.in(tagIds))
            .and(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(Integer resourceType, List<String> resourceIds) {
        Result<? extends Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .and(TABLE.RESOURCE_ID.in(resourceIds))
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(Integer resourceType, String resourceId) {
        Result<? extends Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .and(TABLE.RESOURCE_ID.eq(resourceId))
            .fetch();
        return result.map(this::extract);
    }

    @Override
    public List<ResourceTagDTO> listResourceTags(List<Long> tagIds) {
        Result<? extends Record> result = context.select(ALL_FIELDS).from(TABLE).where(TABLE.TAG_ID.in(tagIds))
            .fetch();
        return result.map(this::extract);
    }

    private ResourceTagDTO extract(Record record) {
        ResourceTagDTO resourceTag = new ResourceTagDTO();
        resourceTag.setResourceType(record.get(TABLE.RESOURCE_TYPE).intValue());
        resourceTag.setResourceId(record.get(TABLE.RESOURCE_ID));
        resourceTag.setTagId(record.get(TABLE.TAG_ID));
        return resourceTag;
    }

    @Override
    public boolean deleteResourceTags(Integer resourceType, String resourceId, List<Long> tagIds) {
        int affectRow = context.deleteFrom(TABLE).where(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .and(TABLE.RESOURCE_ID.eq(resourceId))
            .and(TABLE.TAG_ID.in(tagIds))
            .execute();
        return affectRow > 0;
    }

    @Override
    public boolean deleteResourceTag(Integer resourceType, String resourceId, Long tagId) {
        int affectRow = context.deleteFrom(TABLE).where(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .and(TABLE.RESOURCE_ID.eq(resourceId))
            .and(TABLE.TAG_ID.eq(tagId))
            .execute();
        return affectRow > 0;
    }

    @Override
    public boolean deleteResourceTags(Integer resourceType, String resourceId) {
        int affectRow = context.deleteFrom(TABLE).where(TABLE.RESOURCE_TYPE.eq(resourceType.byteValue()))
            .and(TABLE.RESOURCE_ID.eq(resourceId)).execute();
        return affectRow > 0;
    }

    @Override
    public boolean deleteResourceTags(List<ResourceTagDTO> resourceTags) {
        resourceTags.forEach(resourceTag ->
            deleteResourceTag(resourceTag.getResourceType(), resourceTag.getResourceId(), resourceTag.getTagId()));
        return true;
    }

    @Override
    public boolean deleteResourceTags(Long tagId) {
        int affectRow = context.deleteFrom(TABLE).where(TABLE.TAG_ID.eq(tagId)).execute();
        return affectRow > 0;
    }

    @Override
    public boolean batchSaveResourceTags(List<ResourceTagDTO> resourceTags) throws DAOException {
        try {
            resourceTags.forEach(resourceTag -> context.insertInto(TABLE, TABLE.RESOURCE_TYPE, TABLE.RESOURCE_ID,
                TABLE.TAG_ID)
                .values(resourceTag.getResourceType().byteValue(), resourceTag.getResourceId(),
                    resourceTag.getTagId())
                .onDuplicateKeyIgnore()
                .execute());
            return true;
        } catch (Throwable e) {
            log.error("Batch save resource tags caught exception", e);
            throw new DAOException(e.getMessage(), e);
        }
    }
}
