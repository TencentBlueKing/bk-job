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

package com.tencent.bk.job.manage.dao.whiteip.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.whiteip.ActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPActionScopeDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPAppRelDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPIPDAO;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPRecordDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPActionScopeDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPAppRelDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPRecordDTO;
import com.tencent.bk.job.manage.model.tables.ActionScope;
import com.tencent.bk.job.manage.model.tables.Application;
import com.tencent.bk.job.manage.model.tables.WhiteIpActionScope;
import com.tencent.bk.job.manage.model.tables.WhiteIpAppRel;
import com.tencent.bk.job.manage.model.tables.WhiteIpIp;
import com.tencent.bk.job.manage.model.tables.WhiteIpRecord;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_ACTION_SCOPE_ID_LIST;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_APP_ID_LIST;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_APP_NAME;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_APP_TYPE;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_CLOUD_AREA_ID;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_CREATE_TIME;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_CREATOR;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_HOST_ID;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_ID;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_IP;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_IPV6;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_IP_LIST;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_LAST_MODIFY_TIME;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_LAST_MODIFY_USER;
import static com.tencent.bk.job.manage.api.common.constants.whiteip.Keys.KEY_REMARK;

@Slf4j
@Repository
public class WhiteIPRecordDAOImpl implements WhiteIPRecordDAO {
    private static final Logger LOG = LoggerFactory.getLogger(WhiteIPRecordDAOImpl.class);
    private static final WhiteIpRecord T_WHITE_IP_RECORD = WhiteIpRecord.WHITE_IP_RECORD;
    private final DSLContext dslContext;
    private final WhiteIPIPDAO whiteIPIPDAO;
    private final WhiteIPAppRelDAO whiteIPAppRelDAO;
    private final ActionScopeDAO actionScopeDAO;
    private final WhiteIPActionScopeDAO whiteIPActionScopeDAO;
    private final ApplicationDAO applicationDAO;

    private final WhiteIpRecord tWhiteIPRecord = WhiteIpRecord.WHITE_IP_RECORD;
    private final WhiteIpIp tWhiteIPIP = WhiteIpIp.WHITE_IP_IP;
    private final WhiteIpAppRel tWhiteIPAppRel = WhiteIpAppRel.WHITE_IP_APP_REL;
    private final WhiteIpActionScope tWhiteIPActionScope =
        WhiteIpActionScope.WHITE_IP_ACTION_SCOPE;

    @Autowired
    public WhiteIPRecordDAOImpl(WhiteIPIPDAO whiteIPIPDAO, WhiteIPAppRelDAO whiteIPAppRelDAO,
                                ActionScopeDAO actionScopeDAO, WhiteIPActionScopeDAO whiteIPActionScopeDAO,
                                ApplicationDAO applicationDAO,
                                @Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.whiteIPIPDAO = whiteIPIPDAO;
        this.whiteIPAppRelDAO = whiteIPAppRelDAO;
        this.actionScopeDAO = actionScopeDAO;
        this.whiteIPActionScopeDAO = whiteIPActionScopeDAO;
        this.applicationDAO = applicationDAO;
        this.dslContext = dslContext;
    }

    private static OrderField<?> buildOrderField(TableField<?, ?> field, Integer order) {
        switch (order) {
            case 0:
                return field.desc();
            case 1:
            default:
                return field.asc();
        }
    }

    @SuppressWarnings("all")
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public Long insertWhiteIPRecord(WhiteIPRecordDTO whiteIPRecordDTO) {
        //插入Record表
        final Record record = dslContext.insertInto(
            T_WHITE_IP_RECORD,
            T_WHITE_IP_RECORD.TENANT_ID,
            T_WHITE_IP_RECORD.REMARK,
            T_WHITE_IP_RECORD.CREATOR,
            T_WHITE_IP_RECORD.CREATE_TIME,
            T_WHITE_IP_RECORD.LAST_MODIFY_USER,
            T_WHITE_IP_RECORD.LAST_MODIFY_TIME
        ).values(
            JobContextUtil.getTenantId(),
            whiteIPRecordDTO.getRemark(),
            whiteIPRecordDTO.getCreator(),
            ULong.valueOf(whiteIPRecordDTO.getCreateTime()),
            whiteIPRecordDTO.getLastModifier(),
            ULong.valueOf(whiteIPRecordDTO.getLastModifyTime())
        ).returning(T_WHITE_IP_RECORD.ID).fetchOne();
        val recordId = record.get(T_WHITE_IP_RECORD.ID);
        //插入Record-App关联表
        whiteIPRecordDTO.getAppIdList().forEach(appId ->
            whiteIPAppRelDAO.insertWhiteIPAppRel(whiteIPRecordDTO.getCreator(), recordId, appId));
        //插入IP表
        whiteIPRecordDTO.getIpList().forEach(ip ->
            whiteIPIPDAO.insertWhiteIPIP(new WhiteIPIPDTO(
                null,
                recordId,
                ip.getCloudAreaId(),
                ip.getHostId(),
                ip.getIp(),
                ip.getIpv6(),
                whiteIPRecordDTO.getCreator(),
                System.currentTimeMillis(),
                whiteIPRecordDTO.getLastModifier(),
                System.currentTimeMillis()
            ))
        );
        //插入ActionScope表
        whiteIPRecordDTO.getActionScopeList().forEach(actionScope ->
            whiteIPActionScopeDAO.insertWhiteIPActionScope(new WhiteIPActionScopeDTO(
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

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int deleteWhiteIPRecordById(Long id) {
        //TODO:判断租户下该id是否存在，避免越权删除其他租户数据
        //删关联表
        int deletedAppRelNum = whiteIPAppRelDAO.deleteWhiteIPAppRelByRecordId(id);
        //删IP
        int deletedIpNum = whiteIPIPDAO.deleteWhiteIPIPByRecordId(id);
        //删生效范围
        int deletedActionScopeNum = whiteIPActionScopeDAO.deleteWhiteIPActionScopeByRecordId(id);
        log.debug(
            "deletedAppRelNum={},deletedIpNum={},deletedActionScopeNum={}",
            deletedAppRelNum,
            deletedIpNum,
            deletedActionScopeNum
        );
        //删Record
        return dslContext.deleteFrom(T_WHITE_IP_RECORD)
            .where(buildIdContions(id))
            .execute();
    }

    private List<Condition> buildIdContions(Long id) {
        List<Condition> conditions = buildTenantContions();
        conditions.add(T_WHITE_IP_RECORD.ID.eq(id));
        return conditions;
    }

    private List<Condition> buildTenantContions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_WHITE_IP_RECORD.TENANT_ID.eq(JobContextUtil.getTenantId()));
        return conditions;
    }

    @Override
    public WhiteIPRecordDTO getWhiteIPRecordById(Long id) {
        //查业务List
        val appIdList = whiteIPAppRelDAO.listAppIdByRecordId(id);
        //查IP List
        val ipList = whiteIPIPDAO.getWhiteIPIPByRecordId(id);
        //查ActionScope List
        val actionScopeList = whiteIPActionScopeDAO.getWhiteIPActionScopeByRecordId(id);
        //查Record
        val whiteIPRecord = dslContext.selectFrom(T_WHITE_IP_RECORD)
            .where(buildIdContions(id))
            .fetchOne();
        if (whiteIPRecord == null) {
            throw new DataRetrievalFailureException("cannot find whiteIpRecord by id " + id);
        }
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

    private List<Condition> buildConditions(List<String> inIpList, List<Long> inAppIdList, List<String> appNameList,
                                            List<Long> actionScopeIdList, String creator, String lastModifyUser) {
        val tApplication = Application.APPLICATION;
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP;
        val tWhiteIPRecord = WhiteIpRecord.WHITE_IP_RECORD;
        val tActionScope = ActionScope.ACTION_SCOPE;
        List<Condition> conditions = buildTenantContions();
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
    public List<WhiteIPRecordVO> listWhiteIPRecord(List<String> ipList, List<Long> appIdList,
                                                   List<String> appNameList, List<Long> actionScopeIdList,
                                                   String creator, String lastModifyUser,
                                                   BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(ipList, appIdList, appNameList, actionScopeIdList, creator,
            lastModifyUser);
        return listWhiteIPRecordByConditions(conditions, baseSearchCondition);
    }

    private boolean isAllScope(List<Long> appIdList) {
        return appIdList != null
            && !appIdList.isEmpty()
            && new HashSet<>(appIdList).contains(JobConstants.PUBLIC_APP_ID);
    }

    private List<WhiteIPRecordVO> listWhiteIPRecordByConditions(List<Condition> conditions,
                                                                BaseSearchCondition baseSearchCondition) {
        val tApplication = Application.APPLICATION;
        val tWhiteIPRecord = T_WHITE_IP_RECORD;
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP;
        val tWhiteIPAppRel = WhiteIpAppRel.WHITE_IP_APP_REL;
        val tWhiteIPActionScope = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE;
        val tActionScope = ActionScope.ACTION_SCOPE;
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
            .leftJoin(tApplication).on(tWhiteIPAppRel.APP_ID.eq(tApplication.APP_ID.cast(Long.class)))
            .where(conditions)
            .groupBy(tWhiteIPRecord.ID)
            .orderBy(orderFields)
            .limit(start, length);
        LOG.info(query.getSQL(ParamType.INLINED));
        val records = query.fetch();
        if (!records.isEmpty()) {
            return records.map(record -> {
                val actionScopeIdListStr = (String) record.get(KEY_ACTION_SCOPE_ID_LIST);
                List<String> actionScopeIdList = CustomCollectionUtils.getNoDuplicateList(actionScopeIdListStr, ",");
                List<ActionScopeVO> actionScopeVOList = actionScopeIdList.stream().map(actionScopeId ->
                    actionScopeDAO.getActionScopeVOById(Long.parseLong(actionScopeId))
                ).collect(Collectors.toList());
                val appIdListStr = (String) record.get(KEY_APP_ID_LIST);
                List<Long> appIdList = CustomCollectionUtils.getNoDuplicateList(appIdListStr, ",").stream()
                    .map(Long::parseLong).collect(Collectors.toList());
                boolean allScope = isAllScope(appIdList);
                List<ScopeVO> scopeVOList = null;
                if (!allScope) {
                    scopeVOList = appIdList.stream().map(appId -> {
                        ApplicationDTO applicationDTO =
                            applicationDAO.getAppById(appId);
                        ScopeVO scopeVO = new ScopeVO();
                        if (applicationDTO == null) {
                            scopeVO.setName("[Deleted:" + appId + "]");
                        } else {
                            scopeVO.setName(applicationDTO.getName());
                            scopeVO.setScopeType(applicationDTO.getScope().getType().getValue());
                            scopeVO.setScopeId(applicationDTO.getScope().getId());
                        }
                        return scopeVO;
                    }).collect(Collectors.toList());
                }
                val recordId = (Long) record.get(KEY_ID);
                WhiteIPRecordVO whiteIPRecord = new WhiteIPRecordVO();
                whiteIPRecord.setId(recordId);
                whiteIPRecord.setCloudAreaId((Long) record.get(KEY_CLOUD_AREA_ID));
                whiteIPRecord.setHostList(whiteIPIPDAO.getWhiteIPIPByRecordId(recordId).stream()
                    .map(WhiteIPIPDTO::extractWhiteIPHostVO).collect(Collectors.toList()));
                whiteIPRecord.setActionScopeList(actionScopeVOList);
                whiteIPRecord.setAllScope(allScope);
                whiteIPRecord.setScopeList(scopeVOList);
                whiteIPRecord.setRemark((String) record.get(KEY_REMARK));
                whiteIPRecord.setCreator((String) record.get(KEY_CREATOR));
                whiteIPRecord.setCreateTime(JooqDataTypeUtil.getLongFromULong((ULong) record.get(KEY_CREATE_TIME)));
                whiteIPRecord.setLastModifier((String) record.get(KEY_LAST_MODIFY_USER));
                whiteIPRecord.setLastModifyTime(
                    JooqDataTypeUtil.getLongFromULong(((ULong) record.get(KEY_LAST_MODIFY_TIME)))
                );
                return whiteIPRecord;
            });
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Long countWhiteIPIP() {
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP;
        val query = dslContext.selectCount()
            .from(tWhiteIPIP)
            .where(buildTenantContions());
        return query.fetchOne(0, Long.class);
    }

    @Override
    public Long countWhiteIPRecord(List<String> ipList, List<Long> appIdList,
                                   List<String> appNameList, List<Long> actionScopeIdList, String creator,
                                   String lastModifyUser, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditions(ipList, appIdList, appNameList, actionScopeIdList, creator,
            lastModifyUser);
        return countWhiteIPRecordByConditions(conditions);
    }

    private Long countWhiteIPRecordByConditions(List<Condition> conditions) {
        val tApplication = Application.APPLICATION;
        val tWhiteIpAppRel = WhiteIpAppRel.WHITE_IP_APP_REL;
        val tWhiteIPRecord = T_WHITE_IP_RECORD;
        val tWhiteIPIP = WhiteIpIp.WHITE_IP_IP;
        val tActionScope = ActionScope.ACTION_SCOPE;
        val tWhiteIPActionScope = WhiteIpActionScope.WHITE_IP_ACTION_SCOPE;
        val query = dslContext.select(
                DSL.countDistinct(tWhiteIPRecord.ID)
            ).from(tWhiteIPRecord)
            .join(tWhiteIPIP).on(tWhiteIPRecord.ID.eq(tWhiteIPIP.RECORD_ID))
            .leftJoin(tWhiteIPActionScope).on(tWhiteIPRecord.ID.eq(tWhiteIPActionScope.RECORD_ID))
            .leftJoin(tActionScope).on(tWhiteIPActionScope.ACTION_SCOPE_ID.eq(tActionScope.ID))
            .join(tWhiteIpAppRel).on(tWhiteIPRecord.ID.eq(tWhiteIpAppRel.RECORD_ID))
            .leftJoin(tApplication).on(tWhiteIpAppRel.APP_ID.eq(tApplication.APP_ID.cast(Long.class)))
            .where(conditions);
        return query.fetchOne(0, Long.class);
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int updateWhiteIPRecordById(WhiteIPRecordDTO whiteIPRecordDTO) {
        //更新Record表
        int affectedRowNum = dslContext.update(
                T_WHITE_IP_RECORD)
            .set(T_WHITE_IP_RECORD.REMARK, whiteIPRecordDTO.getRemark())
            .set(T_WHITE_IP_RECORD.LAST_MODIFY_USER, whiteIPRecordDTO.getLastModifier())
            .set(T_WHITE_IP_RECORD.LAST_MODIFY_TIME, ULong.valueOf(System.currentTimeMillis()))
            .where(buildIdContions(whiteIPRecordDTO.getId()))
            .execute();
        //删除业务关联表
        whiteIPAppRelDAO.deleteWhiteIPAppRelByRecordId(whiteIPRecordDTO.getId());
        //重新插入业务关联表
        whiteIPRecordDTO.getAppIdList().forEach(appId ->
            whiteIPAppRelDAO.insertWhiteIPAppRel(
                whiteIPRecordDTO.getCreator(),
                whiteIPRecordDTO.getId(),
                appId
            )
        );
        //删除IP表
        whiteIPIPDAO.deleteWhiteIPIPByRecordId(whiteIPRecordDTO.getId());
        //重新插入IP表
        whiteIPRecordDTO.getIpList().forEach(whiteIPIPDAO::insertWhiteIPIP);
        //删除ActionScope表
        whiteIPActionScopeDAO.deleteWhiteIPActionScopeByRecordId(whiteIPRecordDTO.getId());
        //重新插入ActionScope表
        whiteIPRecordDTO.getActionScopeList().forEach(whiteIPActionScopeDAO::insertWhiteIPActionScope);
        return affectedRowNum;
    }

    private Collection<Condition> buildConditions(Collection<Long> appIds,
                                                  Long actionScopeId) {
        Collection<Condition> conditions = buildTenantContions();
        if (appIds != null) {
            conditions.add(tWhiteIPAppRel.APP_ID.in(appIds));
        }
        if (actionScopeId != null) {
            conditions.add(tWhiteIPActionScope.ACTION_SCOPE_ID.eq(actionScopeId));
        }
        return conditions;
    }

    private Collection<Condition> buildHostIdsConditions(Collection<Long> appIds,
                                                         Long actionScopeId,
                                                         Collection<Long> hostIds) {
        Collection<Condition> conditions = buildConditions(appIds, actionScopeId);
        if (hostIds != null) {
            conditions.add(tWhiteIPIP.HOST_ID.in(hostIds));
        }
        return conditions;
    }

    private Collection<Condition> buildIpsConditions(Collection<Long> appIds,
                                                     Long actionScopeId,
                                                     Collection<String> ips) {
        Collection<Condition> conditions = buildConditions(appIds, actionScopeId);
        if (ips != null) {
            conditions.add(tWhiteIPIP.IP.in(ips));
        }
        return conditions;
    }

    private Collection<Condition> buildIpv6sConditions(Collection<Long> appIds,
                                                       Long actionScopeId,
                                                       Collection<String> ipv6s) {
        Collection<Condition> conditions = buildConditions(appIds, actionScopeId);
        if (ipv6s != null) {
            conditions.add(tWhiteIPIP.IP_V6.in(ipv6s));
        }
        return conditions;
    }

    @Override
    public List<HostDTO> listWhiteIPHost(Collection<Long> appIds, Long actionScopeId, Collection<Long> hostIds) {
        Collection<Condition> conditions = buildHostIdsConditions(appIds, actionScopeId, hostIds);
        return listWhiteIPHostByConditions(conditions);
    }

    @Override
    public List<HostDTO> listWhiteIPHostByIps(Collection<Long> appIds, Long actionScopeId, Collection<String> ips) {
        Collection<Condition> conditions = buildIpsConditions(appIds, actionScopeId, ips);
        return listWhiteIPHostByConditions(conditions);
    }

    @Override
    public List<HostDTO> listWhiteIPHostByIpv6s(Collection<Long> appIds, Long actionScopeId, Collection<String> ipv6s) {
        Collection<Condition> conditions = buildIpv6sConditions(appIds, actionScopeId, ipv6s);
        return listWhiteIPHostByConditions(conditions);
    }

    public List<HostDTO> listWhiteIPHostByConditions(Collection<Condition> conditions) {
        val query = dslContext.select(
                tWhiteIPIP.CLOUD_AREA_ID.as(KEY_CLOUD_AREA_ID),
                tWhiteIPIP.HOST_ID.as(KEY_HOST_ID),
                tWhiteIPIP.IP.as(KEY_IP),
                tWhiteIPIP.IP_V6.as(KEY_IPV6)
            ).from(tWhiteIPAppRel)
            .join(tWhiteIPRecord).on(tWhiteIPAppRel.RECORD_ID.eq(tWhiteIPRecord.ID))
            .join(tWhiteIPIP).on(tWhiteIPAppRel.RECORD_ID.eq(tWhiteIPIP.RECORD_ID))
            .join(tWhiteIPActionScope).on(tWhiteIPAppRel.RECORD_ID.eq(tWhiteIPActionScope.RECORD_ID))
            .where(conditions);
        try {
            val records = query.fetch();
            return records.map(record -> {
                val hostId = (Long) record.get(KEY_HOST_ID);
                val cloudId = (Long) record.get(KEY_CLOUD_AREA_ID);
                val ip = (String) record.get(KEY_IP);
                return new HostDTO(hostId, cloudId, ip);
            });
        } catch (Exception e) {
            log.error("error query={}", query.getSQL(ParamType.INLINED));
            throw e;
        }
    }

    @Override
    public List<WhiteIPRecordDTO> listAllWhiteIPRecord(String tenantId) {
        //查Record
        val tWhiteIPRecord = T_WHITE_IP_RECORD;
        val whiteIPRecords = dslContext.select(T_WHITE_IP_RECORD.ID)
            .from(T_WHITE_IP_RECORD)
            .where(tWhiteIPRecord.TENANT_ID.eq(tenantId))
            .fetch();
        List<WhiteIPAppRelDTO> whiteIPAppRelList = new ArrayList<>();
        List<WhiteIPIPDTO> whiteIPIPList = new ArrayList<>();
        List<WhiteIPActionScopeDTO> whiteIPActionScopeList = new ArrayList<>();
        List<Long> recordIdList;
        if (CollectionUtils.isNotEmpty(whiteIPRecords)) {
            recordIdList =
                new ArrayList<>(whiteIPRecords.map(it -> it.get(tWhiteIPRecord.ID)));

            int maxInCount = 1000;
            List<List<Long>> recordIdsList = Lists.partition(new ArrayList<>(recordIdList), maxInCount);

            for (List<Long> idList : recordIdsList) {
                whiteIPAppRelList.addAll(whiteIPAppRelDAO.listAppRelByRecordIds(idList));
                whiteIPIPList.addAll(whiteIPIPDAO.listWhiteIPIPByRecordIds(idList));
                whiteIPActionScopeList.addAll(whiteIPActionScopeDAO.listWhiteIPActionScopeByRecordIds(idList));
            }
        }

        Map<Long, List<WhiteIPAppRelDTO>> whiteIPAppRelMap = whiteIPAppRelList.stream().collect(
            Collectors.groupingBy(WhiteIPAppRelDTO::getRecordId));
        Map<Long, List<WhiteIPIPDTO>> whiteIPIPMap = whiteIPIPList.stream().collect(
            Collectors.groupingBy(WhiteIPIPDTO::getRecordId));
        Map<Long, List<WhiteIPActionScopeDTO>> whiteIPActionScopeMap = whiteIPActionScopeList.stream().collect(
            Collectors.groupingBy(WhiteIPActionScopeDTO::getRecordId));

        if (CollectionUtils.isNotEmpty(whiteIPRecords)) {
            return whiteIPRecords.stream().map(record ->
                new WhiteIPRecordDTO(
                    record.get(tWhiteIPRecord.ID),
                    whiteIPAppRelMap.get(record.get(tWhiteIPRecord.ID)) == null ? new ArrayList<>() :
                        whiteIPAppRelMap.get(record.get(tWhiteIPRecord.ID)).stream()
                            .map(WhiteIPAppRelDTO::getAppId).collect(Collectors.toList()),
                    null,
                    whiteIPIPMap.get(record.get(tWhiteIPRecord.ID)) == null ? new ArrayList<>() :
                        whiteIPIPMap.get(record.get(tWhiteIPRecord.ID)),
                    whiteIPActionScopeMap.get(record.get(tWhiteIPRecord.ID)) == null ? new ArrayList<>() :
                        whiteIPActionScopeMap.get(record.get(tWhiteIPRecord.ID)),
                    null,
                    null,
                    null,
                    null
                )
            ).collect(Collectors.toList());
        }
        return null;
    }
}
