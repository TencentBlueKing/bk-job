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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.manage.common.consts.EnableStatusEnum;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.MoveDangerousRuleReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.DangerousRuleVO;
import com.tencent.bk.job.manage.service.DangerousRuleService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DangerousRuleServiceImpl implements DangerousRuleService {

    private DSLContext dslContext;
    private DangerousRuleDAO dangerousRuleDAO;

    @Autowired
    public DangerousRuleServiceImpl(
        DSLContext dslContext,
        DangerousRuleDAO dangerousRuleDAO) {
        this.dslContext = dslContext;
        this.dangerousRuleDAO = dangerousRuleDAO;
    }

    @Override
    public List<DangerousRuleVO> listDangerousRules(String username) {
        return dangerousRuleDAO.listDangerousRules(dslContext).stream().map(DangerousRuleDTO::toVO)
            .collect(Collectors.toList());
    }

    @Override
    public Boolean addOrUpdateDangerousRule(String username, AddOrUpdateDangerousRuleReq req) {
        int scriptType = DangerousRuleDTO.encodeScriptType(req.getScriptTypeList());
        if (req.getId() == -1) {
            //新增
            int maxPriority = dangerousRuleDAO.getMaxPriority(dslContext);
            log.info(String.format("current maxPriority:%d", maxPriority));
            dangerousRuleDAO.insertDangerousRule(dslContext, new DangerousRuleDTO(null, req.getExpression(),
                req.getDescription(), maxPriority + 1, scriptType, username, System.currentTimeMillis(), username,
                System.currentTimeMillis(), req.getAction(), EnableStatusEnum.DISABLED.getValue()));
        } else {
            //更新
            DangerousRuleDTO existDangerousRuleDTO = dangerousRuleDAO.getDangerousRuleById(dslContext, req.getId());
            if (existDangerousRuleDTO != null) {
                dangerousRuleDAO.updateDangerousRule(dslContext, new DangerousRuleDTO(req.getId(),
                    req.getExpression(), req.getDescription(), existDangerousRuleDTO.getPriority(), scriptType, null,
                    null, username, System.currentTimeMillis(), req.getAction(), req.getStatus()));
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public Integer moveDangerousRule(String username, MoveDangerousRuleReq req) {
        int dir = req.getDir();
        DangerousRuleDTO currentRuleDTO = dangerousRuleDAO.getDangerousRuleById(dslContext, req.getId());
        if (currentRuleDTO == null) {
            log.info("id=%d dangerousRule not exist");
            return 0;
        }
        if (dir == -1) {
            //往上移动
            int minPriority = dangerousRuleDAO.getMinPriority(dslContext);
            if (currentRuleDTO.getPriority() <= minPriority) {
                log.info("Fail to move, id=%d dangerousRule already has min priority");
                return 0;
            }
            //需要移动的情况
            DangerousRuleDTO upperRuleDTO = dangerousRuleDAO.getDangerousRuleByPriority(dslContext,
                currentRuleDTO.getPriority() - 1);
            upperRuleDTO.setPriority(upperRuleDTO.getPriority() + 1);
            currentRuleDTO.setPriority(currentRuleDTO.getPriority() - 1);
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                dangerousRuleDAO.updateDangerousRule(context, upperRuleDTO);
                dangerousRuleDAO.updateDangerousRule(context, currentRuleDTO);
            });
            return 2;
        } else if (dir == 1) {
            //往下移动
            int maxPriority = dangerousRuleDAO.getMaxPriority(dslContext);
            if (currentRuleDTO.getPriority() >= maxPriority) {
                log.info("Fail to move, id=%d dangerousRule already has max priority");
                return 0;
            }
            //需要移动的情况
            DangerousRuleDTO downerRuleDTO = dangerousRuleDAO.getDangerousRuleByPriority(dslContext,
                currentRuleDTO.getPriority() + 1);
            if (downerRuleDTO == null) {
                return 0;
            }
            downerRuleDTO.setPriority(downerRuleDTO.getPriority() - 1);
            currentRuleDTO.setPriority(currentRuleDTO.getPriority() + 1);
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                dangerousRuleDAO.updateDangerousRule(context, downerRuleDTO);
                dangerousRuleDAO.updateDangerousRule(context, currentRuleDTO);
            });
            return 2;
        } else {
            log.warn("move of dir=%d not supported");
        }
        return 0;
    }

    @Override
    public Integer deleteDangerousRuleById(String username, Long id) {
        DangerousRuleDTO existDangerousRuleDTO = dangerousRuleDAO.getDangerousRuleById(dslContext, id);
        if (existDangerousRuleDTO == null) {
            return -1;
        }
        List<DangerousRuleDTO> dangerousRuleDTOList = dangerousRuleDAO.listDangerousRules(dslContext);
        for (int i = 0; i < dangerousRuleDTOList.size(); i++) {
            if (dangerousRuleDTOList.get(i).getId().equals(id)) {
                dangerousRuleDTOList.remove(i);
                break;
            }
        }
        //每次删除后维持有序
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            dangerousRuleDAO.deleteDangerousRuleById(context, id);
            for (int i = 0; i < dangerousRuleDTOList.size(); i++) {
                DangerousRuleDTO dangerousRuleDTO = dangerousRuleDTOList.get(i);
                dangerousRuleDTO.setPriority(i + 1);
                dangerousRuleDAO.updateDangerousRule(context, dangerousRuleDTO);
            }
        });
        return id.intValue();
    }
}
