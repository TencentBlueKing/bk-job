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

package com.tencent.bk.job.manage.dao.whiteip.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.whiteip.*;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.*;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.tencent.bk.job.manage.common.consts.whiteip.Keys.*;

@Slf4j
@Repository
public class WhiteIPRecordDAOImpl implements WhiteIPRecordDAO {
    private static final Logger LOG = LoggerFactory.getLogger(WhiteIPRecordDAOImpl.class);
    private static final WhiteIpRecord T_WHITE_IP_RECORD = WhiteIpRecord.WHITE_IP_RECORD;
    private static final WhiteIpAppRel T_WHITE_IP_APP_REL = WhiteIpAppRel.WHITE_IP_APP_REL;
    private static final WhiteIpIp T_WHITE_IP_IP = WhiteIpIp.WHITE_IP_IP;
    private static final WhiteIpActionScope T_WHITE_IP_ACTION_SCOPE = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE;
    private static final ActionScope T_ACTION_SCOPE = ActionScope.ACTION_SCOPE;
    private final DSLContext defaultContext;
    private WhiteIPIPDAO whiteIPIPDAO;
    private WhiteIPAppRelDAO whiteIPAppRelDAO;
    private ActionScopeDAO actionScopeDAO;
    private WhiteIPActionScopeDAO whiteIPActionScopeDAO;
    private ApplicationInfoDAO applicationInfoDAO;
    @Autowired
    public WhiteIPRecordDAOImpl(WhiteIPIPDAO whiteIPIPDAO, WhiteIPAppRelDAO whiteIPAppRelDAO,
                                ActionScopeDAO actionScopeDAO, WhiteIPActionScopeDAO whiteIPActionScopeDAO,
                                ApplicationInfoDAO applicationInfoDAO,
                                @Qualifier("job-manage-dsl-context") DSLContext defaultContext) {
        this.whiteIPIPDAO = whiteIPIPDAO;
        this.whiteIPAppRelDAO = whiteIPAppRelDAO;
        this.actionScopeDAO = actionScopeDAO;
        this.whiteIPActionScopeDAO = whiteIPActionScopeDAO;
        this.applicationInfoDAO = applicationInfoDAO;
        this.defaultContext = defaultContext;
    }

    private static OrderField<?> buildOrderField(TableField field, Integer order) {
        switch (order) {
            case 0:
                return field.desc();
            case 1:
            default:
                return field.asc();
        }
    }

    @Override
    public Long insertWhiteIPRecord(DSLContext dslContext, WhiteIPRecordDTO whiteIPRecordDTO) {
        //插入Record表
        final Record record = dslContext.insertInto(
            T_WHITE_IP_RECORD,
            T_WHITE_IP_RECORD.REMARK,
            T_WHITE_IP_RECORD.CREATOR,
            T_WHITE_IP_RECORD.CREATE_TIME,
            T_WHITE_IP_RECORD.LAST_MODIFY_USER,
            T_WHITE_IP_RECORD.LAST_MODIFY_TIME
        ).values(
            whiteIPRecordDTO.getRemark(),
            whiteIPRecordDTO.getCreator(),
            ULong.valueOf(whiteIPRecordDTO.getCreateTime()),
            whiteIPRecordDTO.getLastModifier(),
            ULong.valueOf(whiteIPRecordDTO.getLastModifyTime())
        ).returning(T_WHITE_IP_RECORD.ID).fetchOne();
        System.out.println(record);
        val recordId = record.get(T_WHITE_IP_RECORD.ID);
        //插入Record-App关联表
        whiteIPRecordDTO.getAppIdList().forEach(appId -> {
            whiteIPAppRelDAO.insertWhiteIPAppRel(dslContext, whiteIPRecordDTO.getCreator(), recordId, appId);
        });
        //插入IP表
        whiteIPRecordDTO.getIpList().forEach(ip ->
            whiteIPIPDAO.insertWhiteIPIP(dslContext, new WhiteIPIPDTO(
                null,
                recordId,
                ip.getCloudAreaId(),
                ip.getIp(),
                whiteIPRecordDTO.getCreator(),
                System.currentTimeMillis(),
                whiteIPRecordDTO.getLastModifier(),
                System.currentTimeMillis()
            ))
        );
        //插入ActionScope表
        whiteIPRecordDTO.getActionScopeList().forEach(actionScope ->
            whiteIPActionScopeDAO.insertWhiteIPActionScope(dslContext, new WhiteIPActionScopeDTO(
                null,
                recordId,
                actionScope.getActionScopeId(),
                whiteIPRecordDTO.getCreator(),
                System.currentTimeMillis(),
                whiteIPRecordDTO.getLastModifier(),
                System.currentTimeMillis()
            ))
        );
        return recordId;
    }

    @Override
    public int deleteWhiteIPRecordById(DSLContext dslContext, Long id) {
        //删关联表
        whiteIPAppRelDAO.deleteWhiteIPAppRelByRecordId(dslContext, id);
        //删IP
        whiteIPIPDAO.deleteWhiteIPIPByRecordId(dslContext, id);
        //删生效范围
        whiteIPActionScopeDAO.deleteWhiteIPActionScopeByRecordId(dslContext, id);
        //删Record
        int affectedRowNum = dslContext.deleteFrom(
            T_WHITE_IP_RECORD
        ).where(
            T_WHITE_IP_RECORD.ID.eq(id)
        ).execute();
        return affectedRowNum;
    }

    @Override
    public WhiteIPRecordDTO getWhiteIPRecordById(DSLContext dslContext, Long id) {
        //查业务List
        val appIdList = whiteIPAppRelDAO.listAppIdByRecordId(dslContext, id);
        //查IP List
        val ipList = whiteIPIPDAO.getWhiteIPIPByRecordId(dslContext, id);
        //查ActionScope List
        val actionScopeList = whiteIPActionScopeDAO.getWhiteIPActionScopeByRecordId(dslContext, id);
        //查Record
        val whiteIPRecord = dslContext.selectFrom(T_WHITE_IP_RECORD)
            .where(T_WHITE_IP_RECORD.ID.eq(id))
            .fetchOne();
        return new WhiteIPRecordDTO(
            whiteIPRecord.getId(),
            appIdList,
            whiteIPRecord.getRemark(),
            ipList,
            actionScopeList,
            whiteIPRecord.getCreator(),
            whiteIPRecord.getCreateTime().longValue(),
            whiteIPRecord.getLastModifyUser(),
            whiteIPRecord.getLastModifyTime().longValue()
        );
    }

    private List<Condition> buildConditions(String partIP, List<Long> inAppIdList, List<Long> inActionScopeIdList) {
        val tApplication = Application.APPLICATION.as("tApplication");
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val tActionScope = ActionScope.ACTION_SCOPE.as("tActionScope");
        List<Condition> conditions = new ArrayList<>();
        //专有Condition构造
        if (partIP != null) {
            conditions.add(tWhiteIPIP.IP.like("%" + partIP.trim() + "%"));
        }
        if (inAppIdList != null && !inAppIdList.isEmpty()) {
            conditions.add(tApplication.APP_ID.in(inAppIdList));
        }
        if (inActionScopeIdList != null && !inActionScopeIdList.isEmpty()) {
            conditions.add(tActionScope.ID.in(inActionScopeIdList));
        }
        return conditions;
    }

    private List<Condition> buildConditions(List<String> inIpList, List<Long> inAppIdList, List<String> appNameList,
                                            List<Long> actionScopeIdList, String creator, String lastModifyUser) {
        val tApplication = Application.APPLICATION.as("tApplication");
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val tWhiteIPRecord = WhiteIpRecord.WHITE_IP_RECORD.as("tWhiteIPRecord");
        val tActionScope = ActionScope.ACTION_SCOPE.as("tActionScope");
        List<Condition> conditions = new ArrayList<>();
        //专有Condition构造
        // 多条件模糊
        if (inIpList != null && !inIpList.isEmpty()) {
            Condition condition = tWhiteIPIP.IP.like("%" + inIpList.get(0) + "%");
            for (int i = 1; i < inIpList.size(); i++) {
                condition.or(tWhiteIPIP.IP.like("%" + inIpList.get(i) + "%"));
            }
            conditions.add(condition);
        }
        // 精确匹配
        if (inAppIdList != null && !inAppIdList.isEmpty()) {
            conditions.add(tApplication.APP_ID.in(inAppIdList));
        }
        // 多条件模糊
        if (appNameList != null && !appNameList.isEmpty()) {
            Condition condition = tApplication.APP_NAME.like("%" + appNameList.get(0) + "%");
            for (int i = 1; i < appNameList.size(); i++) {
                condition.or(tApplication.APP_NAME.like("%" + appNameList.get(i) + "%"));
            }
            conditions.add(condition);
        }
        // 精确匹配
        if (actionScopeIdList != null && !actionScopeIdList.isEmpty()) {
            conditions.add(tActionScope.ID.in(actionScopeIdList));
        }
        if (StringUtils.isNotBlank(creator)) {
            conditions.add(tWhiteIPRecord.CREATOR.like("%" + creator + "%"));
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            conditions.add(tWhiteIPRecord.LAST_MODIFY_USER.like("%" + lastModifyUser + "%"));
        }
        return conditions;
    }

    @Override
    public List<Long> listAllWhiteIPRecordId(DSLContext dslContext) {
        List<Long> recordIdList = new ArrayList<>();
        val tWhiteIPRecord = T_WHITE_IP_RECORD.as("tWhiteIPRecord");
        val query = dslContext.select(
            tWhiteIPRecord.ID
        ).from(tWhiteIPRecord);
        val records = query.fetch();
        if (records != null && records.isNotEmpty()) {
            recordIdList = records.map(it -> it.get(tWhiteIPRecord.ID)).parallelStream().collect(Collectors.toList());
        }
        return recordIdList;
    }

    @Override
    public List<WhiteIPRecordVO> listWhiteIPRecord(DSLContext dslContext, String partIP, List<Long> inAppIdList,
                                                   List<Long> inActionScopeIdList,
                                                   BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(partIP, inAppIdList, inActionScopeIdList);
        return listWhiteIPRecordByConditions(dslContext, conditions, baseSearchCondition);
    }

    @Override
    public List<WhiteIPRecordVO> listWhiteIPRecord(DSLContext dslContext, List<String> ipList, List<Long> appIdList,
                                                   List<String> appNameList, List<Long> actionScopeIdList,
                                                   String creator, String lastModifyUser,
                                                   BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(ipList, appIdList, appNameList, actionScopeIdList, creator,
            lastModifyUser);
        return listWhiteIPRecordByConditions(dslContext, conditions, baseSearchCondition);
    }

    private List<WhiteIPRecordVO> listWhiteIPRecordByConditions(DSLContext dslContext, List<Condition> conditions,
                                                                BaseSearchCondition baseSearchCondition) {
        val tApplication = Application.APPLICATION.as("tApplication");
        val tWhiteIPRecord = T_WHITE_IP_RECORD.as("tWhiteIPRecord");
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val tWhiteIPAppRel = WhiteIpAppRel.WHITE_IP_APP_REL.as("tWhiteIPAppRel");
        val tWhiteIPActionScope = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE.as("tWhiteIPActionScope");
        val tActionScope = ActionScope.ACTION_SCOPE.as("tActionScope");
        //基础Condition构造
        List<OrderField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(tWhiteIPRecord.LAST_MODIFY_TIME.desc());
        } else {
            if (tWhiteIPAppRel.APP_ID.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(tWhiteIPAppRel.APP_ID, baseSearchCondition.getOrder()));
            } else if (tWhiteIPRecord.LAST_MODIFY_USER.getName().equals(baseSearchCondition.getOrderField())) {
                orderFields.add(buildOrderField(tWhiteIPRecord.LAST_MODIFY_USER, baseSearchCondition.getOrder()));
            } else {
                orderFields.add(buildOrderField(tWhiteIPRecord.LAST_MODIFY_TIME, baseSearchCondition.getOrder()));
            }
        }
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        val query = dslContext.select(
            tWhiteIPRecord.ID.as(KEY_ID),
            DSL.max(tApplication.APP_NAME).as(KEY_APP_NAME),
            DSL.max(tApplication.APP_TYPE).as(KEY_APP_TYPE),
            DSL.max(tWhiteIPRecord.REMARK).as(KEY_REMARK),
            DSL.max(tWhiteIPRecord.CREATOR).as(KEY_CREATOR),
            DSL.max(tWhiteIPRecord.CREATE_TIME).as(KEY_CREATE_TIME),
            DSL.max(tWhiteIPRecord.LAST_MODIFY_USER).as(KEY_LAST_MODIFY_USER),
            DSL.max(tWhiteIPRecord.LAST_MODIFY_TIME).as(KEY_LAST_MODIFY_TIME),
            DSL.max(tWhiteIPIP.CLOUD_AREA_ID).as(KEY_CLOUD_AREA_ID),
            DSL.groupConcat(tWhiteIPAppRel.APP_ID).as(KEY_APP_ID_LIST),
            DSL.groupConcat(tWhiteIPIP.IP).as(KEY_IP_LIST),
            DSL.groupConcat(tActionScope.ID).as(KEY_ACTION_SCOPE_ID_LIST)
        ).from(tWhiteIPRecord)
            .join(tWhiteIPIP).on(tWhiteIPRecord.ID.eq(tWhiteIPIP.RECORD_ID))
            .leftJoin(tWhiteIPActionScope).on(tWhiteIPRecord.ID.eq(tWhiteIPActionScope.RECORD_ID))
            .leftJoin(tActionScope).on(tWhiteIPActionScope.ACTION_SCOPE_ID.eq(tActionScope.ID))
            .join(tWhiteIPAppRel).on(tWhiteIPRecord.ID.eq(tWhiteIPAppRel.RECORD_ID))
            .join(tApplication).on(tWhiteIPAppRel.APP_ID.eq(tApplication.APP_ID.cast(Long.class)))
            .where(conditions)
            .groupBy(tWhiteIPRecord.ID)
            .orderBy(orderFields)
            .limit(start, length);
        LOG.info(query.getSQL(true));
        val records = query.fetch();
        if (records != null && records.size() > 0) {
            return records.map(record -> {
                val actionScopeIdListStr = (String) record.get(KEY_ACTION_SCOPE_ID_LIST);
                List<String> actionScopeIdList = CustomCollectionUtils.getNoDuplicateList(actionScopeIdListStr, ",");
                List<ActionScopeVO> actionScopeVOList = actionScopeIdList.stream().map(actionScopeId ->
                    actionScopeDAO.getActionScopeVOById(Long.parseLong(actionScopeId))
                ).collect(Collectors.toList());
                val appIdListStr = (String) record.get(KEY_APP_ID_LIST);
                List<String> appIdList = CustomCollectionUtils.getNoDuplicateList(appIdListStr, ",");
                List<AppVO> appVOList = appIdList.stream().map(appId -> {
                    ApplicationInfoDTO applicationInfoDTO =
                        applicationInfoDAO.getCacheAppInfoById(Long.parseLong(appId));
                    return new AppVO(
                        applicationInfoDTO.getId(),
                        applicationInfoDTO.getName(),
                        applicationInfoDTO.getAppType().getValue(),
                        null,
                        null,
                        null
                    );
                }).collect(Collectors.toList());
                val recordId = (Long) record.get(KEY_ID);
                return new WhiteIPRecordVO(
                    recordId,
                    (Long) record.get(KEY_CLOUD_AREA_ID),
                    whiteIPIPDAO.getWhiteIPIPByRecordId(dslContext, recordId).stream().map(WhiteIPIPDTO::getIp).collect(Collectors.toList()),
                    actionScopeVOList,
                    appVOList,
                    (String) record.get(KEY_REMARK),
                    (String) record.get(KEY_CREATOR),
                    ((ULong) record.get(KEY_CREATE_TIME)).longValue(),
                    (String) record.get(KEY_LAST_MODIFY_USER),
                    ((ULong) record.get(KEY_LAST_MODIFY_TIME)).longValue()
                );
            });
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Long countWhiteIPRecord(DSLContext dslContext, String partIP, List<Long> inAppIdList,
                                   List<Long> inActionScopeIdList, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(partIP, inAppIdList, inActionScopeIdList);
        return countWhiteIPRecordByConditions(dslContext, conditions, baseSearchCondition);
    }

    @Override
    public Long countWhiteIPIP() {
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val query = defaultContext.selectCount().from(tWhiteIPIP);
        return query.fetchOne(0, Long.class);
    }

    @Override
    public Long countWhiteIPRecord(DSLContext dslContext, List<String> ipList, List<Long> appIdList,
                                   List<String> appNameList, List<Long> actionScopeIdList, String creator,
                                   String lastModifyUser, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(ipList, appIdList, appNameList, actionScopeIdList, creator,
            lastModifyUser);
        return countWhiteIPRecordByConditions(dslContext, conditions, baseSearchCondition);
    }

    private Long countWhiteIPRecordByConditions(DSLContext dslContext, List<Condition> conditions,
                                                BaseSearchCondition baseSearchCondition) {
        val tApplication = Application.APPLICATION.as("tApplication");
        val tWhiteIpAppRel = WhiteIpAppRel.WHITE_IP_APP_REL.as("tWhiteIpAppRel");
        val tWhiteIPRecord = T_WHITE_IP_RECORD.as("tWhiteIPRecord");
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val tActionScope = ActionScope.ACTION_SCOPE.as("tActionScope");
        val tWhiteIPActionScope = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE.as("tWhiteIPActionScope");
        val query = dslContext.select(
            DSL.countDistinct(tWhiteIPRecord.ID)
        ).from(tWhiteIPRecord)
            .join(tWhiteIPIP).on(tWhiteIPRecord.ID.eq(tWhiteIPIP.RECORD_ID))
            .leftJoin(tWhiteIPActionScope).on(tWhiteIPRecord.ID.eq(tWhiteIPActionScope.RECORD_ID))
            .leftJoin(tActionScope).on(tWhiteIPActionScope.ACTION_SCOPE_ID.eq(tActionScope.ID))
            .join(tWhiteIpAppRel).on(tWhiteIPRecord.ID.eq(tWhiteIpAppRel.RECORD_ID))
            .join(tApplication).on(tWhiteIpAppRel.APP_ID.eq(tApplication.APP_ID.cast(Long.class)))
            .where(conditions);
        val count = query.fetchOne(0, Long.class);
        return count;
    }

    @Override
    public int updateWhiteIPRecordById(DSLContext dslContext, WhiteIPRecordDTO whiteIPRecordDTO) {
        //更新Record表
        int affectedRowNum = dslContext.update(
            T_WHITE_IP_RECORD)
            .set(T_WHITE_IP_RECORD.REMARK, whiteIPRecordDTO.getRemark())
            .set(T_WHITE_IP_RECORD.LAST_MODIFY_USER, whiteIPRecordDTO.getLastModifier())
            .set(T_WHITE_IP_RECORD.LAST_MODIFY_TIME, ULong.valueOf(System.currentTimeMillis()))
            .where(T_WHITE_IP_RECORD.ID.eq(whiteIPRecordDTO.getId()))
            .execute();
        //删除业务关联表
        whiteIPAppRelDAO.deleteWhiteIPAppRelByRecordId(dslContext, whiteIPRecordDTO.getId());
        //重新插入业务关联表
        whiteIPRecordDTO.getAppIdList().forEach(appId -> {
            whiteIPAppRelDAO.insertWhiteIPAppRel(dslContext, whiteIPRecordDTO.getCreator(), whiteIPRecordDTO.getId(),
                appId);
        });
        //删除IP表
        whiteIPIPDAO.deleteWhiteIPIPByRecordId(dslContext, whiteIPRecordDTO.getId());
        //重新插入IP表
        whiteIPRecordDTO.getIpList().forEach(it -> {
            whiteIPIPDAO.insertWhiteIPIP(dslContext, it);
        });
        //删除ActionScope表
        whiteIPActionScopeDAO.deleteWhiteIPActionScopeByRecordId(dslContext, whiteIPRecordDTO.getId());
        //重新插入ActionScope表
        whiteIPRecordDTO.getActionScopeList().forEach(it -> {
            whiteIPActionScopeDAO.insertWhiteIPActionScope(dslContext, it);
        });
        return affectedRowNum;
    }

    @Override
    public List<String> getWhiteIPActionScopes(DSLContext dslContext, Collection<Long> appIds, String ip,
                                               Long cloudAreaId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_WHITE_IP_APP_REL.APP_ID.in(appIds));
        if (ip != null && !ip.isEmpty()) {
            conditions.add(T_WHITE_IP_IP.IP.eq(ip.trim()));
        }
        conditions.add(T_WHITE_IP_IP.CLOUD_AREA_ID.eq(cloudAreaId));
        return getWhiteIPActionScopesByConditions(dslContext, conditions);
    }

    @Override
    public List<String> getWhiteIPActionScopes(DSLContext dslContext, Long appId, String ip, Long cloudAreaId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_WHITE_IP_APP_REL.APP_ID.eq(appId));
        if (ip != null && !ip.isEmpty()) {
            conditions.add(T_WHITE_IP_IP.IP.eq(ip.trim()));
        }
        conditions.add(T_WHITE_IP_IP.CLOUD_AREA_ID.eq(cloudAreaId));
        return getWhiteIPActionScopesByConditions(dslContext, conditions);
    }

    private List<String> getWhiteIPActionScopesByConditions(DSLContext dslContext, List<Condition> conditions) {
        val query = dslContext.selectDistinct(T_ACTION_SCOPE.CODE).from(T_WHITE_IP_APP_REL)
            .join(T_WHITE_IP_RECORD).on(T_WHITE_IP_APP_REL.RECORD_ID.eq(T_WHITE_IP_RECORD.ID))
            .join(T_WHITE_IP_IP).on(T_WHITE_IP_RECORD.ID.eq(T_WHITE_IP_IP.RECORD_ID))
            .join(T_WHITE_IP_ACTION_SCOPE).on(T_WHITE_IP_RECORD.ID.eq(T_WHITE_IP_ACTION_SCOPE.RECORD_ID))
            .join(T_ACTION_SCOPE).on(T_WHITE_IP_ACTION_SCOPE.ACTION_SCOPE_ID.eq(T_ACTION_SCOPE.ID))
            .where(conditions);
        try {
            Result<Record1<String>> records = query.fetch();
            if (records == null || records.isEmpty()) {
                return Collections.emptyList();
            } else {
                return records.stream().map(Record1::value1).collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("error query=" + query.getSQL(ParamType.INLINED));
            throw e;
        }
    }

    @Override
    public List<CloudIPDTO> listWhiteIPByAppIds(DSLContext dslContext, Collection<Long> appIds, Long actionScopeId) {
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP.as("tWhiteIPIP");
        val tWhiteIPAppRel = WhiteIpAppRel.WHITE_IP_APP_REL.as("tWhiteIPAppRel");
        val tWhiteIPActionScope = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE.as("tWhiteIPActionScope");
        Collection<Condition> conditions = new ArrayList<>();
        if (appIds != null) {
            conditions.add(tWhiteIPAppRel.APP_ID.in(appIds));
        }
        if (actionScopeId != null) {
            conditions.add(tWhiteIPActionScope.ACTION_SCOPE_ID.eq(actionScopeId));
        }
        val query = dslContext.select(
            tWhiteIPIP.CLOUD_AREA_ID.as(KEY_CLOUD_AREA_ID),
            tWhiteIPIP.IP.as(KEY_IP)
        ).from(tWhiteIPAppRel)
            .join(tWhiteIPIP).on(tWhiteIPAppRel.RECORD_ID.eq(tWhiteIPIP.RECORD_ID))
            .join(tWhiteIPActionScope).on(tWhiteIPAppRel.RECORD_ID.eq(tWhiteIPActionScope.RECORD_ID))
            .where(conditions);
        try {
            val records = query.fetch();
            if (records.size() > 0) {
                return records.map(record -> {
                    val cloudId = (Long) record.get(KEY_CLOUD_AREA_ID);
                    val ip = (String) record.get(KEY_IP);
                    return new CloudIPDTO(cloudId, ip);
                });
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("error query={}", query.getSQL(ParamType.INLINED));
            throw e;
        }
    }
}
