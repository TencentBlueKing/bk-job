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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.FeatureToggleModeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.web.WebAppAccountResource;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.AccountVO;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 8/11/2019 15:41
 */
@Slf4j
@RestController
public class WebAppAccountResourceImpl implements WebAppAccountResource {
    private AccountService accountService;
    private MessageI18nService i18nService;
    private WebAuthService authService;
    private ApplicationService applicationService;
    private JobManageConfig jobManageConfig;

    @Autowired
    public WebAppAccountResourceImpl(AccountService accountService, MessageI18nService i18nService,
                                     WebAuthService webAuthService, ApplicationService applicationService,
                                     JobManageConfig jobManageConfig) {
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.authService = webAuthService;
        this.applicationService = applicationService;
        this.jobManageConfig = jobManageConfig;
    }

    @Override
    public ServiceResponse<Long> saveAccount(String username, Long appId,
                                             AccountCreateUpdateReq accountCreateUpdateReq) {
        ApplicationInfoDTO applicationInfoDTO = applicationService.getAppInfoById(appId);
        if (applicationInfoDTO == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.WRONG_APP_ID);
        }
        AuthResultVO authResultVO = checkCreateAccountPermission(username, appId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        JobContextUtil.setAppId(appId);
        try {
            accountService.checkCreateParam(accountCreateUpdateReq, true, true);
        } catch (InvalidParamException e) {
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        }
        AccountDTO newAccount = accountService.buildCreateAccountDTO(username, appId, accountCreateUpdateReq);
        try {
            long accountId = accountService.saveAccount(newAccount);
            authService.registerResource(
                "" + accountId,
                newAccount.getAlias(),
                ResourceId.ACCOUNT,
                username,
                null
            );
            return ServiceResponse.buildSuccessResp(accountId);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to save account, appId={}, account={}, reason={}", appId, accountCreateUpdateReq, errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    @Override
    public ServiceResponse updateAccount(String username, Long appId, AccountCreateUpdateReq accountCreateUpdateReq) {
        Long accountId = accountCreateUpdateReq.getId();
        AccountDTO account = accountService.getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            return ServiceResponse.buildCommonFailResp(
                i18nService.getI18nWithArgs(String.valueOf(ErrorCode.ACCOUNT_NOT_EXIST), accountId)
            );
        }
        AuthResultVO authResultVO = checkManageAccountPermission(username, appId, accountId);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        if (!checkUpdateAccountParam(accountCreateUpdateReq)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        AccountDTO updateAccount = buildUpdateAccountDTO(account, username, accountCreateUpdateReq);
        try {
            accountService.updateAccount(updateAccount);
            return ServiceResponse.buildSuccessResp(null);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to update account, appId={}, account={}, reason={}", appId, accountCreateUpdateReq,
                errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    private boolean checkUpdateAccountParam(AccountCreateUpdateReq req) {
        // 账号名称是不能更新的，所以这里不用校验
        if (req.getId() == null) {
            log.warn("Id is invalid, id={}", req.getId());
            return false;
        }
        if (req.getCategory() != null && req.getCategory().equals(AccountCategoryEnum.DB.getValue())
            && (req.getDbPort() == null || req.getDbSystemAccountId() == null)) {
            log.warn("Db port or dbSystemAccountId is empty, dbPort={}, dbSystemAccountId={}", req.getDbPort(),
                req.getDbSystemAccountId());
            return false;
        }
        return true;
    }

    private AccountDTO buildUpdateAccountDTO(AccountDTO existAccount, String operator, AccountCreateUpdateReq req) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setAppId(existAccount.getAppId());
        accountDTO.setType(existAccount.getType());
        accountDTO.setCategory(existAccount.getCategory());
        accountDTO.setAlias(req.getAlias());
        accountDTO.setRemark(req.getRemark());
        accountDTO.setGrantees(Utils.concatStringWithSeperator(req.getGrantees(), ","));
        if (StringUtils.isNotEmpty(req.getPassword()) && !req.getPassword().equals("******")) {
            // 前端所有的返回密码都是"******"，如果更新接口传给后台的仍然是******，说明密码未改动
            accountDTO.setPassword(req.getPassword());
        }
        if (StringUtils.isNotEmpty(req.getDbPassword()) && !req.getDbPassword().equals("******")) {
            // 前端所有的返回密码都是"******"，如果更新接口传给后台的仍然是******，说明密码未改动
            accountDTO.setDbPassword(req.getDbPassword());
        }
        accountDTO.setDbPort(req.getDbPort());
        accountDTO.setDbSystemAccountId(req.getDbSystemAccountId());
        accountDTO.setLastModifyUser(operator);
        accountDTO.setLastModifyTime(DateUtils.currentTimeMillis());
        accountDTO.setId(req.getId());

        return accountDTO;
    }

    @Override
    public ServiceResponse<PageData<AccountVO>> listAppAccounts(
        String username,
        Long appId,
        Long id,
        String name,
        String alias,
        Integer category,
        Integer type,
        String creator,
        String lastModifyUser,
        Integer start,
        Integer pageSize,
        String orderField,
        Integer order,
        String keyword
    ) {
        ApplicationInfoDTO applicationInfoDTO = applicationService.getAppInfoById(appId);
        if (applicationInfoDTO == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.WRONG_APP_ID);
        }
        JobContextUtil.setAppId(appId);
        PageData<AccountDTO> pageData;
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        baseSearchCondition.setOrderField(orderField);
        baseSearchCondition.setOrder(order);
        if (keyword == null) {
            // 按字段搜索
            AccountDTO accountQuery = new AccountDTO();
            accountQuery.setAppId(appId);
            if (id != null) {
                accountQuery.setId(id);
            } else {
                accountQuery.setAccount(name);
                accountQuery.setAlias(alias);
                accountQuery.setCategory(AccountCategoryEnum.valOf(category));
                accountQuery.setType(AccountTypeEnum.valueOf(type));
                accountQuery.setCreator(creator);
                accountQuery.setLastModifyUser(lastModifyUser);
            }
            pageData = accountService.listPageAccount(accountQuery, baseSearchCondition);
        } else {
            // 模糊搜索
            pageData = accountService.searchPageAccount(appId, keyword, baseSearchCondition);
        }
        PageData<AccountVO> result = new PageData<>();
        result.setTotal(pageData.getTotal());
        result.setPageSize(pageData.getPageSize());
        result.setStart(pageData.getStart());

        List<AccountVO> accountVOS = new ArrayList<>();
        if (pageData.getData() != null) {
            for (AccountDTO accountDTO : pageData.getData()) {
                AccountVO accountVO = convertToAccountVO(accountDTO);
                accountVOS.add(accountVO);
            }
        }
        // 添加权限数据
        List<String> canManageIdList =
            authService.batchAuth(username, ActionId.MANAGE_ACCOUNT, appId, ResourceTypeEnum.ACCOUNT,
                accountVOS.parallelStream().map(AccountVO::getId).map(Objects::toString).collect(Collectors.toList()));
        accountVOS.forEach(it -> {
            it.setCanManage(canManageIdList.contains(it.getId().toString()));
        });
        result.setData(accountVOS);
        result.setCanCreate(checkCreateAccountPermission(username, appId).isPass());
        return ServiceResponse.buildSuccessResp(result);
    }

    private AccountVO convertToAccountVO(AccountDTO accountDTO) {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(accountDTO.getId());
        accountVO.setAppId(accountDTO.getAppId());
        accountVO.setAccount(accountDTO.getAccount());
        accountVO.setAlias(accountDTO.getAlias());
        accountVO.setCategory(accountDTO.getCategory().getValue());
        accountVO.setCategoryName(i18nService.getI18n(accountDTO.getCategory().getI18nKey()));
        accountVO.setType(accountDTO.getType().getType());
        accountVO.setTypeName(accountDTO.getType().getName());
        accountVO.setOwnerUsers(Utils.getNotBlankSplitList(accountDTO.getGrantees(), ","));
        accountVO.setRemark(accountDTO.getRemark());
        accountVO.setOs(accountDTO.getOs());
        accountVO.setPassword("******");
        accountVO.setDbPort(accountDTO.getDbPort());
        accountVO.setDbPassword("******");
        accountVO.setDbSystemAccountId(accountDTO.getDbSystemAccountId());
        accountVO.setLastModifyUser(accountDTO.getLastModifyUser());
        accountVO.setCreator(accountDTO.getCreator());
        if (accountDTO.getCreateTime() != null) {
            accountVO.setCreateTime(accountDTO.getCreateTime());
        }
        if (accountDTO.getLastModifyTime() != null) {
            accountVO.setLastModifyTime(accountDTO.getLastModifyTime());
        }
        return accountVO;
    }

    @Override
    public ServiceResponse deleteAccount(String username, Long appId, Long accountId) {
        JobContextUtil.setAppId(appId);
        log.info("Delete account, operator={}, appId={}, accountId={}", username, appId, accountId);
        try {
            AccountDTO account = accountService.getAccountById(accountId);
            if (account == null) {
                log.info("Account is not exist, accountId={}", accountId);
                return ServiceResponse.buildCommonFailResp(
                    i18nService.getI18nWithArgs(String.valueOf(ErrorCode.ACCOUNT_NOT_EXIST), accountId));
            }
            AuthResultVO authResultVO = checkManageAccountPermission(username, appId, accountId);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResultVO);
            }
            if (accountService.isAccountRefByAnyStep(accountId)) {
                log.info("Account:{} is ref by step, should not delete!", accountId);
                return ServiceResponse.buildCommonFailResp(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN, i18nService);
            }
            if (account.getCategory() == AccountCategoryEnum.SYSTEM
                && accountService.isSystemAccountRefByDbAccount(accountId)) {
                log.info("Account:{} is ref by db account, should not delete!", accountId);
                return ServiceResponse.buildCommonFailResp(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN, i18nService);
            }
            accountService.deleteAccount(accountId);
            return ServiceResponse.buildSuccessResp(null);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to delete account, appId={}, accountId={}, reason={}", appId, accountId, errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    @Override
    public ServiceResponse<AccountVO> getAccountById(String username, Long appId, Long accountId) {
        JobContextUtil.setAppId(appId);
        try {
            AccountDTO accountDTO = accountService.getAccountById(accountId);
            if (accountDTO == null) {
                return ServiceResponse.buildSuccessResp(null);
            }
            return ServiceResponse.buildSuccessResp(convertToAccountVO(accountDTO));
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to get account by id, accountId={}, reason={}", accountId, errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    @Override
    public ServiceResponse<List<AccountVO>> listAccounts(String username, Long appId, Integer category) {
        JobContextUtil.setAppId(appId);
        if (category != null && AccountCategoryEnum.valOf(category) == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        try {
            List<AccountDTO> accountDTOS =
                accountService.listAllAppAccount(appId, AccountCategoryEnum.valOf(category));
            List<AccountVO> accountVOS = new ArrayList<>();
            if (accountDTOS != null && !accountDTOS.isEmpty()) {
                List<Long> accountIdList = new ArrayList<>();
                for (int i = 0; i < accountDTOS.size(); i++) {
                    AccountDTO accountDTO = accountDTOS.get(i);
                    AccountVO accountVO = convertToAccountVO(accountDTO);
                    accountVO.setPassword("******");
                    accountVO.setDbPassword("******");
                    accountVO.setCanManage(true);
                    accountVO.setCanUse(true);
                    accountIdList.add(accountDTO.getId());
                    accountVOS.add(accountVO);
                }
                // 批量鉴权
                Set<String> allowedManageAccounts = new HashSet<>(authService
                    .batchAuth(username, ActionId.MANAGE_ACCOUNT, appId, ResourceTypeEnum.ACCOUNT,
                        accountIdList.parallelStream().map(Object::toString).collect(Collectors.toList())));
                accountVOS.forEach(accountVO ->
                    accountVO.setCanManage(allowedManageAccounts.contains(accountVO.getId().toString())));

                setUseAccountPermission(username, appId, accountVOS);
            }
            return ServiceResponse.buildSuccessResp(accountVOS);
        } catch (ServiceException e) {
            String errorMsg = i18nService.getI18n(String.valueOf(e.getErrorCode()));
            log.warn("Fail to get accounts, appId={}, category={}, , reason={}", appId, category, errorMsg);
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), errorMsg);
        }
    }

    private AuthResultVO checkCreateAccountPermission(String username, Long appId) {
        // 需要拥有在业务下创建账号的权限
        return authService.auth(
            true,
            username,
            ActionId.CREATE_ACCOUNT,
            ResourceTypeEnum.BUSINESS,
            appId.toString(),
            null
        );
    }

    private AuthResultVO checkManageAccountPermission(String username, Long appId, Long accountId) {
        // 需要拥有在业务下管理某个具体账号的权限
        return authService.auth(true, username, ActionId.MANAGE_ACCOUNT, ResourceTypeEnum.ACCOUNT,
            accountId.toString(), PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }

    private void setUseAccountPermission(String username, Long appId, List<AccountVO> accountVOS) {
        if (CollectionUtils.isEmpty(accountVOS)) {
            return;
        }
        if (shouldAuthAccount(appId)) {
            List<Long> accountIdList = accountVOS.stream().map(AccountVO::getId).collect(Collectors.toList());
            Set<String> allowedUseAccounts = new HashSet<>(authService
                .batchAuth(username, ActionId.USE_ACCOUNT, appId, ResourceTypeEnum.ACCOUNT,
                    accountIdList.parallelStream().map(Object::toString).collect(Collectors.toList())));
            accountVOS.forEach(accountVO ->
                accountVO.setCanUse(allowedUseAccounts.contains(accountVO.getId().toString())));
        } else {
            accountVOS.forEach(accountVO -> accountVO.setCanUse(true));
        }
    }

    private boolean shouldAuthAccount(Long appId) {
        String authAccountEnableMode = jobManageConfig.getEnableAuthAccountMode();
        if (FeatureToggleModeEnum.ENABLED.getMode().equals(authAccountEnableMode)) {
            return true;
        } else if (FeatureToggleModeEnum.DISABLED.getMode().equals(authAccountEnableMode)) {
            return false;
        } else if (FeatureToggleModeEnum.GRAY.getMode().equals(authAccountEnableMode)) {
            // 如果配置了灰度业务，仅针对灰度业务启用账号鉴权
            if (StringUtils.isNotBlank(jobManageConfig.getAccountAuthGrayApps())) {
                try {
                    String[] grayApps = jobManageConfig.getAccountAuthGrayApps().split(",");
                    if (grayApps.length == 0) {
                        // 如果没有配置灰度业务ID,那么账号鉴权功能对所有业务关闭
                        return false;
                    }
                    Set<Long> grayAppIds = new HashSet<>();
                    for (String app : grayApps) {
                        grayAppIds.add(Long.valueOf(app.trim()));
                    }
                    return grayAppIds.contains(appId);
                } catch (Throwable e) {
                    // 如果配置灰度业务ID错误,那么账号鉴权功能对所有业务关闭
                    log.error("Parse account auth gray app fail!", e);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
