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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.api.common.constants.EnableStatusEnum;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.manager.cache.DangerousRuleCache;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.MoveDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.DangerousRuleVO;
import com.tencent.bk.job.manage.service.DangerousRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DangerousRuleServiceImpl implements DangerousRuleService {

    private final DangerousRuleDAO dangerousRuleDAO;
    private final DangerousRuleCache dangerousRuleCache;

    @Autowired
    public DangerousRuleServiceImpl(
        DangerousRuleDAO dangerousRuleDAO,
        DangerousRuleCache dangerousRuleCache) {
        this.dangerousRuleDAO = dangerousRuleDAO;
        this.dangerousRuleCache = dangerousRuleCache;
    }

    @Override
    public List<DangerousRuleVO> listDangerousRules(String username) {
        return dangerousRuleDAO.listDangerousRules().stream().map(DangerousRuleDTO::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public DangerousRuleDTO getDangerousRuleById(Long id) {
        return dangerousRuleDAO.getDangerousRuleById(id);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.CREATE_HIGH_RISK_DETECT_RULE
    )
    public DangerousRuleDTO createDangerousRule(String username, AddOrUpdateDangerousRuleReq req) {
        int scriptType = DangerousRuleDTO.encodeScriptType(req.getScriptTypeList());
        int maxPriority = dangerousRuleDAO.getMaxPriority();
        log.info(String.format("current maxPriority:%d", maxPriority));
        long id = dangerousRuleDAO.insertDangerousRule(new DangerousRuleDTO(null, req.getExpression(),
            req.getDescription(), maxPriority + 1, scriptType, username, System.currentTimeMillis(), username,
            System.currentTimeMillis(), req.getAction(), EnableStatusEnum.DISABLED.getValue()));

        // 清理缓存
        dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(req.getScriptTypeList());

        return getDangerousRuleById(id);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.EDIT_HIGH_RISK_DETECT_RULE
    )
    public DangerousRuleDTO updateDangerousRule(String username, AddOrUpdateDangerousRuleReq req) {
        int scriptType = DangerousRuleDTO.encodeScriptType(req.getScriptTypeList());
        DangerousRuleDTO existDangerousRuleDTO = dangerousRuleDAO.getDangerousRuleById(req.getId());
        if (existDangerousRuleDTO != null) {
            dangerousRuleDAO.updateDangerousRule(new DangerousRuleDTO(req.getId(),
                req.getExpression(), req.getDescription(), existDangerousRuleDTO.getPriority(), scriptType, null,
                null, username, System.currentTimeMillis(), req.getAction(), req.getStatus()));
            // 清理缓存
            List<Byte> existScriptTypes  = DangerousRuleDTO.decodeScriptType(existDangerousRuleDTO.getScriptType());
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(existScriptTypes);
        }
        return getDangerousRuleById(req.getId());
    }


    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.EDIT_HIGH_RISK_DETECT_RULE
    )
    public Integer moveDangerousRule(String username, MoveDangerousRuleReq req) {
        int dir = req.getDir();
        DangerousRuleDTO currentRuleDTO = dangerousRuleDAO.getDangerousRuleById(req.getId());
        if (currentRuleDTO == null) {
            log.info("id=%d dangerousRule not exist");
            return 0;
        }
        if (dir == -1) {
            //往上移动
            int minPriority = dangerousRuleDAO.getMinPriority();
            if (currentRuleDTO.getPriority() <= minPriority) {
                log.info("Fail to move, id=%d dangerousRule already has min priority");
                return 0;
            }
            //需要移动的情况
            DangerousRuleDTO upperRuleDTO = dangerousRuleDAO.getDangerousRuleByPriority(
                currentRuleDTO.getPriority() - 1);
            upperRuleDTO.setPriority(upperRuleDTO.getPriority() + 1);
            currentRuleDTO.setPriority(currentRuleDTO.getPriority() - 1);
            dangerousRuleDAO.updateDangerousRule(upperRuleDTO);
            dangerousRuleDAO.updateDangerousRule(currentRuleDTO);
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                DangerousRuleDTO.decodeScriptType(upperRuleDTO.getScriptType()));
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                DangerousRuleDTO.decodeScriptType(currentRuleDTO.getScriptType()));
            return 2;
        } else if (dir == 1) {
            //往下移动
            int maxPriority = dangerousRuleDAO.getMaxPriority();
            if (currentRuleDTO.getPriority() >= maxPriority) {
                log.info("Fail to move, id=%d dangerousRule already has max priority");
                return 0;
            }
            //需要移动的情况
            DangerousRuleDTO downerRuleDTO = dangerousRuleDAO.getDangerousRuleByPriority(
                currentRuleDTO.getPriority() + 1);
            if (downerRuleDTO == null) {
                return 0;
            }
            downerRuleDTO.setPriority(downerRuleDTO.getPriority() - 1);
            currentRuleDTO.setPriority(currentRuleDTO.getPriority() + 1);
            dangerousRuleDAO.updateDangerousRule(downerRuleDTO);
            dangerousRuleDAO.updateDangerousRule(currentRuleDTO);
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                DangerousRuleDTO.decodeScriptType(downerRuleDTO.getScriptType()));
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                DangerousRuleDTO.decodeScriptType(currentRuleDTO.getScriptType()));
            return 2;
        } else {
            log.warn("move of dir=%d not supported");
        }
        return 0;
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.DELETE_HIGH_RISK_DETECT_RULE
    )
    public Integer deleteDangerousRuleById(String username, Long id) {
        DangerousRuleDTO existDangerousRuleDTO = dangerousRuleDAO.getDangerousRuleById(id);
        if (existDangerousRuleDTO == null) {
            return -1;
        }
        List<DangerousRuleDTO> dangerousRuleDTOList = dangerousRuleDAO.listDangerousRules();
        for (int i = 0; i < dangerousRuleDTOList.size(); i++) {
            if (dangerousRuleDTOList.get(i).getId().equals(id)) {
                dangerousRuleDTOList.remove(i);
                break;
            }
        }
        try {
            //每次删除后维持有序
            dangerousRuleDAO.deleteDangerousRuleById(id);
            for (int i = 0; i < dangerousRuleDTOList.size(); i++) {
                DangerousRuleDTO dangerousRuleDTO = dangerousRuleDTOList.get(i);
                dangerousRuleDTO.setPriority(i + 1);
                dangerousRuleDAO.updateDangerousRule(dangerousRuleDTO);
                dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                    DangerousRuleDTO.decodeScriptType(dangerousRuleDTO.getScriptType()));
            }
            dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
                DangerousRuleDTO.decodeScriptType(existDangerousRuleDTO.getScriptType()));
        } catch (Exception e) {
            log.error(String.format("delete dangerous rule fail! id: %s", id), e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return id.intValue();
    }

    @Override
    public List<DangerousRuleVO> listDangerousRules(DangerousRuleQuery query) {
        return dangerousRuleDAO.listDangerousRules(query)
            .stream()
            .map(DangerousRuleDTO::toVO)
            .collect(Collectors.toList());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.HIGH_RISK_DETECT_RULE,
        content = EventContentConstants.EDIT_HIGH_RISK_DETECT_RULE
    )
    public DangerousRuleDTO updateDangerousRuleStatus(String userName, Long id, EnableStatusEnum status) {
        dangerousRuleDAO.updateDangerousRuleStatus(userName, id, status);
        DangerousRuleDTO dangerousRuleDTO = getDangerousRuleById(id);
        dangerousRuleCache.deleteDangerousRuleCacheByScriptTypes(
            DangerousRuleDTO.decodeScriptType(dangerousRuleDTO.getScriptType()));
        return dangerousRuleDTO;
    }
}
