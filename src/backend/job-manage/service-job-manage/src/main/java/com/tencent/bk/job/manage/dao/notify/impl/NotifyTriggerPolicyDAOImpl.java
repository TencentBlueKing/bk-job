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

package com.tencent.bk.job.manage.dao.notify.impl;

import com.tencent.bk.job.common.RequestIdLogger;
import com.tencent.bk.job.common.util.SimpleRequestIdLogger;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.JobRoleEnum;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.dao.notify.NotifyConfigStatusDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyPolicyRoleTargetDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyRoleTargetChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTriggerPolicyDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyPolicyRoleTargetDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyRoleTargetChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTriggerPolicyDTO;
import com.tencent.bk.job.manage.model.web.vo.notify.TriggerPolicyVO;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.NotifyTriggerPolicy;
import org.jooq.generated.tables.records.NotifyTriggerPolicyRecord;
import org.jooq.types.ULong;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyTriggerPolicyDAOImpl implements NotifyTriggerPolicyDAO {
    private static final RequestIdLogger logger =
        new SimpleRequestIdLogger(LoggerFactory.getLogger(NotifyTriggerPolicyDAOImpl.class));
    private static final NotifyTriggerPolicy T_NOTIFY_TRIGGER_POLICY = NotifyTriggerPolicy.NOTIFY_TRIGGER_POLICY;
    private static final NotifyTriggerPolicy defaultTable = T_NOTIFY_TRIGGER_POLICY;
    private final NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO;
    private final NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO;
    private final NotifyConfigStatusDAO notifyConfigStatusDAO;

    @Autowired
    public NotifyTriggerPolicyDAOImpl(NotifyPolicyRoleTargetDAO notifyPolicyRoleTargetDAO,
                                      NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO,
                                      NotifyConfigStatusDAO notifyConfigStatusDAO
    ) {
        this.notifyPolicyRoleTargetDAO = notifyPolicyRoleTargetDAO;
        this.notifyRoleTargetChannelDAO = notifyRoleTargetChannelDAO;
        this.notifyConfigStatusDAO = notifyConfigStatusDAO;
    }

    @Override
    public Long insertNotifyTriggerPolicy(DSLContext dslContext, NotifyTriggerPolicyDTO notifyTriggerPolicyDTO) {
        val query = dslContext.insertInto(T_NOTIFY_TRIGGER_POLICY,
            T_NOTIFY_TRIGGER_POLICY.APP_ID,
            T_NOTIFY_TRIGGER_POLICY.RESOURCE_ID,
            T_NOTIFY_TRIGGER_POLICY.RESOURCE_TYPE,
            T_NOTIFY_TRIGGER_POLICY.TRIGGER_USER,
            T_NOTIFY_TRIGGER_POLICY.TRIGGER_TYPE,
            T_NOTIFY_TRIGGER_POLICY.EXECUTE_STATUS,
            T_NOTIFY_TRIGGER_POLICY.CREATOR,
            T_NOTIFY_TRIGGER_POLICY.CREATE_TIME,
            T_NOTIFY_TRIGGER_POLICY.LAST_MODIFY_USER,
            T_NOTIFY_TRIGGER_POLICY.LAST_MODIFY_TIME
        ).values(
            notifyTriggerPolicyDTO.getAppId(),
            notifyTriggerPolicyDTO.getResourceId(),
            (byte) notifyTriggerPolicyDTO.getResourceType().getType(),
            notifyTriggerPolicyDTO.getTriggerUser(),
            (byte) notifyTriggerPolicyDTO.getTriggerType().getType(),
            (byte) notifyTriggerPolicyDTO.getExecuteStatus().getStatus(),
            notifyTriggerPolicyDTO.getCreator(),
            ULong.valueOf(notifyTriggerPolicyDTO.getCreateTime()),
            notifyTriggerPolicyDTO.getLastModifier(),
            ULong.valueOf(notifyTriggerPolicyDTO.getLastModifyTime())
        ).returning(T_NOTIFY_TRIGGER_POLICY.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            Record record = query.fetchOne();
            assert record != null;
            return record.get(T_NOTIFY_TRIGGER_POLICY.ID);
        } catch (Exception e) {
            logger.errorWithRequestId(sql);
            throw e;
        }
    }

    @Override
    public int deleteNotifyTriggerPolicyById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(T_NOTIFY_TRIGGER_POLICY).where(
            T_NOTIFY_TRIGGER_POLICY.ID.eq(id)
        ).execute();
    }

    private int cascadeDelete(DSLContext dslContext, Result<NotifyTriggerPolicyRecord> records) {
        if (null == records || records.isEmpty()) {
            return 0;
        }
        //1.删从表
        records.forEach(record -> notifyPolicyRoleTargetDAO.deleteByPolicyId(dslContext, record.getId()));
        //2.删主表
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.in(records.map(NotifyTriggerPolicyRecord::getId))
        ).execute();
    }

    @Override
    public int deleteAllDefaultNotifyPolicies(DSLContext dslContext) {
        // 1.查出所有记录
        val records = dslContext.selectFrom(defaultTable)
            .where(defaultTable.TRIGGER_USER.eq(NotifyConsts.DEFAULT_TRIGGER_USER))
            .and(defaultTable.APP_ID.eq(NotifyConsts.DEFAULT_APP_ID))
            .and(defaultTable.RESOURCE_ID.eq(NotifyConsts.DEFAULT_RESOURCE_ID))
            .fetch();
        // 2.级联删除
        return cascadeDelete(dslContext, records);
    }

    @Override
    public int deleteAppNotifyPolicies(DSLContext dslContext, Long appId, String triggerUser) {
        // 1.查出所有记录
        val records = dslContext.selectFrom(defaultTable)
            .where(defaultTable.TRIGGER_USER.eq(triggerUser))
            .and(defaultTable.APP_ID.eq(appId))
            .and(defaultTable.RESOURCE_ID.eq(NotifyConsts.DEFAULT_RESOURCE_ID))
            .fetch();
        // 2.级联删除
        return cascadeDelete(dslContext, records);
    }

    @Override
    public NotifyTriggerPolicyDTO getNotifyTriggerPolicyById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(T_NOTIFY_TRIGGER_POLICY).where(
            T_NOTIFY_TRIGGER_POLICY.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return new NotifyTriggerPolicyDTO(
                record.getId(),
                record.getAppId(),
                record.getResourceId(),
                ResourceTypeEnum.get(record.getResourceType()),
                record.getTriggerUser(),
                TriggerTypeEnum.get(record.getTriggerType()),
                ExecuteStatusEnum.get(record.getExecuteStatus()),
                record.getCreator(),
                record.getCreateTime().longValue(),
                record.getLastModifyUser(),
                record.getLastModifyTime().longValue()
            );
        }
    }

    @Override
    public int updateNotifyTriggerPolicyById(DSLContext dslContext, NotifyTriggerPolicyDTO notifyTriggerPolicyDTO) {
        return dslContext.update(T_NOTIFY_TRIGGER_POLICY)
            .set(T_NOTIFY_TRIGGER_POLICY.APP_ID, notifyTriggerPolicyDTO.getAppId())
            .set(T_NOTIFY_TRIGGER_POLICY.RESOURCE_ID, notifyTriggerPolicyDTO.getResourceId())
            .set(T_NOTIFY_TRIGGER_POLICY.RESOURCE_TYPE, (byte) notifyTriggerPolicyDTO.getResourceType().getType())
            .set(T_NOTIFY_TRIGGER_POLICY.TRIGGER_USER, notifyTriggerPolicyDTO.getTriggerUser())
            .set(T_NOTIFY_TRIGGER_POLICY.TRIGGER_TYPE, (byte) notifyTriggerPolicyDTO.getTriggerType().getType())
            .set(T_NOTIFY_TRIGGER_POLICY.EXECUTE_STATUS, (byte) notifyTriggerPolicyDTO.getExecuteStatus().getStatus())
            .set(T_NOTIFY_TRIGGER_POLICY.CREATOR, notifyTriggerPolicyDTO.getCreator())
            .set(T_NOTIFY_TRIGGER_POLICY.CREATE_TIME, ULong.valueOf(notifyTriggerPolicyDTO.getCreateTime()))
            .set(T_NOTIFY_TRIGGER_POLICY.LAST_MODIFY_USER, notifyTriggerPolicyDTO.getLastModifier())
            .set(T_NOTIFY_TRIGGER_POLICY.LAST_MODIFY_TIME, ULong.valueOf(notifyTriggerPolicyDTO.getLastModifyTime()))
            .where(T_NOTIFY_TRIGGER_POLICY.ID.eq(notifyTriggerPolicyDTO.getId()))
            .execute();
    }

    @Override
    public List<TriggerPolicyVO> list(DSLContext dslContext, String triggerUser, Long appId, String resourceId) {
        val resultList = new ArrayList<TriggerPolicyVO>();
        var records = dslContext.selectFrom(defaultTable)
            .where(defaultTable.TRIGGER_USER.eq(triggerUser))
            .and(defaultTable.APP_ID.eq(appId))
            .and(defaultTable.RESOURCE_ID.eq(resourceId))
            .fetch();
        if (records.isEmpty()) {
            if (!notifyConfigStatusDAO.exist(dslContext, triggerUser, appId)) {
                logger.warn(triggerUser + "未在业务(id=" + appId + ")下配置消息通知策略，采用业务无关通用默认策略");
                records = dslContext.selectFrom(defaultTable)
                    .where(defaultTable.TRIGGER_USER.eq(NotifyConsts.DEFAULT_TRIGGER_USER))
                    .and(defaultTable.APP_ID.eq(NotifyConsts.DEFAULT_APP_ID))
                    .and(defaultTable.RESOURCE_ID.eq(resourceId))
                    .fetch();
                if (records.isEmpty()) {
                    logger.info("业务无关通用默认策略未配置");
                }
            } else {
                //已配置为不发送任何通知
                val triggerTypes = TriggerTypeEnum.values();
                for (TriggerTypeEnum triggerType : triggerTypes) {
                    resultList.add(getEmptyTriggerPolicyVO(triggerType));
                }
                return resultList;
            }
        }
        val triggerTypes = TriggerTypeEnum.values();
        for (TriggerTypeEnum triggerType : triggerTypes) {
            resultList.add(getTriggerPolicyVO(dslContext, records, triggerType));
        }
        return resultList;
    }

    @Override
    public List<NotifyTriggerPolicyDTO> list(DSLContext dslContext, String triggerUser, Long appId, String resourceId
        , Integer resourceType, Integer triggerType, Integer executeStatus) {
        List<NotifyTriggerPolicyDTO> resultList;
        List<Condition> conditions = new ArrayList<>();
        if (null != triggerUser) {
            conditions.add(defaultTable.TRIGGER_USER.eq(triggerUser));
        }
        if (null != appId) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (null != resourceId) {
            conditions.add(defaultTable.RESOURCE_ID.eq(resourceId));
        }
        if (null != resourceType) {
            conditions.add(defaultTable.RESOURCE_TYPE.eq((byte) resourceType.intValue()));
        }
        if (null != triggerType) {
            conditions.add(defaultTable.TRIGGER_TYPE.eq((byte) triggerType.intValue()));
        }
        if (null != executeStatus) {
            conditions.add(defaultTable.EXECUTE_STATUS.eq((byte) executeStatus.intValue()));
        }
        var records = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .fetch();
        // 2.未指定TriggerType，查出所有TriggerType对应策略
        resultList = records.map(record ->
            new NotifyTriggerPolicyDTO(
                record.getId(),
                record.getAppId(),
                record.getResourceId(),
                ResourceTypeEnum.get(record.getResourceType()),
                record.getTriggerUser(),
                TriggerTypeEnum.get(record.getTriggerType()),
                ExecuteStatusEnum.get(record.getExecuteStatus()),
                record.getCreator(),
                record.getCreateTime().longValue(),
                record.getLastModifyUser(),
                record.getLastModifyTime().longValue()
            )
        );
        return resultList;
    }

    @Override
    public int countDefaultPolicies(DSLContext dslContext) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(NotifyConsts.DEFAULT_APP_ID));
        Integer count = dslContext.selectCount().from(defaultTable)
            .where(conditions)
            .fetchOne(0, Integer.class);
        assert count != null;
        return count;
    }

    private TriggerPolicyVO getEmptyTriggerPolicyVO(TriggerTypeEnum triggerType) {
        Map<String, List<String>> resourceStatusChannelMap = new HashMap<>();
        resourceStatusChannelMap.put(ExecuteStatusEnum.SUCCESS.name(), new ArrayList<>());
        resourceStatusChannelMap.put(ExecuteStatusEnum.FAIL.name(),
            new ArrayList<>());
        resourceStatusChannelMap.put(ExecuteStatusEnum.READY.name(),
            new ArrayList<>());
        return new TriggerPolicyVO(
            triggerType.name(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            resourceStatusChannelMap
        );
    }

    private TriggerPolicyVO getTriggerPolicyVO(DSLContext dslContext, List<NotifyTriggerPolicyRecord> records,
                                               TriggerTypeEnum triggerType) {
        List<NotifyTriggerPolicyRecord> currentTriggerTypeRecords = new ArrayList<>();
        //1.过滤出当前触发类型对应的记录
        records.forEach(record -> {
            if (triggerType.getType() == (int) record.getTriggerType()) {
                //页面执行
                currentTriggerTypeRecords.add(record);
            }
        });
        if (currentTriggerTypeRecords.isEmpty()) {
            logger.info(triggerType.getDefaultName() + " Default Policy not configed");
            //返回空数据
            return getEmptyTriggerPolicyVO(triggerType);
        }
        //页面执行
        List<String> resourceTypeList;
        List<String> appRoleList = new ArrayList<>();
        final List<String> extraObserverList = new ArrayList<>();
        Map<String, List<String>> resourceStatusChannelMap = new HashMap<>();
        resourceTypeList =
            currentTriggerTypeRecords.stream().map(NotifyTriggerPolicyRecord::getResourceType)
                .collect(Collectors.toSet()).stream().map(ResourceTypeEnum::getName).collect(Collectors.toList());
        //随机取一条记录即可，所有记录对应的通知对象是一样的
        val triggerPolicyRecord = currentTriggerTypeRecords.get(0);
        List<NotifyPolicyRoleTargetDTO> roleTargetDTOList =
            notifyPolicyRoleTargetDAO.listByPolicyId(dslContext, triggerPolicyRecord.getId());
        roleTargetDTOList.forEach(it -> {
            if (it.isEnable()) {
                val appRole = it.getRole();
                appRoleList.add(appRole);
                if (appRole.equals(JobRoleEnum.JOB_EXTRA_OBSERVER.name())) {
                    String extraObserversStr = it.getExtraObservers();
                    if (extraObserversStr != null && !extraObserversStr.isEmpty()) {
                        extraObserverList.addAll(Arrays.asList(extraObserversStr.split(NotifyConsts.SEPERATOR_COMMA)));
                    }
                }
            }
        });
        List<NotifyTriggerPolicyRecord> executeStatusDefaultRecords = currentTriggerTypeRecords.stream().filter(it -> {
            // 当前所有资源类型均采取一致的默认策略，任取一种即可
            return it.getResourceType().equals(currentTriggerTypeRecords.get(0).getResourceType());
        }).collect(Collectors.toList());
        executeStatusDefaultRecords.forEach(it -> {
            List<NotifyPolicyRoleTargetDTO> roleTargets =
                notifyPolicyRoleTargetDAO.listByPolicyId(dslContext, it.getId());
            if (roleTargets.size() > 0) {
                List<NotifyRoleTargetChannelDTO> roleTargetChannelDTOList =
                    notifyRoleTargetChannelDAO.listByRoleTargetId(dslContext,
                        roleTargets.get(0).getId());
                resourceStatusChannelMap.put(
                    ExecuteStatusEnum.getName(it.getExecuteStatus()),
                    roleTargetChannelDTOList.stream().map(NotifyRoleTargetChannelDTO::getChannel)
                        .collect(Collectors.toList())
                );
            } else {
                resourceStatusChannelMap.put(
                    ExecuteStatusEnum.getName(it.getExecuteStatus()),
                    new ArrayList<>()
                );
            }
        });
        return new TriggerPolicyVO(
            triggerType.name(),
            resourceTypeList,
            appRoleList,
            extraObserverList,
            resourceStatusChannelMap
        );
    }
}
