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

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptScopeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.generated.tables.Script;
import org.jooq.generated.tables.ScriptVersion;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @date 2019/09/19
 */
@Repository
public class ScriptDAOImpl implements ScriptDAO {
    private static final Script TB_SCRIPT = Script.SCRIPT;
    private static final ScriptVersion TB_SCRIPT_VERSION = ScriptVersion.SCRIPT_VERSION;
    private DSLContext create;

    @Autowired
    public ScriptDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext create) {
        this.create = create;
    }

    @Override
    public PageData<ScriptDTO> listPageScript(ScriptQueryDTO scriptQuery,
                                              BaseSearchCondition baseSearchCondition) {
        long count = getPageScriptCount(scriptQuery, baseSearchCondition);

        Collection<SortField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("name".equals(orderField)) {
                //正序
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_SCRIPT.NAME.asc());
                } else {
                    orderFields.add(TB_SCRIPT.NAME.desc());
                }
            } else if ("type".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_SCRIPT.TYPE.asc());
                } else {
                    orderFields.add(TB_SCRIPT.TYPE.desc());
                }
            } else if ("tags".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_SCRIPT.TAGS.asc());
                } else {
                    orderFields.add(TB_SCRIPT.TAGS.desc());
                }
            } else if ("creator".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_SCRIPT.CREATOR.asc());
                } else {
                    orderFields.add(TB_SCRIPT.CREATOR.desc());
                }
            } else if ("lastModifyTime".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.asc());
                } else {
                    orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
                }
            } else {
                orderFields.add(TB_SCRIPT.LAST_MODIFY_TIME.desc());
            }
        }

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result<Record12<String, ULong, String, UByte, String, UByte, String, ULong, String, ULong, UByte, String>> result =
            create.select(TB_SCRIPT.ID, TB_SCRIPT.APP_ID, TB_SCRIPT.NAME, TB_SCRIPT.CATEGORY,
                TB_SCRIPT.TAGS, TB_SCRIPT.TYPE, TB_SCRIPT.CREATOR, TB_SCRIPT.CREATE_TIME, TB_SCRIPT.LAST_MODIFY_USER,
                TB_SCRIPT.LAST_MODIFY_TIME, TB_SCRIPT.IS_PUBLIC, TB_SCRIPT.DESCRIPTION)
                .from(TB_SCRIPT)
                .where(buildScriptConditionList(scriptQuery, baseSearchCondition))
                .orderBy(orderFields)
                .limit(start, length).fetch();
        List<ScriptDTO> scripts = new ArrayList<>();
        if (result.size() != 0) {
            result.map((RecordMapper) record -> {
                scripts.add(extractScriptData(record));
                return null;
            });
        }

        PageData<ScriptDTO> scriptPageData = new PageData<>();
        scriptPageData.setTotal(count);
        scriptPageData.setPageSize(length);
        scriptPageData.setData(scripts);
        scriptPageData.setStart(start);
        return scriptPageData;
    }


    /**
     * 查询符合条件的脚本数量
     */
    private long getPageScriptCount(ScriptQueryDTO scriptQuery, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildScriptConditionList(scriptQuery, baseSearchCondition);
        return create.selectCount().from(TB_SCRIPT).where(conditions).fetchOne(0, Long.class);
    }

    @Override
    public ScriptDTO getScriptByScriptId(String scriptId) {
        Record result = create.select(TB_SCRIPT.ID, TB_SCRIPT.APP_ID, TB_SCRIPT.NAME, TB_SCRIPT.CATEGORY,
            TB_SCRIPT.TAGS, TB_SCRIPT.TYPE, TB_SCRIPT.IS_PUBLIC, TB_SCRIPT.CREATOR, TB_SCRIPT.CREATE_TIME,
            TB_SCRIPT.LAST_MODIFY_USER, TB_SCRIPT.LAST_MODIFY_TIME, TB_SCRIPT.DESCRIPTION).from(Script.SCRIPT)
            .where(TB_SCRIPT.ID.eq(scriptId)).fetchOne();
        return extractScriptData(result);
    }

    @Override
    public ScriptDTO getScriptVersionById(long id) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");

        Record result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.STATUS, tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER,
            tbScriptVersion.VERSION_DESC).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScriptVersion.ID.eq(ULong.valueOf(id)).and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .fetchOne();
        return extractScriptVersionData(result);
    }

    @Override
    public ScriptDTO getByScriptIdAndVersion(Long appId, String scriptId, String version) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");
        List<Condition> conditions = new ArrayList<>();
        conditions.add(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0)));
        if (scriptId != null) {
            conditions.add(tbScript.ID.eq(scriptId));
        }
        if (version != null) {
            conditions.add(tbScriptVersion.VERSION.eq(version));
        }

        Record result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.STATUS, tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER,
            tbScriptVersion.VERSION_DESC).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions)
            .fetchOne();
        return extractScriptVersionData(result);
    }

    @Override
    public List<ScriptDTO> batchGetScriptVersionsByIds(Collection<Long> scriptVersionIds) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");

        List<ULong> scriptVersionIdList = scriptVersionIds.stream().map(ULong::valueOf).collect(Collectors.toList());
        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.STATUS, tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER,
            tbScriptVersion.VERSION_DESC).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScriptVersion.ID.in(scriptVersionIdList).and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .fetch();

        if (result == null || result.size() == 0) {
            return Collections.emptyList();
        }

        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });
        return scriptVersions;
    }

    private List<Condition> buildScriptConditionList(ScriptQueryDTO scriptQuery,
                                                     BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(String.valueOf(0))));
        if (StringUtils.isNotBlank(scriptQuery.getId())) {
            conditions.add(TB_SCRIPT.ID.eq(scriptQuery.getId()));
            return conditions;
        }
        if (scriptQuery.getType() != null && scriptQuery.getType() > 0) {
            conditions.add(TB_SCRIPT.TYPE.eq(UByte.valueOf(scriptQuery.getType())));
        }
        if (StringUtils.isNotBlank(scriptQuery.getTags())) {
            String[] tags = scriptQuery.getTags().split(",");
            for (String tag : tags) {
                if (StringUtils.isNotBlank(tag)) {
                    conditions.add(TB_SCRIPT.TAGS.like("%" + tag + "%"));
                }
            }
        }
        if (StringUtils.isNotBlank(scriptQuery.getName())) {
            conditions.add(TB_SCRIPT.NAME.like("%" + scriptQuery.getName() + "%"));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
            conditions.add(TB_SCRIPT.CREATOR.eq(baseSearchCondition.getCreator()));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
            conditions.add(TB_SCRIPT.LAST_MODIFY_USER.eq(baseSearchCondition.getLastModifyUser()));
        }
        Long appId = scriptQuery.getAppId();
        if (appId != null) {
            conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        }

        Boolean isPublic = scriptQuery.getPublicScript();
        int publicFlag = ScriptScopeEnum.APP.getValue();
        if (isPublic != null && isPublic) {
            publicFlag = ScriptScopeEnum.PUBLIC.getValue();
        }
        conditions.add(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(String.valueOf(publicFlag))));
        return conditions;
    }

    private List<Condition> buildScriptVersionConditionList(ScriptQueryDTO scriptQuery,
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
        return saveScript(create, script, DateUtils.currentTimeMillis(), DateUtils.currentTimeMillis());
    }

    @Override
    public String saveScript(ScriptDTO script, long createTime, long lastModifyTime) {
        return saveScript(create, script, createTime, lastModifyTime);
    }

    @Override
    public String saveScript(DSLContext dslContext, ScriptDTO script, long createTime, long lastModifyTime) {
        if (StringUtils.isBlank(script.getId())) {
            script.setId(JobUUID.getUUID());
        }
        String dbTagList = buildDBTagIds(script.getTags());

        int isPublicScript = script.isPublicScript() ? ScriptScopeEnum.PUBLIC.getValue() :
            ScriptScopeEnum.APP.getValue();
        dslContext.insertInto(TB_SCRIPT, TB_SCRIPT.ID, TB_SCRIPT.NAME, TB_SCRIPT.APP_ID, TB_SCRIPT.CATEGORY,
            TB_SCRIPT.TYPE, TB_SCRIPT.DESCRIPTION,
            TB_SCRIPT.IS_PUBLIC, TB_SCRIPT.TAGS, TB_SCRIPT.CREATOR, TB_SCRIPT.LAST_MODIFY_USER,
            TB_SCRIPT.LAST_MODIFY_TIME, TB_SCRIPT.CREATE_TIME)
            .values(script.getId(), script.getName(), ULong.valueOf(script.getAppId()),
                UByte.valueOf(script.getCategory()),
                UByte.valueOf(script.getType()), script.getDescription(), UByte.valueOf(isPublicScript), dbTagList,
                script.getCreator(),
                script.getCreator(), ULong.valueOf(createTime), ULong.valueOf(lastModifyTime)).execute();
        return script.getId();
    }

    private String buildDBTagIds(List<TagDTO> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        List<Long> tagIdList = new ArrayList<>();
        for (TagDTO tagDTO : tags) {
            tagIdList.add(tagDTO.getId());
        }
        return TagUtils.buildDbTagList(tagIdList);
    }

    @Override
    public void updateScript(ScriptDTO script) {
        updateScript(script, DateUtils.currentTimeMillis());
    }

    @Override
    public void updateScript(ScriptDTO script, long lastModifyTime) {
        updateScript(create, script, lastModifyTime);
    }

    @Override
    public void updateScript(DSLContext dslContext, ScriptDTO script, long lastModifyTime) {
        dslContext.update(TB_SCRIPT)
            .set(TB_SCRIPT.TAGS, buildDBTagIds(script.getTags()))
            .set(TB_SCRIPT.NAME, script.getName())
            .set(TB_SCRIPT.LAST_MODIFY_USER, script.getLastModifyUser())
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(lastModifyTime))
            .where(TB_SCRIPT.ID.eq(script.getId())).execute();
    }

    @Override
    public void deleteScript(String scriptId) {
        deleteScriptSoftly(scriptId);
    }

    private void deleteScriptHardly(String scriptId) {
        create.deleteFrom(TB_SCRIPT).where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    private void deleteScriptSoftly(String scriptId) {
        create.update(TB_SCRIPT).set(TB_SCRIPT.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public Long saveScriptVersion(ScriptDTO scriptVersion) {
        return saveScriptVersion(create, scriptVersion, DateUtils.currentTimeMillis(), DateUtils.currentTimeMillis());
    }

    @Override
    public Long saveScriptVersion(ScriptDTO scriptVersion, long createTime, long lastModifyTime) {
        return saveScriptVersion(create, scriptVersion, createTime, lastModifyTime);
    }

    @Override
    public Long saveScriptVersion(DSLContext dslContext, ScriptDTO scriptDTO, long createTime, long lastModifyTime) {
        Long scriptVersionId = scriptDTO.getScriptVersionId();
        if (scriptVersionId == null || scriptVersionId < 0) {
            scriptVersionId = null;
        }
        Record record = dslContext.insertInto(TB_SCRIPT_VERSION, TB_SCRIPT_VERSION.ID, TB_SCRIPT_VERSION.SCRIPT_ID,
            TB_SCRIPT_VERSION.CONTENT, TB_SCRIPT_VERSION.VERSION_DESC, TB_SCRIPT_VERSION.VERSION,
            TB_SCRIPT_VERSION.CREATOR, TB_SCRIPT_VERSION.LAST_MODIFY_USER, TB_SCRIPT_VERSION.STATUS
            , TB_SCRIPT_VERSION.CREATE_TIME, TB_SCRIPT_VERSION.LAST_MODIFY_TIME)
            .values(scriptVersionId == null ? null : ULong.valueOf(scriptVersionId), scriptDTO.getId(),
                scriptDTO.getContent(), scriptDTO.getVersionDesc(), scriptDTO.getVersion(), scriptDTO.getCreator(),
                scriptDTO.getCreator(), UByte.valueOf(scriptDTO.getStatus()), ULong.valueOf(createTime),
                ULong.valueOf(lastModifyTime))
            .returning(TB_SCRIPT_VERSION.ID).fetchOne();
        return record.getValue(TB_SCRIPT_VERSION.ID).longValue();
    }

    @Override
    public List<ScriptDTO> listByScriptId(String scriptId) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");

        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScript.ID.eq(scriptId).and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .orderBy(tbScriptVersion.LAST_MODIFY_TIME.desc())
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
    public boolean isExistDuplicateId(Long appId, String scriptId) {
        int count = create.selectCount().from(TB_SCRIPT).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId))
            .and(TB_SCRIPT.ID.eq(scriptId))).fetchOne(0, Integer.class);
        return (count >= 1);
    }

    @Override
    public boolean isExistDuplicateName(Long appId, String scriptName) {
        int count = create.selectCount().from(TB_SCRIPT).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)))
            .and(TB_SCRIPT.NAME.eq(scriptName))
            .and(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne(0, Integer.class);
        return (count >= 1);
    }

    @Override
    public void updateScriptVersionDesc(Long scriptVersionId, String desc) {
        create.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.VERSION_DESC, desc)
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public void deleteScriptVersion(Long scriptVersionId) {
        create.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public void deleteScriptVersionByScriptId(String scriptId) {
        deleteScriptVersionByScriptIdSoftly(scriptId);
    }

    private void deleteScriptVersionByScriptIdHardly(String scriptId) {
        create.deleteFrom(TB_SCRIPT_VERSION).where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId)).execute();
    }

    private void deleteScriptVersionByScriptIdSoftly(String scriptId) {
        create.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.IS_DELETED, UByte.valueOf(1))
            .where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId)).execute();
    }

    private ScriptDTO extractScriptData(Record result) {
        ScriptDTO script = new ScriptDTO();
        script.setName(result.get(TB_SCRIPT.NAME));
        script.setId(result.get(TB_SCRIPT.ID));
        script.setType(result.get(TB_SCRIPT.TYPE, Integer.class));
        script.setTags(buildTagListFromDBTagIds(result.get(TB_SCRIPT.TAGS)));

        int scriptScopeValue = result.get(TB_SCRIPT.IS_PUBLIC, Integer.class);
        boolean isPublic = false;
        if (scriptScopeValue == ScriptScopeEnum.PUBLIC.getValue()) {
            isPublic = true;
        }
        script.setPublicScript(isPublic);
        script.setAppId(result.get(TB_SCRIPT.APP_ID).longValue());
        script.setCategory(result.get(TB_SCRIPT.CATEGORY, Integer.class));
        script.setDescription(result.get(TB_SCRIPT.DESCRIPTION));
        script.setCreator(result.get(TB_SCRIPT.CREATOR));
        script.setCreateTime(result.get(TB_SCRIPT.CREATE_TIME).longValue());
        script.setLastModifyUser(result.get(TB_SCRIPT.LAST_MODIFY_USER));
        script.setLastModifyTime(result.get(TB_SCRIPT.LAST_MODIFY_TIME).longValue());
        return script;
    }

    private List<TagDTO> buildTagListFromDBTagIds(String dbTagIds) {
        List<TagDTO> tagDTOS = new ArrayList<>();
        List<Long> tagIds = TagUtils.decodeDbTag(dbTagIds);
        if (tagIds.isEmpty()) {
            return null;
        }
        for (Long tagId : tagIds) {
            TagDTO tagDTO = new TagDTO();
            tagDTO.setId(tagId);
            tagDTOS.add(tagDTO);
        }
        return tagDTOS;
    }

    private ScriptDTO extractScriptVersionData(Record result) {
        if (result == null || result.size() == 0) {
            return null;
        }
        ScriptDTO script = new ScriptDTO();
        script.setId(result.get(TB_SCRIPT.ID));
        script.setName(result.get(TB_SCRIPT.NAME));
        script.setTags(buildTagListFromDBTagIds(result.get(TB_SCRIPT.TAGS)));
        script.setType(result.get(TB_SCRIPT.TYPE, Integer.class));
        int scriptScopeValue = result.get(TB_SCRIPT.IS_PUBLIC, Integer.class);
        boolean isPublic = false;
        if (scriptScopeValue == ScriptScopeEnum.PUBLIC.getValue()) {
            isPublic = true;
        }
        script.setPublicScript(isPublic);
        script.setAppId(result.get(TB_SCRIPT.APP_ID).longValue());
        script.setCategory(result.get(TB_SCRIPT.CATEGORY, Integer.class));
        script.setDescription(result.get(TB_SCRIPT.DESCRIPTION));
        script.setScriptVersionId(result.get("scriptVersionId", Long.class));
        script.setVersion(result.get(TB_SCRIPT_VERSION.VERSION));
        script.setContent(result.get(TB_SCRIPT_VERSION.CONTENT));
        script.setCreator(result.get(TB_SCRIPT_VERSION.CREATOR));
        script.setCreateTime(result.get(TB_SCRIPT_VERSION.CREATE_TIME).longValue());
        script.setLastModifyUser(result.get(TB_SCRIPT_VERSION.LAST_MODIFY_USER));
        script.setLastModifyTime(result.get(TB_SCRIPT_VERSION.LAST_MODIFY_TIME).longValue());
        script.setStatus(result.get(TB_SCRIPT_VERSION.STATUS, Integer.class));
        script.setVersionDesc(result.get(TB_SCRIPT_VERSION.VERSION_DESC));
        return script;
    }

    @Override
    public void updateScriptVersionStatus(Long scriptVersionId, Integer status) {
        create.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.STATUS, UByte.valueOf(status))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();
    }

    @Override
    public Map<String, ScriptDTO> batchGetOnlineByScriptIds(List<String> scriptIds) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");

        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScript.ID.in(scriptIds)
                .and(tbScriptVersion.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .fetch();
        List<ScriptDTO> scriptVersions = new ArrayList<>();
        result.into(record -> {
            ScriptDTO scriptVersion = extractScriptVersionData(record);
            if (scriptVersion != null) {
                scriptVersions.add(scriptVersion);
            }
        });

        Map<String, ScriptDTO> onlineScriptMap = new HashMap<>();
        for (ScriptDTO scriptVersion : scriptVersions) {
            onlineScriptMap.put(scriptVersion.getId(), scriptVersion);
        }
        return onlineScriptMap;
    }

    @Override
    public void updateScriptDesc(String operator, String scriptId, String desc) {
        create.update(TB_SCRIPT).set(TB_SCRIPT.DESCRIPTION, desc)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .set(TB_SCRIPT.LAST_MODIFY_USER, operator)
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void updateScriptName(String operator, String scriptId, String name) {
        create.update(TB_SCRIPT).set(Script.SCRIPT.NAME, name)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .set(TB_SCRIPT.LAST_MODIFY_USER, operator)
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void updateScriptTags(String operator, String scriptId, List<TagDTO> tags) {
        String dbTagList = buildDBTagIds(tags);
        create.update(TB_SCRIPT).set(Script.SCRIPT.TAGS, dbTagList)
            .set(TB_SCRIPT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .set(TB_SCRIPT.LAST_MODIFY_USER, operator)
            .where(TB_SCRIPT.ID.eq(scriptId)).execute();
    }

    @Override
    public void updateScriptVersion(String operator, Long scriptVersionId, ScriptDTO scriptVersion) {
        create.update(TB_SCRIPT_VERSION).set(TB_SCRIPT_VERSION.CONTENT, scriptVersion.getContent())
            .set(TB_SCRIPT_VERSION.VERSION_DESC, scriptVersion.getVersionDesc())
            .set(TB_SCRIPT_VERSION.LAST_MODIFY_USER, operator)
            .set(TB_SCRIPT_VERSION.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()))
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId))).execute();

    }

    @Override
    public List<String> listScriptNames(Long appId, String keyword) {
        String likePattern = "%" + keyword + "%";
        Result result = create.select(TB_SCRIPT.NAME).from(TB_SCRIPT).where(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)))
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
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");

        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScript.APP_ID.eq(ULong.valueOf(appId))
                .and(tbScriptVersion.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .orderBy(tbScriptVersion.LAST_MODIFY_TIME.desc())
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
    public PageData<ScriptDTO> listPageOnlineScript(ScriptQueryDTO scriptCondition,
                                                    BaseSearchCondition baseSearchCondition) {
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;
        List<Condition> conditions = buildScriptConditionList(scriptCondition, baseSearchCondition);
        conditions.add(ScriptVersion.SCRIPT_VERSION.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())));

        long total = create.selectCount().from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).
                where(conditions).fetchOne(0, Long.class);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
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
    public PageData<ScriptDTO> listPageScriptVersion(ScriptQueryDTO scriptQuery,
                                                     BaseSearchCondition baseSearchCondition) {
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;

        long count = getScriptVersionPageCount(scriptQuery, baseSearchCondition);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result result = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
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

    private long getScriptVersionPageCount(ScriptQueryDTO scriptQuery, BaseSearchCondition baseSearchCondition) {
        Script tbScript = Script.SCRIPT;
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION;
        List<Condition> conditions = buildScriptVersionConditionList(scriptQuery, baseSearchCondition);
        return create.selectCount().from(tbScriptVersion).join(tbScript).on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions).fetchOne(0, Long.class);
    }

    @Override
    public ScriptDTO getOnlineByScriptId(long appId, String scriptId) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");
        Record record = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID)).where(tbScript.APP_ID.eq(ULong.valueOf(appId))
                .and(tbScriptVersion.SCRIPT_ID.eq(scriptId))
                .and(tbScriptVersion.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
                .and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0))))
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public ScriptDTO getOnlineByScriptId(String scriptId) {
        Script tbScript = Script.SCRIPT.as("t1");
        ScriptVersion tbScriptVersion = ScriptVersion.SCRIPT_VERSION.as("t2");
        Record record = create.select(tbScript.ID, tbScript.APP_ID, tbScript.NAME, tbScript.CATEGORY,
            tbScript.TAGS, tbScript.TYPE, tbScript.IS_PUBLIC, tbScript.DESCRIPTION, tbScriptVersion.ID.as(
                "scriptVersionId"),
            tbScriptVersion.VERSION, tbScriptVersion.CONTENT, tbScriptVersion.VERSION_DESC, tbScriptVersion.STATUS,
            tbScriptVersion.CREATE_TIME,
            tbScriptVersion.CREATOR, tbScriptVersion.LAST_MODIFY_TIME, tbScriptVersion.LAST_MODIFY_USER).from(tbScriptVersion).join(tbScript)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(tbScriptVersion.SCRIPT_ID.eq(scriptId))
            .and(tbScriptVersion.STATUS.eq(UByte.valueOf(JobResourceStatusEnum.ONLINE.getValue())))
            .and(tbScriptVersion.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne();
        return extractScriptVersionData(record);
    }

    @Override
    public long countScriptByAppId(long appId) {
        Script tbScript = Script.SCRIPT.as("tbScript");
        return create.selectCount().from(tbScript)
            .where(tbScript.APP_ID.eq(ULong.valueOf(appId))
                .and(tbScript.IS_DELETED.eq(UByte.valueOf(0)))).fetchOne().value1();
    }

    @Override
    public boolean isExistDuplicateVersion(String scriptId, String version) {
        int count = create.selectCount().from(TB_SCRIPT_VERSION)
            .where(TB_SCRIPT_VERSION.SCRIPT_ID.eq(scriptId))
            .and(TB_SCRIPT_VERSION.VERSION.eq(version))
            .and(TB_SCRIPT_VERSION.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne(0, Integer.class);
        return (count >= 1);
    }

    @Override
    public boolean isExistDuplicateId(Long scriptVersionId) {
        int count = create.selectCount().from(TB_SCRIPT_VERSION)
            .where(TB_SCRIPT_VERSION.ID.eq(ULong.valueOf(scriptVersionId)))
            .fetchOne(0, Integer.class);
        return (count >= 1);
    }

    @Override
    public boolean isExistAnyScript(Long appId) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        return create.fetchExists(TB_SCRIPT, conditions);
    }

    @Override
    public boolean isExistAnyPublicScript() {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(TB_SCRIPT.IS_PUBLIC.eq(UByte.valueOf(ScriptScopeEnum.PUBLIC.getValue())));
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        return create.fetchExists(TB_SCRIPT, conditions);
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
        return create.select(DSL.countDistinct(tbScript.ID)).from(tbScript).join(tbScriptVersion)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions).fetchOne().value1();
    }

    @Override
    public Integer countScripts() {
        Script tbScript = Script.SCRIPT.as("tbScript");
        return create.selectCount().from(tbScript)
            .where(tbScript.IS_DELETED.eq(UByte.valueOf(0))).fetchOne().value1();
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
        return create.select(DSL.countDistinct(tbScriptVersion.ID)).from(tbScript).join(tbScriptVersion)
            .on(tbScript.ID.eq(tbScriptVersion.SCRIPT_ID))
            .where(conditions).fetchOne().value1();
    }

    private Integer countScriptByConditions(Collection<Condition> conditions) {
        return create.selectCount().from(TB_SCRIPT)
            .where(conditions)
            .fetchOne().value1();
    }

    @Override
    public Integer countByTag(Long appId, Long tagId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (tagId != null) {
            conditions.add(TB_SCRIPT.TAGS.like("%<" + tagId + ">%"));
        }
        return countScriptByConditions(conditions);
    }

    @Override
    public List<String> listScriptId(Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_SCRIPT.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TB_SCRIPT.APP_ID.eq(ULong.valueOf(appId)));
        }
        Result result = create.select(TB_SCRIPT.ID).from(TB_SCRIPT)
            .where(conditions)
            .fetch();
        List<String> scriptIdList = new ArrayList<>();
        result.into(record -> {
            String scriptId = record.get(TB_SCRIPT.ID);
            scriptIdList.add(scriptId);
        });
        return scriptIdList;
    }
}
