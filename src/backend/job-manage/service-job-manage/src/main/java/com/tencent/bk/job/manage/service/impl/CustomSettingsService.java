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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.customsetting.impl.UserCustomSettingDAO;
import com.tencent.bk.job.manage.model.dto.customsetting.UserCustomSettingDTO;
import com.tencent.bk.job.manage.model.web.request.customsetting.BatchGetCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.DeleteCustomSettingsReq;
import com.tencent.bk.job.manage.model.web.request.customsetting.SaveCustomSettingsReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CustomSettingsService {

    private final UserCustomSettingDAO userCustomSettingDAO;

    @Autowired
    public CustomSettingsService(
        UserCustomSettingDAO userCustomSettingDAO) {
        this.userCustomSettingDAO = userCustomSettingDAO;
    }

    public Map<String, Map<String, Object>> saveCustomSettings(String username,
                                                               AppResourceScope appResourceScope,
                                                               SaveCustomSettingsReq req) {
        int affectedNum = userCustomSettingDAO.batchSave(
            buildCustomSettingDTOs(
                username,
                decidePersistedAppId(appResourceScope.getAppId()),
                req.getSettingsMap()
            )
        );
        log.info("{}|{}|{} records saved", username, appResourceScope.toBasicStr(), affectedNum);
        List<String> keyList = buildKeysWithCurrentAppId(
            username,
            appResourceScope.getAppId(),
            req.getSettingsMap().keySet()
        );
        return buildCustomSettingMap(userCustomSettingDAO.batchGet(keyList));
    }

    public Map<String, Map<String, Object>> batchGetCustomSettings(String username,
                                                                   AppResourceScope appResourceScope,
                                                                   BatchGetCustomSettingsReq req) {
        if (req == null || req.getModuleList() == null) {
            return buildCustomSettingMap(userCustomSettingDAO.listAll());
        }
        List<String> keyList = buildKeysWithCurrentAppId(username, appResourceScope.getAppId(), req.getModuleList());
        return buildCustomSettingMap(userCustomSettingDAO.batchGet(keyList));
    }

    public Integer deleteCustomSettings(String username,
                                        AppResourceScope appResourceScope,
                                        DeleteCustomSettingsReq req) {
        List<String> keyList = buildKeysWithCurrentAppId(username, appResourceScope.getAppId(), req.getModuleList());
        return userCustomSettingDAO.batchDelete(keyList);
    }

    private Long decidePersistedAppId(Long currentAppId) {
        Long persistedAppId = JobConstants.PUBLIC_APP_ID;
        log.debug(
            "currentAppId={},use appId={} to save/query customSettings",
            currentAppId,
            persistedAppId
        );
        return persistedAppId;
    }

    private List<String> buildKeysWithCurrentAppId(String username,
                                                   Long appId,
                                                   Collection<String> modules) {
        return buildKeys(
            username,
            decidePersistedAppId(appId),
            modules
        );
    }

    private List<String> buildKeys(String username,
                                   Long appId,
                                   Collection<String> modules) {
        if (CollectionUtils.isEmpty(modules)) {
            return Collections.emptyList();
        }
        return modules.stream()
            .map(module -> UserCustomSettingDTO.getKey(username, appId, module))
            .collect(Collectors.toList());
    }

    private List<UserCustomSettingDTO> buildCustomSettingDTOs(String username,
                                                              Long appId,
                                                              Map<String, Map<String, Object>> settingsMap) {
        List<UserCustomSettingDTO> userCustomSettingDTOList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : settingsMap.entrySet()) {
            String module = entry.getKey();
            Map<String, Object> valuesMap = entry.getValue();
            userCustomSettingDTOList.add(buildCustomSettingDTO(username, appId, module, JsonUtils.toJson(valuesMap)));
        }
        return userCustomSettingDTOList;
    }


    private UserCustomSettingDTO buildCustomSettingDTO(String username,
                                                       Long appId,
                                                       String module,
                                                       String value) {
        UserCustomSettingDTO userCustomSettingDTO = new UserCustomSettingDTO();
        userCustomSettingDTO.setUsername(username);
        userCustomSettingDTO.setAppId(appId);
        userCustomSettingDTO.setModule(module);
        userCustomSettingDTO.setValue(value);
        userCustomSettingDTO.setLastModifyUser(username);
        return userCustomSettingDTO;
    }

    private Map<String, Map<String, Object>> buildCustomSettingMap(List<UserCustomSettingDTO> settingList) {
        Map<String, Map<String, Object>> map = new HashMap<>();
        for (UserCustomSettingDTO settingDTO : settingList) {
            map.put(settingDTO.getModule(), JsonUtils.fromJson(settingDTO.getValue(), new TypeReference<Map<String,
                Object>>() {
            }));
        }
        return map;
    }

}
