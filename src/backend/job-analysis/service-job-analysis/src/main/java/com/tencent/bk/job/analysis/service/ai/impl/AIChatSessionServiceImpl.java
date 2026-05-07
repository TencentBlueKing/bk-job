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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.consts.AIChatSessionSceneTypeEnum;
import com.tencent.bk.job.analysis.dao.AIChatSessionDAO;
import com.tencent.bk.job.analysis.model.dto.AIChatSessionDTO;
import com.tencent.bk.job.analysis.service.ai.AIChatSessionService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIChatSessionServiceImpl implements AIChatSessionService {

    private final AIChatSessionDAO aiChatSessionDAO;

    public AIChatSessionServiceImpl(AIChatSessionDAO aiChatSessionDAO) {
        this.aiChatSessionDAO = aiChatSessionDAO;
    }

    @Override
    public AIChatSessionDTO getSession(Long appId, String username, Integer sceneType, String sceneResourceId) {
        validateSceneType(sceneType);
        String normalizedResourceId = normalizeResourceId(sceneResourceId);
        return aiChatSessionDAO.getSession(appId, username, sceneType, normalizedResourceId);
    }

    @Override
    public void saveSession(AIChatSessionDTO dto) {
        validateSceneType(dto.getSceneType());
        dto.setSceneResourceId(normalizeResourceId(dto.getSceneResourceId()));
        aiChatSessionDAO.upsertSession(dto);
    }

    private void validateSceneType(Integer sceneType) {
        if (sceneType == null || !AIChatSessionSceneTypeEnum.isValid(sceneType)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    private String normalizeResourceId(String sceneResourceId) {
        return StringUtils.isBlank(sceneResourceId) ? "" : sceneResourceId.trim();
    }
}
