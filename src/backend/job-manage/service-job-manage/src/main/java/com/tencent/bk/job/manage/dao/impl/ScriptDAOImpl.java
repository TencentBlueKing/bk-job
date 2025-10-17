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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptScopeEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.model.tables.Script;
import com.tencent.bk.job.manage.model.tables.ScriptVersion;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record6;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

/**
 * 脚本DAO - script/script_version
 */
@Repository
public class ScriptDAOImpl implements ScriptDAO {
    private static final Script TB_SCRIPT = Script.SCRIPT;
    private static final ScriptVersion TB_SCRIPT_VERSION = ScriptVersion.SCRIPT_VERSION;
    private final DSLContext dslContext;

    private static final TableField<?, ?>[] SCRIPT_ALL_FIELDS = {
        TB_SCRIPT.ID,
        TB_SCRIPT.APP_ID,
        TB_SCRIPT.NAME,
        TB_SCRIPT.CATEGORY,
        TB_SCRIPT.TYPE,
        TB_SCRIPT.CREATOR,
        TB_SCRIPT.CREATE_TIME,
        TB_SCRIPT.LAST_MODIFY_USER,
        TB_SCRIPT.LAST_MODIFY_TIME,
        TB_SCRIPT.IS_PUBLIC,
        TB_SCRIPT.DESCRIPTION,
        TB_SCRIPT.TENANT_ID
    };

    private static final TableField<?, ?>[] ALL_FIELDS = {
        TB_SCRIPT.ID,
        TB_SCRIPT.APP_ID,
        TB_SCRIPT.NAME,
        TB_SCRIPT.CATEGORY,
        TB_SCRIPT.TYPE,
        TB_SCRIPT.CREATOR,
        TB_SCRIPT.CREATE_TIME,
        TB_SCRIPT.LAST_MODIFY_USER,
        TB_SCRIPT.LAST_MODIFY_TIME,
        TB_SCRIPT.IS_PUBLIC,
        TB_SCRIPT.DESCRIPTION,
        TB_SCRIPT.TENANT_ID,
        TB_SCRIPT_VERSION.ID,
        TB_SCRIPT_VERSION.VERSION,
        TB_SCRIPT_VERSION.CONTENT,
        TB_SCRIPT_VERSION.STATUS,
        TB_SCRIPT_VERSION.CREATE_TIME,
        TB_SCRIPT_VERSION.CREATOR,
        TB_SCRIPT_VERSION.LAST_MODIFY_TIME,
        TB_SCRIPT_VERSION.LAST_MODIFY_USER,
        TB_SCRIPT_VERSION.VERSION_DESC
    };

    @Autowired
    public ScriptDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public PageData<ScriptDTO> listPageScript(ScriptQuery scriptQuery,
                                              BaseSearchCondition baseSearchCondition) {
        if (scriptQuery.hasScriptVersionQueryCondition()) {
            return listPageScriptWithScriptVersionQueryCondition(scriptQuery, baseSearchCondition);
        } else {
            return listPageScriptWithBasicQueryCondition(scriptQuery, baseSearchCondition);
        }
    }

    private PageData<ScriptDTO> listPageScriptWithBasicQueryCondition(ScriptQuery scriptQuery,
                                                                      BaseSearchCondition baseSearchCondition) {
        long count = getPageScriptCountWithBasicQueryCondition(scriptQuery, baseSearchCondition);

        List<SortField<?>> orderFields = buildOrderFields(baseSearchCondition);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result<? extends Record> result =
            dslContext.select(SCRIPT_ALL_FIELDS)
                .from(TB_SCRIPT)
                .where(buildScriptConditionList(scriptQuery, baseSearchCondition))
                .orderBy(orderFields)
                .limit(start, length).fetch();
        return pageResult(result, count, start, length);
    }

    private long getPageScriptCountWithBasicQueryCondition(ScriptQuery scriptQuery,
                                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildScriptConditionList(scriptQuery, baseSearchCondition);
        Long count = dslContext.selectCount().from(TB_SCRIPT).where(conditions).fetchOne(0, Long.class);
        return count == null ? 0 : count;
    }

    private PageData<ScriptDTO> listPageScriptWithScriptVersionQueryCondition(ScriptQuery scriptQuery,
                                                                              BaseSearchCondition baseSearchCondition) {
        // 如果查询条件包含script_version表中的字段，比如脚本内容，需要关联 script+script_version 查询
        int count = getPageScriptCountWithScriptVersionQueryCondition(scriptQuery, baseSearchCondition);
        List<SortField<?>> orderFields = buildOrderFields(baseSearchCondition);
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result<? extends Record> result =
            dslContext.selectDistinct(SCRIPT_ALL_FIELDS)
                .from(TB_SCRIPT)
                .leftJoin(TB_SCRIPT_VERSION)
                .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
                .where(buildScriptConditionList(scriptQuery, baseSearchCondition))
                .orderBy(orderFields)
                .limit(start, length).fetch();
        return pageResult(result, count, start, length);
    }

    private int getPageScriptCountWithScriptVersionQueryCondition(ScriptQuery scriptQuery,
                                                                  BaseSearchCondition baseSearchCondition) {
        return dslContext.fetchCount(
            dslContext.selectDistinct(TB_SCRIPT.ID)
                .from(TB_SCRIPT)
                .leftJoin(TB_SCRIPT_VERSION)
                .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
                .where(buildScriptConditionList(scriptQuery, baseSearchCondition)));
    }

    private List<SortField<?>> buildOrderFields(BaseSearchCondition baseSearchCondition) {
        List<SortField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("name".equals(orderField)) {
                //升序
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TB_SCRIPT.NAME.asc());
                } else {
                    orderFields.add(TB_SCRIPT.NAME.desc());
                }
            } else if ("type".equals(orderField)) {
                String sortExpr = Arrays.stream(ScriptTypeEnum.getScriptTypeNameAscSort())
                    .map(scriptTypeEnum -> scriptTypeEnum.getValue().toString())
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(DSL.field("field({0}," + sortExpr + ")", TB_SCRIPT.TYPE).asc());
                } else {
                    orderFields.add(DSL.field("field({0}," + sortExpr + ")", TB_SCRIPT.TYPE).desc());
                }
            } else if ("creator".equals(orderField)) {
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TB_SCRIPT.CREATOR.asc());
                } else {
                    orderFields.add(TB_SCRIPT.CREATOR.desc());
                }
            } else if ("lastModifyTime".equals(orderField)) {
                if (baseSearchCondition.getOrder() == Order.ASCENDING.getOrder()) {
                    orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.asc());
                } else {
                    orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
                }
            } else {
                orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
            }
        }
        return orderFields;
    }

    private PageData<ScriptDTO> pageResult(Result<? extends Record> result, long count, int start, int length) {
        List<ScriptDTO> scripts = new ArrayList<>();
        if (!result.isEmpty()) {
            scripts = result.map(this::extractScriptData);
        }

        PageData<ScriptDTO> scriptPageData = new PageData<>();
        scriptPageData.setTotal(count);
        scriptPageData.setPageSize(length);
        scriptPageData.setData(scripts);
        scriptPageData.setStart(start);
        return scriptPageData;
    }

    @Override
    public List<ScriptDTO> listScripts(ScriptQuery scriptQuery) {
        Result<? extends Record> result =
            dslContext.select(SCRIPT_ALL_FIELDS)
                .from(TB_SCRIPT)
                .where(buildScriptConditionList(scriptQuery, null))
                .orderBy(TB_SCRIPT.NAME.asc())
                .fetch();
        List<ScriptDTO> scripts = new ArrayList<>();
        if (!result.isEmpty()) {
            scripts = result.map(this::extractScriptData);
        }
        return scripts;
    }

    @Override
    public ScriptDTO getScriptByScriptId(String scriptId) {
        Record record = dslContext.select(SCRIPT_ALL_FIELDS).from(Script.SCRIPT)
            .where(TB_SCRIPT.ID.eq(scriptId)).fetchOne();
        return extractScriptData(record);
    }

    @Override
    public List<ScriptBasicDTO> listScriptBasicInfoByScriptIds(Collection<String> scriptIds) {
        Result<Record6<String, ULong, String, UByte, UByte, UByte>> records = dslContext.select(
                TB_SCRIPT.ID,
                TB_SCRIPT.APP_ID,
                TB_SCRIPT.NAME,
                TB_SCRIPT.CATEGORY,
                TB_SCRIPT.TYPE,
                TB_SCRIPT.IS_PUBLIC
            ).from(Script.SCRIPT)
            .where(TB_SCRIPT.ID.in(scriptIds)).fetch();
        return records.map(this::extractScriptBasicDTO);
    }

    @Override
    public ScriptDTO getScriptVersionById(long id) {
        Record record = dslContext.select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION)
            .join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(id))
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public ScriptDTO getByScriptIdAndVersion(Long appId, String scriptId, String version) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0)));
        if (scriptId != null) {
            conditions.add(TB_SCRIPT.ID.eq(scriptId));
        }
        if (version != null) {
            conditions.add(TB_SCRIPT_VERSION.VERSION.eq(version));
        }

        Record record = dslContext.select(ALL_FIELDS).from(TB_SCRIPT_VERSION).join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(conditions)
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public List<ScriptDTO> batchGetScriptVersionsByIds(Collection<Long> scriptVersionIds) {
        List<ULong> scriptVersionIdList = scriptVersionIds.stream().map(ULong::valueOf).collect(Collectors.toList());
        Result<? extends Record> result = dslContext
            .select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION)
            .join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(TB_SCRIPT_VERSION.ID.in(scriptVersionIdList)
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .fetch();

        if (result.isEmpty()) {
            return Collections.emptyList();
        }

        return result.map(this::extractScriptVersionData);
    }

    private List<Condition> buildScriptConditionList(ScriptQuery scriptQuery,
                                                     BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(String.valueOf(0))));
        if (scriptQuery != null) {
            if (StringUtils.isNotBlank(scriptQuery.getId())) {
                conditions.add(TB_SCRIPT.ID.eq(scriptQuery.getId()));
            } else if (CollectionUtils.isNotEmpty(scriptQuery.getIds())) {
                if (scriptQuery.getIds().size() == 1) {
                    conditions.add(TB_SCRIPT.ID.eq(scriptQuery.getIds().get(0)));
                } else {
                    conditions.add(TB_SCRIPT.ID.in(scriptQuery.getIds()));
                }
            }
            if (scriptQuery.getType() != null && scriptQuery.getType() > 0) {
                conditions.add(TB_SCRIPT.TYPE.eq(UByte.valueOf(scriptQuery.getType())));
            }
            if (StringUtils.isNotBlank(scriptQuery.getName())) {
                conditions.add(TB_SCRIPT.NAME.like("%" + scriptQuery.getName() + "%"));
            }

            if (baseSearchCondition != null) {
                if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
                    conditions.add(TB_SCRIPT.CREATOR.eq(baseSearchCondition.getCreator()));
                }
                if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
                    conditions.add(TB_SCRIPT.LAST_MODIFY_USER.eq(baseSearchCondition.getLastModifyUser()));
                }
            }
            Long appId = scriptQuery.getAppId();
            if (appId != null) {
                conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
            }

            int publicFlag = scriptQuery.isPublicScript() ? ScriptScopeEnum.PUBLIC.getValue() :
                ScriptScopeEnum.APP.getValue();
            conditions.add(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(String.valueOf(publicFlag))));

            if (StringUtils.isNotEmpty(scriptQuery.getContentKeyword())) {
                // 对于like表达式中的%_\三个特殊字符转义
                conditions.add(TB_SCRIPT_VERSION.CONTENT.like("%" +
                        StringUtil.escape(scriptQuery.getContentKeyword(),
                            new char[]{'%', '_', '\\'}, new String[]{"\\%", "\\_", "\\\\"}) + "%"
                    )
                );
            }

            String tenantId = scriptQuery.getTenantId();
            if (tenantId != null) {
                conditions.add(TB_SCRIPT.TENANT_ID.eq(tenantId));
            }

        }

        return conditions;
    }

    private List<Condition> buildScriptVersionConditionList(ScriptQuery scriptQuery,
                                                            BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildScriptConditionList(scriptQuery, baseSearchCondition);
        conditions.add(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(String.valueOf(0))));
        if (scriptQuery.getStatus() != null) {
            conditions.add(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(scriptQuery.getStatus())));
        }
        return conditions;
    }

    @Override
    public String saveScript(ScriptDTO script) {
        if (StringUtils.isBlank(script.getId())) {
            script.setId(JobUUID.getUUID());
        }

        int isPublicScript = script.isPublicScript() ? ScriptScopeEnum.PUBLIC.getValue() :
            ScriptScopeEnum.APP.getValue();
        dslContext.insertInto(TB_SCRIPT,
                TB_SCRIPT.ID,
                TB_SCRIPT.NAME,
                TB_SCRIPT.APP_ID,
                TB_SCRIPT.CATEGORY,
                TB_SCRIPT.TYPE,
                TB_SCRIPT.DESCRIPTION,
                TB_SCRIPT.IS_PUBLIC,
                TB_SCRIPT.CREATOR,
                TB_SCRIPT.LAST_MODIFY_USER,
                TB_SCRIPT.LAST_MODIFY_TIME,
                TB_SCRIPT.CREATE_TIME,
                TB_SCRIPT.TENANT_ID)
            .values(script.getId(),
                script.getName(),
                ULong.valueOf(script.getAppId()),
                UByte.valueOf(script.getCategory()),
                UByte.valueOf(script.getType()),
                script.getDescription(),
                UByte.valueOf(isPublicScript),
                script.getCreator(),
                script.getCreator(),
                ULong.valueOf(script.getCreateTime() == null ? System.currentTimeMillis() : script.getCreateTime()),
                ULong.valueOf(script.getLastModifyTime() == null ? System.currentTimeMillis() :
                    script.getLastModifyTime()),
                script.getTenantId())
            .execute();
        return script.getId();
    }

    @Override
    public void updateScriptLastModify(String scriptId, String lastModifyUser, Long lastModifyTime) {
        dslContext.update(TB_SCRIPT)
            .set(TB_SCRIPT.LAST_MODIFY_USER, lastModifyUser)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(lastModifyTime == null ?
                System.currentTimeMillis() : lastModifyTime))
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void deleteScript(String scriptId) {
        deleteScriptSoftly(scriptId);
    }

    private void deleteScriptSoftly(String scriptId) {
        dslContext.update(TB_SCRIPT).set(TB_SCRIPT.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public Long saveScriptVersion(ScriptDTO scriptDTO) {
        Long scriptVersionId = scriptDTO.getScriptVersionId();
        if (scriptVersionId == null || scriptVersionId < 0) {
            scriptVersionId = null;
        }
        Record record = dslContext.insertInto(
                TB_SCRIPT_VERSION,
                TB_SCRIPT_VERSION.ID,
                TB_SCRIPT_VERSION.SCRIPT_ID,
                TB_SCRIPT_VERSION.CONTENT,
                TB_SCRIPT_VERSION.VERSION_DESC,
                TB_SCRIPT_VERSION.VERSION,
                TB_SCRIPT_VERSION.CREATOR,
                TB_SCRIPT_VERSION.LAST_MODIFY_USER,
                TB_SCRIPT_VERSION.STATUS,
                TB_SCRIPT_VERSION.CREATE_TIME,
                TB_SCRIPT_VERSION.LAST_MODIFY_TIME)
            .values(
                scriptVersionId == null ? null : ULong.valueOf(scriptVersionId),
                scriptDTO.getId(),
                scriptDTO.getContent(),
                scriptDTO.getVersionDesc(),
                scriptDTO.getVersion(),
                scriptDTO.getCreator(),
                scriptDTO.getCreator(),
                UByte.valueOf(scriptDTO.getStatus()),
                ULong.valueOf(scriptDTO.getCreateTime() == null ? System.currentTimeMillis() :
                    scriptDTO.getCreateTime()),
                ULong.valueOf(scriptDTO.getLastModifyTime() == null ? System.currentTimeMillis() :
                    scriptDTO.getLastModifyTime()))
            .returning(TB_SCRIPT_VERSION.ID).fetchOne();
        return record != null ? record.getValue(TB_SCRIPT_VERSION.ID).longValue() : 0;
    }

    @Override
    public List<ScriptDTO> listScriptVersionsByScriptId(String scriptId) {
        Result<? extends Record> result = dslContext.select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION).join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(TB_SCRIPT.ID.eq(scriptId).and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .orderBy(TB_SCRIPT_VERSION.LAST_MODIFY_TIME.desc())
            .fetch();
        return result.map(this::extractScriptVersionData);
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public boolean isExistDuplicateName(String tenantId, Long appId, String scriptName) {
        int count = dslContext.selectCount()
            .from(TB_SCRIPT)
            .where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)))
            .and(TB_SCRIPT.TENANT_ID.eq(tenantId))
            .and(TB_SCRIPT.NAME.eq(scriptName))
            .and(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne(0, Integer.class);
        return (count >= 1);
    }

    @Override
    public void updateScriptVersionDesc(Long scriptVersionId, String desc) {
        dslContext.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.VERSION_DESC, desc)
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public void deleteScriptVersion(Long scriptVersionId) {
        dslContext.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public void deleteScriptVersionByScriptId(String scriptId) {
        deleteScriptVersionByScriptIdSoftly(scriptId);
    }

    private void deleteScriptVersionByScriptIdSoftly(String scriptId) {
        dslContext.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId)).execute();
    }

    private ScriptBasicDTO extractScriptBasicDTO(Record record) {
        if (record == null) {
            return null;
        }
        ScriptBasicDTO scriptBasicDTO = new ScriptBasicDTO();
        scriptBasicDTO.setId(record.get(TB_SCRIPT.ID));
        scriptBasicDTO.setName(record.get(TB_SCRIPT.NAME));
        scriptBasicDTO.setAppId(record.get(TB_SCRIPT.APP_ID).longValue());

        int scriptScopeValue = record.get(TB_SCRIPT.IS_PUBLIC, Integer.class);
        boolean isPublic = scriptScopeValue == ScriptScopeEnum.PUBLIC.getValue();
        scriptBasicDTO.setPublicScript(isPublic);
        scriptBasicDTO.setType(record.get(TB_SCRIPT.TYPE, Integer.class));
        scriptBasicDTO.setCategory(record.get(TB_SCRIPT.CATEGORY, Integer.class));
        return scriptBasicDTO;
    }

    private ScriptDTO extractScriptData(Record record) {
        if (record == null) {
            return null;
        }
        ScriptDTO script = new ScriptDTO();
        script.setName(record.get(TB_SCRIPT.NAME));
        script.setId(record.get(TB_SCRIPT.ID));
        script.setType(record.get(TB_SCRIPT.TYPE, Integer.class));

        int scriptScopeValue = record.get(TB_SCRIPT.IS_PUBLIC, Integer.class);
        boolean isPublic = scriptScopeValue == ScriptScopeEnum.PUBLIC.getValue();
        script.setPublicScript(isPublic);
        script.setAppId(record.get(TB_SCRIPT.APP_ID).longValue());
        script.setCategory(record.get(TB_SCRIPT.CATEGORY, Integer.class));
        script.setDescription(record.get(TB_SCRIPT.DESCRIPTION));
        script.setCreator(record.get(TB_SCRIPT.CREATOR));
        script.setCreateTime(record.get(TB_SCRIPT.CREATE_TIME).longValue());
        script.setLastModifyUser(record.get(TB_SCRIPT.LAST_MODIFY_USER));
        script.setLastModifyTime(record.get(TB_SCRIPT.LAST_MODIFY_TIME).longValue());
        script.setTenantId(record.get(TB_SCRIPT.TENANT_ID));
        return script;
    }

    private ScriptDTO extractScriptVersionData(Record record) {
        if (record == null) {
            return null;
        }
        ScriptDTO script = new ScriptDTO();
        script.setId(record.get(TB_SCRIPT.ID));
        script.setName(record.get(TB_SCRIPT.NAME));
        script.setType(record.get(TB_SCRIPT.TYPE, Integer.class));
        int scriptScopeValue = record.get(TB_SCRIPT.IS_PUBLIC, Integer.class);
        boolean isPublic = scriptScopeValue == ScriptScopeEnum.PUBLIC.getValue();
        script.setPublicScript(isPublic);
        script.setAppId(record.get(TB_SCRIPT.APP_ID).longValue());
        script.setCategory(record.get(TB_SCRIPT.CATEGORY, Integer.class));
        script.setDescription(record.get(TB_SCRIPT.DESCRIPTION));
        script.setScriptVersionId(record.get(TB_SCRIPT_VERSION.ID, Long.class));
        script.setVersion(record.get(TB_SCRIPT_VERSION.VERSION));
        script.setContent(record.get(TB_SCRIPT_VERSION.CONTENT));
        script.setCreator(record.get(TB_SCRIPT_VERSION.CREATOR));
        script.setCreateTime(record.get(TB_SCRIPT_VERSION.CREATE_TIME).longValue());
        script.setLastModifyUser(record.get(TB_SCRIPT_VERSION.LAST_MODIFY_USER));
        script.setLastModifyTime(record.get(TB_SCRIPT_VERSION.LAST_MODIFY_TIME).longValue());
        script.setStatus(record.get(TB_SCRIPT_VERSION.STATUS, Integer.class));
        script.setVersionDesc(record.get(TB_SCRIPT_VERSION.VERSION_DESC));
        script.setTenantId(record.get(TB_SCRIPT.TENANT_ID));
        return script;
    }

    @Override
    public void updateScriptVersionStatus(Long scriptVersionId, Integer status) {
        dslContext.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.STATUS, UByte.valueOf(status))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public Map<String, ScriptDTO> batchGetOnlineByScriptIds(List<String> scriptIds) {
        Result<? extends Record> result =
            dslContext.select(ALL_FIELDS)
                .from(TB_SCRIPT_VERSION)
                .join(TB_SCRIPT)
                .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID)).where(TB_SCRIPT.ID.in(scriptIds)
                    .and(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                    .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
                .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        if (!result.isEmpty()) {
            scriptVersions = result.map(this::extractScriptVersionData);
        }

        Map<String, ScriptDTO> onlineScriptMap = new HashMap<>();
        for (ScriptDTO scriptVersion : scriptVersions) {
            onlineScriptMap.put(scriptVersion.getId(), scriptVersion);
        }
        return onlineScriptMap;
    }

    @Override
    public void updateScriptDesc(String operator, String scriptId, String desc) {
        dslContext.update(TB_SCRIPT).set(TB_SCRIPT.DESCRIPTION, desc)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .set(TB_SCRIPT.LAST_MODIFY_USER, operator)
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void updateScriptName(String operator, String scriptId, String name) {
        dslContext.update(TB_SCRIPT).set(Script.SCRIPT.NAME, name)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .set(TB_SCRIPT.LAST_MODIFY_USER, operator)
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void updateScriptVersion(String operator, Long scriptVersionId, ScriptDTO scriptVersion) {
        dslContext.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.CONTENT, scriptVersion.getContent())
            .set(TB_SCRIPT_VERSION.VERSION_DESC, scriptVersion.getVersionDesc())
            .set(TB_SCRIPT_VERSION.LAST_MODIFY_USER, operator)
            .set(TB_SCRIPT_VERSION.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();

    }

    @Override
    public List<String> listScriptNames(Long appId, String keyword) {
        String likePattern = "%" + keyword + "%";
        val result =
            dslContext.select(TB_SCRIPT.NAME).from(TB_SCRIPT).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)))
                .and(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)))
                .and(TB_SCRIPT.NAME.like(likePattern))
                .orderBy(TB_SCRIPT.NAME.asc())
                .fetch();
        List<String> scriptNames = new ArrayList<>();
        result.into(record -> {
            String scriptName = record.get(TB_SCRIPT.NAME);
            scriptNames.add(scriptName);
        });
        return scriptNames;
    }

    @Override
    public List<String> listPublicScriptNames(String tenantId, String keyword) {
        String likePattern = "%" + keyword + "%";
        Result<Record1<String>> result =
            dslContext.select(TB_SCRIPT.NAME)
                .from(TB_SCRIPT)
                .where(TB_SCRIPT.TENANT_ID.eq(tenantId))
                .and(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(ScriptScopeEnum.PUBLIC.getValue())))
                .and(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)))
                .and(TB_SCRIPT.NAME.like(likePattern))
                .orderBy(TB_SCRIPT.NAME.asc())
                .fetch();
        List<String> scriptNames = new ArrayList<>();
        result.into(record -> {
            String scriptName = record.get(TB_SCRIPT.NAME);
            scriptNames.add(scriptName);
        });
        return scriptNames;
    }

    public List<ScriptDTO> listOnlineScriptForApp(long appId) {
        val result = dslContext.select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION).join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID)).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId))
                .and(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .orderBy(TB_SCRIPT_VERSION.LAST_MODIFY_TIME.desc())
            .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });
        return scriptVersions;
    }

    @Override
    public List<ScriptDTO> listOnlinePublicScript(String tenantId) {
        val result = dslContext
            .select(ALL_FIELDS)
            .from(TB_SCRIPT)
            .join(TB_SCRIPT_VERSION)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(TB_SCRIPT.TENANT_ID.eq(tenantId)
                .and(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(ScriptScopeEnum.PUBLIC.getValue())))
                .and(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .orderBy(TB_SCRIPT_VERSION.LAST_MODIFY_TIME.desc())
            .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });
        return scriptVersions;
    }

    @Override
    public PageData<ScriptDTO> listPageOnlineScript(ScriptQuery scriptCondition,
                                                    BaseSearchCondition baseSearchCondition) {
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;
        List<Condition> conditions = buildScriptConditionList(scriptCondition, baseSearchCondition);
        conditions.add(ScriptVersion.SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())));

        Long total = dslContext.selectCount()
            .from(tbScriptVersion)
            .join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .fetchOne(0, Long.class);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        val result = dslContext.select(ALL_FIELDS).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .orderBy(tbScriptVersion.LAST_MODIFY_TIME.desc())
            .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });
        PageData<ScriptDTO> scriptPageData = new PageData<>();
        scriptPageData.setTotal(total);
        scriptPageData.setPageSize(length);
        scriptPageData.setData(scriptVersions);
        scriptPageData.setStart(start);
        return scriptPageData;
    }

    @Override
    public PageData<ScriptDTO> listPageScriptVersion(ScriptQuery scriptQuery) {
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;

        Long count = getScriptVersionPageCount(scriptQuery);

        BaseSearchCondition baseSearchCondition = scriptQuery.getBaseSearchCondition();
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        val result = dslContext.select(ALL_FIELDS).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(buildScriptVersionConditionList(scriptQuery, baseSearchCondition))
            .orderBy(tbScriptVersion.LAST_MODIFY_TIME.desc())
            .limit(start, length)
            .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });
        PageData<ScriptDTO> scriptVersionPageData = new PageData<>();
        scriptVersionPageData.setTotal(count);
        scriptVersionPageData.setPageSize(length);
        scriptVersionPageData.setData(scriptVersions);
        scriptVersionPageData.setStart(start);
        return scriptVersionPageData;
    }

    private Long getScriptVersionPageCount(ScriptQuery scriptQuery) {
        BaseSearchCondition baseSearchCondition = scriptQuery.getBaseSearchCondition();
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;
        List<Condition> conditions = buildScriptVersionConditionList(scriptQuery, baseSearchCondition);
        return dslContext.selectCount()
            .from(tbScriptVersion)
            .join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .fetchOne(0, Long.class);
    }

    @Override
    public ScriptDTO getOnlineScriptVersionByScriptId(long appId, String scriptId) {
        Record record = dslContext.select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION).join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID)).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId))
                .and(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId))
                .and(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public ScriptDTO getOnlineScriptVersionByScriptId(String scriptId) {
        Record record = dslContext.select(ALL_FIELDS)
            .from(TB_SCRIPT_VERSION).join(TB_SCRIPT)
            .on(TB_SCRIPT.ID.eq(TB_SCRIPT_VERSION.SCRIPT_ID))
            .where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId))
            .and(TB_SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
            .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public Long countScriptByAppId(long appId) {
        Script tbScript = Script.SCRIPT.as("tbScript");
        return dslContext.selectCount()
            .from(tbScript)
            .where(tbScript.APP_ID.eq(ULong.valueOf(appId))
                .and(tbScript.IS_DELETED.eq(UByte.valueOf(0))))
            .fetchOne(0, Long.class);
    }

    @Override
    public boolean isExistDuplicateVersion(String scriptId, String version) {
        Integer count = dslContext.selectCount()
            .from(TB_SCRIPT_VERSION)
            .where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId))
            .and(TB_SCRIPT_VERSION.VERSION.eq(version))
            .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne(0, Integer.class);
        return (count != null && count >= 1);
    }

    @Override
    public boolean isExistAnyScript(Long appId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        return dslContext.fetchExists(TB_SCRIPT, conditions);
    }

    @Override
    public boolean isExistAnyPublicScript(String tenantId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(ScriptScopeEnum.PUBLIC.getValue())));
        conditions.add(TB_SCRIPT.TENANT_ID.eq(tenantId));
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        return dslContext.fetchExists(TB_SCRIPT, conditions);
    }

    @Override
    public Integer countScripts(Long appId, ScriptTypeEnum scriptTypeEnum,
                                JobResourceStatusEnum jobResourceStatusEnum) {
        Script tbScript = Script.SCRIPT.as("tbScript");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("tbScriptVersion");
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tbScript.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptTypeEnum != null) {
            conditions.add(tbScript.TYPE.eq(UByte.valueOf(scriptTypeEnum.getValue())));
        }
        if (jobResourceStatusEnum != null) {
            conditions.add(tbScriptVersion.STATUS.eq(UByte.valueOf(jobResourceStatusEnum.getValue())));
        }
        conditions.add(tbScript.IS_DELETED.eq(UByte.valueOf(0)));
        return dslContext.select(DSL.countDistinct(tbScript.ID))
            .from(tbScript)
            .join(tbScriptVersion)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .fetchOne(0, Integer.class);
    }

    @Override
    public Integer countScripts() {
        Script tbScript = Script.SCRIPT.as("tbScript");
        return dslContext.selectCount().from(tbScript)
            .where(tbScript.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne(0, Integer.class);
    }

    @Override
    public Integer countScriptVersions(Long appId, ScriptTypeEnum scriptTypeEnum,
                                       JobResourceStatusEnum jobResourceStatusEnum) {
        Script tbScript = Script.SCRIPT.as("tbScript");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("tbScriptVersion");
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(tbScript.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (scriptTypeEnum != null) {
            conditions.add(tbScript.TYPE.eq(UByte.valueOf(scriptTypeEnum.getValue())));
        }
        if (jobResourceStatusEnum != null) {
            conditions.add(tbScriptVersion.STATUS.eq(UByte.valueOf(jobResourceStatusEnum.getValue())));
        }
        conditions.add(tbScript.IS_DELETED.eq(UByte.valueOf(0)));
        return dslContext.select(DSL.countDistinct(tbScriptVersion.ID))
            .from(tbScript)
            .join(tbScriptVersion)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .fetchOne(0, Integer.class);
    }

    @Override
    public List<String> listAppScriptIds(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        }
        val result = dslContext.select(TB_SCRIPT.ID).from(TB_SCRIPT)
            .where(conditions)
            .fetch();
        List<String> scriptIdList = new ArrayList<>();
        result.into(record -> {
            String scriptId = record.get(TB_SCRIPT.ID);
            scriptIdList.add(scriptId);
        });
        return scriptIdList;
    }

    @Override
    public List<String> listPublicScriptIds(String tenantId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(PUBLIC_APP_ID)));
        conditions.add(TB_SCRIPT.TENANT_ID.eq(tenantId));
        val result = dslContext
            .select(TB_SCRIPT.ID)
            .from(TB_SCRIPT)
            .where(conditions)
            .fetch();
        List<String> scriptIdList = new ArrayList<>();
        result.into(record -> {
            String scriptId = record.get(TB_SCRIPT.ID);
            scriptIdList.add(scriptId);
        });
        return scriptIdList;
    }

    @Override
    public Map<String, List<Long>> listAllScriptTagsCompatible() {
        Result<? extends Record> result =
            dslContext.select(TB_SCRIPT.ID, TB_SCRIPT.TAGS).from(TB_SCRIPT)
                .where(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0))).fetch();
        Map<String, List<Long>> scriptTagsMap = new HashMap<>();
        result.map(record -> {
            String scriptId = record.get(TB_SCRIPT.ID);
            List<Long> tagIds = TagUtils.decodeDbTag(record.get(TB_SCRIPT.TAGS));
            scriptTagsMap.put(scriptId, tagIds);
            return null;
        });
        return scriptTagsMap;
    }

    @Override
    public Map<Long, JobResourceStatusEnum> batchGetScriptVersionStatus(Collection<Long> scriptVersionIds) {
        Result<? extends Record> result = dslContext
            .select(TB_SCRIPT_VERSION.ID, TB_SCRIPT_VERSION.STATUS)
            .from(TB_SCRIPT_VERSION)
            .where(TB_SCRIPT_VERSION.ID.in(scriptVersionIds)
                .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0))))
            .fetch();
        Map<Long, JobResourceStatusEnum> scriptVersionStatusMap = new HashMap<>();
        if (result.isEmpty()) {
            return scriptVersionStatusMap;
        }

        result.forEach(record ->
            scriptVersionStatusMap.put(
                record.get(TB_SCRIPT_VERSION.ID).longValue(),
                JobResourceStatusEnum.getJobResourceStatus(record.get(TB_SCRIPT_VERSION.STATUS).intValue())
            )
        );

        return scriptVersionStatusMap;
    }
}
