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

import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.manage.api.inner.ServiceAppSetResource;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.tmp.TmpAddAppSetRequest;
import com.tencent.bk.job.manage.model.tmp.TmpUpdateAppSetRequest;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MAX;
import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MIN;

@Slf4j
@RestController
@DeprecatedAppLogic
public class ServiceAppSetResourceImpl implements ServiceAppSetResource {

    private final ApplicationDAO applicationDAO;
    private final ApplicationService applicationService;
    private final EsbUserInfoDAO userDAO;
    private final DSLContext dslContext;

    @Autowired
    public ServiceAppSetResourceImpl(ApplicationDAO applicationDAO,
                                     ApplicationService applicationService,
                                     EsbUserInfoDAO userDAO,
                                     @Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.applicationDAO = applicationDAO;
        this.applicationService = applicationService;
        this.userDAO = userDAO;
        this.dslContext = dslContext;
    }


    @Override
    public InternalResponse<ServiceApplicationDTO> queryAppSetById(Long appId) {
        ServiceApplicationDTO appInfo = getAppSet(appId);
        if (appInfo == null) {
            throw new NotFoundException(ErrorCode.WRONG_APP_ID);
        }
        return InternalResponse.buildSuccessResp(appInfo);
    }

    @Override
    public InternalResponse<List<ServiceApplicationDTO>> listAppSet() {
        List<ApplicationDTO> appSetList = applicationDAO.listAppsByType(AppTypeEnum.APP_SET);
        List<ApplicationDTO> allAppSetList = applicationDAO.listAppsByType(AppTypeEnum.ALL_APP);

        List<ApplicationDTO> results = new ArrayList<>();
        if (appSetList != null && !appSetList.isEmpty()) {
            results.addAll(appSetList);
        }
        if (allAppSetList != null && !allAppSetList.isEmpty()) {
            results.addAll(allAppSetList);
        }
        if (!results.isEmpty()) {
            results = results.stream().sorted((Comparator.comparing(ApplicationDTO::getId)))
                .collect(Collectors.toList());
        }
        return InternalResponse.buildSuccessResp(results.stream().map(ServiceApplicationDTO::fromApplicationDTO)
            .collect(Collectors.toList()));
    }

    @Override
    public InternalResponse<Boolean> deleteAppSet(Long appId) {
        checkAppSetId(appId);
        ApplicationDTO app = applicationDAO.getAppById(appId);
        if (app == null) {
            log.warn("App-set is not exist!");
            throw new NotFoundException(ErrorCode.WRONG_APP_ID);
        }
        applicationDAO.deleteAppByIdSoftly(dslContext, appId);
        return InternalResponse.buildSuccessResp(null);
    }

    private void checkAppSetId(Long appId) {
        // appSet id should between 8000000 and 9999999
        if (appId == null || appId < JOB_BUILD_IN_BIZ_SET_ID_MIN || appId > JOB_BUILD_IN_BIZ_SET_ID_MAX) {
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
    }

    private ServiceApplicationDTO getAppSet(Long appId) {
        ApplicationDTO appInfo = applicationDAO.getAppById(appId);
        if (appInfo == null) {
            return null;
        }
        return ServiceApplicationDTO.fromApplicationDTO(appInfo);
    }

    @Override
    public InternalResponse<ServiceApplicationDTO> addAppSet(TmpAddAppSetRequest request) {
        log.info("Add app-set, request:{}", request);
        checkAddAppSetRequest(request);

        ApplicationDTO app = applicationDAO.getAppById(request.getId());
        if (app != null) {
            log.warn("App-set is exist!");
            throw new NotFoundException(ErrorCode.WRONG_APP_ID);
        }

        ApplicationDTO appInfo = new ApplicationDTO();
        appInfo.setId(request.getId());
        // 使用appId作为scopeId，等cmdb业务集上线之后需要废除这个API
        appInfo.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, String.valueOf(request.getId())));
        appInfo.setName(request.getName());
        appInfo.setBkSupplierAccount("tencent");
        appInfo.setAppType(AppTypeEnum.APP_SET);
        appInfo.setMaintainers(request.getMaintainers());
        if (request.isDynamicAppSet()) {
            appInfo.setOperateDeptId(request.getDeptId());
        } else {
            List<Long> subAppList = new ArrayList<>();
            for (String subAppId : splitToList(request.getSubAppIds())) {
                subAppList.add(Long.parseLong(subAppId));
            }
            appInfo.setSubBizIds(subAppList);
        }

        if (StringUtils.isEmpty(request.getTimeZone())) {
            appInfo.setTimeZone("Asia/Shanghai");
        } else {
            appInfo.setTimeZone(request.getTimeZone());
        }
        appInfo.setLanguage("1");
        applicationService.createAppWithSpecifiedAppId(appInfo);

        ServiceApplicationDTO newAppSet = ServiceApplicationDTO.fromApplicationDTO(
            applicationDAO.getAppById(request.getId())
        );
        return InternalResponse.buildSuccessResp(newAppSet);
    }

    private void checkAddAppSetRequest(TmpAddAppSetRequest request) {
        if (request.getId() == null || request.getId() > JOB_BUILD_IN_BIZ_SET_ID_MAX
            || request.getId() < JOB_BUILD_IN_BIZ_SET_ID_MIN) {
            log.warn("Add app-set, appId is invalid");
            throw new InvalidParamException(ErrorCode.WRONG_APP_ID);
        }
        if (StringUtils.isEmpty(request.getName())) {
            log.warn("Add app-set, appName is empty");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("App name is empty"));
        }
        checkAndGetMaintainers(request.getMaintainers());
        if (request.isDynamicAppSet()) {
            if (request.getDeptId() == null || request.getDeptId() < 1) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                    ArrayUtil.toArray("Dept id is empty!"));
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
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        for (String maintainer : maintainerList) {
            if (!userDAO.isUserExist(maintainer)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                    ArrayUtil.toArray("maintainer:" + maintainer + " is not exist"));
            }
        }
        return maintainerList;
    }

    @Override
    public InternalResponse<ServiceApplicationDTO> addMaintainers(TmpUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String maintainers = request.getAddMaintainers();
        log.info("Add app-set maintainers, appId:{}, maintainers:{}", appId, maintainers);
        if (appId == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("Param appId is empty"));
        }
        if (StringUtils.isEmpty(maintainers)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("Param maintainers is empty"));
        }
        ApplicationDTO app = checkGetAndAppSet(appId);
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
        return InternalResponse.buildSuccessResp(updatedApp);
    }

    private ApplicationDTO checkGetAndAppSet(long appId) {
        ApplicationDTO app = applicationDAO.getAppById(appId);
        if (app == null) {
            log.warn("App is not exist, appId:{}", appId);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("App-set is not exist"));
        }
        if (!(app.getAppType() == AppTypeEnum.APP_SET || app.getAppType() == AppTypeEnum.ALL_APP)) {
            log.warn("App is not app-set or all-app, appId:{}", appId);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("Not app-set or app-all type"));
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
    public InternalResponse<ServiceApplicationDTO> deleteMaintainers(TmpUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String maintainers = request.getDelMaintainers();
        log.info("Delete maintainers, appId:{}, maintainers:{}", appId, maintainers);
        ApplicationDTO app = checkGetAndAppSet(appId);

        List<String> currentMaintainers = splitToList(app.getMaintainers());
        log.info("App-set:{} , maintainers before deleting:{}", appId, currentMaintainers);
        List<String> deleteMaintainers = checkAndGetMaintainers(maintainers);
        currentMaintainers.removeAll(deleteMaintainers);
        log.info("App-set:{} , maintainers after deleting:{}", appId, currentMaintainers);

        applicationDAO.updateMaintainers(appId, concatListAsString(currentMaintainers));

        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return InternalResponse.buildSuccessResp(updatedApp);
    }

    @Override
    public InternalResponse<ServiceApplicationDTO> addSubAppToAppSet(TmpUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String subAppIds = request.getAddSubBizIds();
        log.info("Add app-set subApp, appId:{}, subAppIds:{}", appId, subAppIds);
        if (appId == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("Param appId is empty"));
        }
        if (StringUtils.isEmpty(subAppIds)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                ArrayUtil.toArray("Param subAppIds is empty"));
        }
        ApplicationDTO app = checkGetAndAppSet(appId);
        List<String> newAppIds = checkAndGetAppIds(subAppIds);

        List<String> currentSubBizIds = app.getSubBizIds() == null ? new ArrayList<>() :
            app.getSubBizIds().stream().map(String::valueOf).collect(Collectors.toList());
        log.info("App-set:{} current subBizIds:{}", appId, currentSubBizIds);

        newAppIds.forEach(newAppId -> {
            if (!currentSubBizIds.contains(newAppId)) {
                currentSubBizIds.add(newAppId);
            }
        });

        log.info("Update app-set sub-app, appId:{}, subAppIds:{}", appId, currentSubBizIds);
        applicationDAO.updateSubBizIds(appId, concatListAsString(currentSubBizIds));
        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return InternalResponse.buildSuccessResp(updatedApp);
    }

    private List<String> checkAndGetAppIds(String subAppIds) {
        List<String> appList = splitToList(subAppIds);
        if (appList.isEmpty()) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        for (String appId : appList) {
            if (applicationDAO.getAppById(Long.parseLong(appId)) == null) {
                throw new NotFoundException(ErrorCode.WRONG_APP_ID);
            }
        }
        return appList;
    }

    @Override
    public InternalResponse<ServiceApplicationDTO> deleteSubApp(TmpUpdateAppSetRequest request) {
        Long appId = request.getAppId();
        String subAppIds = request.getDelSubBizIds();
        log.info("Delete subBizIds, appId:{}, subAppIds:{}", appId, subAppIds);
        ApplicationDTO app = checkGetAndAppSet(appId);
        List<String> deleteSubAppIds = checkAndGetAppIds(subAppIds);

        List<String> currentSubBizIds = app.getSubBizIds() == null ? new ArrayList<>() :
            app.getSubBizIds().stream().map(Object::toString).collect(Collectors.toList());
        log.info("App-set:{} , subBizIds before deleting:{}", appId, currentSubBizIds);
        currentSubBizIds.removeAll(deleteSubAppIds);
        log.info("App-set:{} , subBizIds after deleting:{}", appId, currentSubBizIds);

        applicationDAO.updateSubBizIds(appId, concatListAsString(currentSubBizIds));
        ServiceApplicationDTO updatedApp = getAppSet(appId);
        return InternalResponse.buildSuccessResp(updatedApp);
    }
}
