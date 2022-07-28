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
import com.tencent.bk.job.manage.dao.notify.NotifyPolicyRoleTargetDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyRoleTargetChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyPolicyRoleTargetDTO;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.NotifyPolicyRoleTarget;
import org.jooq.generated.tables.records.NotifyPolicyRoleTargetRecord;
import org.jooq.types.ULong;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyPolicyRoleTargetDAOImpl implements NotifyPolicyRoleTargetDAO {
    private static final RequestIdLogger logger =
        new SimpleRequestIdLogger(LoggerFactory.getLogger(NotifyPolicyRoleTargetDAOImpl.class));
    private static final NotifyPolicyRoleTarget T_NOTIFY_POLICY_ROLE_TARGET =
        NotifyPolicyRoleTarget.NOTIFY_POLICY_ROLE_TARGET;
    private static final NotifyPolicyRoleTarget defaultTable = T_NOTIFY_POLICY_ROLE_TARGET;

    private final DSLContext dslContext;
    private final NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO;

    @Autowired
    public NotifyPolicyRoleTargetDAOImpl(DSLContext dslContext,
                                         NotifyRoleTargetChannelDAO notifyRoleTargetChannelDAO) {
        this.dslContext = dslContext;
        this.notifyRoleTargetChannelDAO = notifyRoleTargetChannelDAO;
    }

    @Override
    public Long insert(NotifyPolicyRoleTargetDTO notifyPolicyRoleTargetDTO) {
        val query = dslContext.insertInto(T_NOTIFY_POLICY_ROLE_TARGET,
            T_NOTIFY_POLICY_ROLE_TARGET.POLICY_ID,
            T_NOTIFY_POLICY_ROLE_TARGET.ROLE,
            T_NOTIFY_POLICY_ROLE_TARGET.ENABLE,
            T_NOTIFY_POLICY_ROLE_TARGET.EXTRA_OBSERVERS,
            T_NOTIFY_POLICY_ROLE_TARGET.CREATOR,
            T_NOTIFY_POLICY_ROLE_TARGET.CREATE_TIME,
            T_NOTIFY_POLICY_ROLE_TARGET.LAST_MODIFY_USER,
            T_NOTIFY_POLICY_ROLE_TARGET.LAST_MODIFY_TIME
        ).values(
            notifyPolicyRoleTargetDTO.getPolicyId(),
            notifyPolicyRoleTargetDTO.getRole(),
            notifyPolicyRoleTargetDTO.isEnable(),
            notifyPolicyRoleTargetDTO.getExtraObservers(),
            notifyPolicyRoleTargetDTO.getCreator(),
            ULong.valueOf(notifyPolicyRoleTargetDTO.getCreateTime()),
            notifyPolicyRoleTargetDTO.getLastModifier(),
            ULong.valueOf(notifyPolicyRoleTargetDTO.getLastModifyTime())
        ).returning(T_NOTIFY_POLICY_ROLE_TARGET.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            Record record = query.fetchOne();
            assert record != null;
            return record.get(T_NOTIFY_POLICY_ROLE_TARGET.ID);
        } catch (Exception e) {
            logger.errorWithRequestId(sql);
            throw e;
        }
    }

    @Override
    public int deleteByPolicyId(DSLContext dslContext, Long policyId) {
        //1.查记录
        val records = dslContext.selectFrom(defaultTable).where(
            defaultTable.POLICY_ID.eq(policyId)
        ).fetch();
        if (records.isEmpty()) {
            return 0;
        }
        //2.删从表
        records.forEach(record -> notifyRoleTargetChannelDAO.deleteByRoleTargetId(dslContext, record.getId()));
        //3.删主表
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.in(records.map(NotifyPolicyRoleTargetRecord::getId))
        ).execute();
    }

    @Override
    public List<NotifyPolicyRoleTargetDTO> listByPolicyId(DSLContext dslContext, Long policyId) {
        val records = dslContext.selectFrom(T_NOTIFY_POLICY_ROLE_TARGET).where(
            T_NOTIFY_POLICY_ROLE_TARGET.POLICY_ID.eq(policyId)
        ).fetch();
        return new ArrayList<>(records.map(record -> new NotifyPolicyRoleTargetDTO(
            record.getId(),
            record.getPolicyId(),
            record.getRole(),
            record.getEnable(),
            record.getExtraObservers(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        )));
    }
}
