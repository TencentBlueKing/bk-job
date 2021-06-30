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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.manage.api.inner.ServiceAppSetResource;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceAddAppSetRequest;
import com.tencent.bk.job.manage.model.inner.request.ServiceUpdateAppSetRequest;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceAppSetResourceImpl implements ServiceAppSetResource {

    private final ApplicationInfoDAO applicationDAO;
    private final ApplicationService applicationService;
    private final EsbUserInfoDAO userDAO;

    @Autowired
    public ServiceAppSetResourceImpl(ApplicationInfoDAO applicationDAO, ApplicationService applicationService,
                                     EsbUserInfoDAO userDAO) {
        this.applicationDAO = applicationDAO;
        this.applicationService = applicationService;
        this.userDAO = userDAO;
    }


    @Override
    public ServiceResponse<ServiceApplicationDTO> queryAppSetById(Long appId) {
        ServiceApplicationDTO appInfo = getAppSet(appId);
        if (appInfo == null) {
            return ServiceResponse.buildCommonFailResp("App is not exist");
        }
        return ServiceResponse.buildSuccessResp(appInfo);
    }

    @Override
    public ServiceResponse<List<ServiceApplicationDTO>> listAppSet() {
        List<ApplicationInfoDTO> appSetList = applicationDAO.listAppInfoByType(AppTypeEnum.APP_SET);
        if (appSetList == null || appSetList.isEmpty()) {
            return ServiceResponse.buildSuccessResp(null);
        }
        return ServiceResponse.buildSuccessResp(appSetList.stream().map(this::convertToServiceApp)
            .collect(Collectors.toList()));
    }

    private ServiceApplicationDTO getAppSet(Long appId) {
        ApplicationInfoDTO appInfo = applicationDAO.getAppInfoById(appId);
        if (appInfo == null) {
            return null;
        }
        return convertToServiceApp(appInfo);
    }

    private ServiceApplicationDTO convertToServiceApp(ApplicationInfoDTO appInfo) {
        ServiceApplicationDTO app = new ServiceApplicationDTO();
        app.setId(appInfo.getId());
        app.setSubAppIds(appInfo.getSubAppIds());
        app.setName(appInfo.getName());
        app.setAppType(appInfo.getAppType().getValue());
        app.setOwner(appInfo.getBkSupplierAccount());
        app.setMaintainers(appInfo.getMaintainers());
        app.setOperateDeptId(appInfo.getOperateDeptId());
        app.setTimeZone(appInfo.getTimeZone());
        return app;
    }

    @Override
    public ServiceResponse<ServiceApplicationDTO> addAppSet(ServiceAddAppSetRequest request) {
        log.info("Add app-set, request:{}", request);
        checkAddAppSetRequest(request);

        ApplicationInfoDTO app = applicationDAO.getAppInfoById(request.getId());
        if (app != null) {
            log.warn("App-set is exist!");
            return ServiceResponse.buildCommonFailResp("App-Set is exist, can not add again!");
        }

        ApplicationInfoDTO appInfo = new ApplicationInfoDTO();
        appInfo.setId(request.getId());
        appInfo.setName(request.getName());
        appInfo.setBkSupplierAccount("tencent");
        appInfo.setAppType(AppTypeEnum.APP_SET);
        appInfo.setMaintainers(request.getMaintainers());
        appInfo.setOperateDeptId(request.getDeptId());
        if (request.isDynamicAppSet()) {
            appInfo.setOperateDeptId(request.getDeptId());
        } else {
            List<Long> subAppList = new ArrayList<>();
            for (String subAppId : splitToList(request.getSubAppIds())) {
                subAppList.add(Long.parseLong(subAppId));
            }
            appInfo.setSubAppIds(subAppList);
        }

        if (StringUtils.isEmpty(request.getTimeZone())) {
            appInfo.setTimeZone("Asia/Shanghai");
        } else {
            appInfo.setTimeZone(request.getTimeZone());
        }
        appInfo.setLanguage("1");
        applicationService.createApp(appInfo);

        ServiceApplicationDTO newAppSet = convertToServiceApp(applicationDAO.getAppInfoById(request.getId()));
        return ServiceResponse.buildSuccessResp(newAppSet);
    }

    private void checkAddAppSetRequest(ServiceAddAppSetRequest request) {
        if (request.getId() == null || request.getId() > 9999999 || request.getId() < 8000000) {
            log.warn("Add app-set, appId is invalid");
            throw new ServiceException("AppId should not be empty, and should between 8000000 and 9999999");
        }
        if (StringUtils.isEmpty(request.getName())) {
            log.warn("Add app-set, appName is empty");
            throw new ServiceException("App name is empty");
        }
        checkAndGetMaintainers(request.getMaintainers());
        if (request.isDynamicAppSet()) {
            if (request.getDeptId() == null || request.getDeptId() < 1) {
                throw new ServiceException("Dept id is empty!");
            }
        } else {
            if (request.getSubAppIds() != null && !request.getSubAppIds().isEmpty()) {
                checkAndGetAppIds(request.getSubAppIds());
            }
        }
    }

    private List<String> checkAndGetMaintainers(String maintainers) {
        List<String> maintainerList = splitToList(maintainers);
        if (maintainerList.isEmpty()) {
            throw new ServiceException("Param maintainers is empty");
        }
        for (String maintainer : maintainerList) {
            if (!userDAO.isUserExist(maintainer)) {
                throw new ServiceException("maintainer:" + maintainer + " is not exist");
            }
        }
        return maintainerList;
    }

    @Override
    public ServiceResponse<ServiceApplicationDTO> addMaintainers(ServiceUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String maintainers = request.getAddMaintainers();
        log.info("Add app-set maintainers, appId:{}, maintainers:{}", appId, maintainers);
        if (appId == null) {
            return ServiceResponse.buildCommonFailResp("Param appId is empty");
        }
        if (StringUtils.isEmpty(maintainers)) {
            return ServiceResponse.buildCommonFailResp("Param maintainers is empty");
        }
        ApplicationInfoDTO app = checkGetAndAppSet(appId);
        List<String> newMaintainers = checkAndGetMaintainers(maintainers);

        List<String> currentMaintainers = splitToList(app.getMaintainers());
        log.info("App-set:{} current maintainers:{}", appId, currentMaintainers);

        newMaintainers.forEach(newMaintainer -> {
            if (!currentMaintainers.contains(newMaintainer)) {
                currentMaintainers.add(newMaintainer);
            }
        });

        log.info("Update app-set maintainers, appId:{}, maintainers:{}", appId, currentMaintainers);
        applicationDAO.updateMaintainers(appId, concatListAsString(currentMaintainers));

        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return ServiceResponse.buildSuccessResp(updatedApp);
    }

    private ApplicationInfoDTO checkGetAndAppSet(long appId) {
        ApplicationInfoDTO app = applicationDAO.getAppInfoById(appId);
        if (app == null) {
            log.warn("App is not exist, appId:{}", appId);
            throw new ServiceException("App-set is not exist");
        }
        if (!(app.getAppType() == AppTypeEnum.APP_SET || app.getAppType() == AppTypeEnum.ALL_APP)) {
            log.warn("App is not app-set or all-app, appId:{}", appId);
            throw new ServiceException("Not app-set or app-all type");
        }
        return app;
    }

    private List<String> splitToList(String itemsStr) {
        List<String> itemList = new ArrayList<>();
        if (StringUtils.isNotEmpty(itemsStr)) {
            itemList.addAll(Arrays.asList(itemsStr.split("[,;]")));
        }
        return itemList;
    }

    private String concatListAsString(List<String> itemList) {
        StringJoiner sj = new StringJoiner(";");
        itemList.forEach(sj::add);
        return sj.toString();
    }

    @Override
    public ServiceResponse<ServiceApplicationDTO> deleteMaintainers(ServiceUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String maintainers = request.getDelMaintainers();
        log.info("Delete maintainers, appId:{}, maintainers:{}", appId, maintainers);
        ApplicationInfoDTO app = checkGetAndAppSet(appId);

        List<String> currentMaintainers = splitToList(app.getMaintainers());
        log.info("App-set:{} , maintainers before deleting:{}", appId, currentMaintainers);
        List<String> deleteMaintainers = checkAndGetMaintainers(maintainers);
        currentMaintainers.removeAll(deleteMaintainers);
        log.info("App-set:{} , maintainers after deleting:{}", appId, currentMaintainers);

        applicationDAO.updateMaintainers(appId, concatListAsString(currentMaintainers));

        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return ServiceResponse.buildSuccessResp(updatedApp);
    }

    @Override
    public ServiceResponse<ServiceApplicationDTO> addSubAppToAppSet(ServiceUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String subAppIds = request.getAddSubAppIds();
        log.info("Add app-set subApp, appId:{}, subAppIds:{}", appId, subAppIds);
        if (appId == null) {
            return ServiceResponse.buildCommonFailResp("Param appId is empty");
        }
        if (StringUtils.isEmpty(subAppIds)) {
            return ServiceResponse.buildCommonFailResp("Param subAppIds is empty");
        }
        ApplicationInfoDTO app = checkGetAndAppSet(appId);
        List<String> newAppIds = checkAndGetAppIds(subAppIds);

        List<String> currentSubAppIds = app.getSubAppIds() == null ? new ArrayList<>() :
            app.getSubAppIds().stream().map(String::valueOf).collect(Collectors.toList());
        log.info("App-set:{} current subAppIds:{}", appId, currentSubAppIds);

        newAppIds.forEach(newAppId -> {
            if (!currentSubAppIds.contains(newAppId)) {
                currentSubAppIds.add(newAppId);
            }
        });

        log.info("Update app-set sub-app, appId:{}, subAppIds:{}", appId, currentSubAppIds);
        applicationDAO.updateSubAppIds(appId, concatListAsString(currentSubAppIds));
        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return ServiceResponse.buildSuccessResp(updatedApp);
    }

    private List<String> checkAndGetAppIds(String subAppIds) {
        List<String> appList = splitToList(subAppIds);
        if (appList.isEmpty()) {
            throw new ServiceException("Param subAppIds is empty");
        }
        for (String appId : appList) {
            if (applicationDAO.getAppInfoById(Long.parseLong(appId)) == null) {
                throw new ServiceException("Sub-app:" + appId + " is not exist");
            }
        }
        return appList;
    }

    @Override
    public ServiceResponse<ServiceApplicationDTO> deleteSubApp(ServiceUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String subAppIds = request.getDelSubAppIds();
        log.info("Delete subAppIds, appId:{}, subAppIds:{}", appId, subAppIds);
        ApplicationInfoDTO app = checkGetAndAppSet(appId);
        List<String> deleteSubAppIds = checkAndGetAppIds(subAppIds);

        List<String> currentSubAppIds = app.getSubAppIds() == null ? new ArrayList<>() :
            app.getSubAppIds().stream().map(Object::toString).collect(Collectors.toList());
        log.info("App-set:{} , subAppIds before deleting:{}", appId, currentSubAppIds);
        currentSubAppIds.removeAll(deleteSubAppIds);
        log.info("App-set:{} , subAppIds after deleting:{}", appId, currentSubAppIds);

        applicationDAO.updateSubAppIds(appId, concatListAsString(currentSubAppIds));
        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return ServiceResponse.buildSuccessResp(updatedApp);
    }
}
