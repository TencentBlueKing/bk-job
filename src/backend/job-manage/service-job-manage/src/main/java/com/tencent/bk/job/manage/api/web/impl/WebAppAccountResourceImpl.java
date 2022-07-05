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
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.metrics.RecordHttpStatus;
import com.tencent.bk.job.manage.api.web.WebAppAccountResource;
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.AccountVO;
import com.tencent.bk.job.manage.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@RestController
public class WebAppAccountResourceImpl implements WebAppAccountResource {
    private final AccountService accountService;
    private final MessageI18nService i18nService;
    private final AccountAuthService accountAuthService;
    private final JobManageConfig jobManageConfig;

    @Autowired
    public WebAppAccountResourceImpl(AccountService accountService,
                                     MessageI18nService i18nService,
                                     AccountAuthService accountAuthService,
                                     JobManageConfig jobManageConfig) {
        this.accountService = accountService;
        this.i18nService = i18nService;
        this.accountAuthService = accountAuthService;
        this.jobManageConfig = jobManageConfig;
    }

    @Override
    public Response<Long> saveAccount(String username,
                                      AppResourceScope appResourceScope,
                                      String scopeType,
                                      String scopeId,
                                      AccountCreateUpdateReq accountCreateUpdateReq) {
        AuthResult authResult = checkCreateAccountPermission(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        accountService.checkCreateParam(accountCreateUpdateReq, true, true);

        AccountDTO newAccount = accountService.buildCreateAccountDTO(username, appResourceScope.getAppId(),
            accountCreateUpdateReq);
        long accountId = accountService.saveAccount(newAccount);
        accountAuthService.registerAccount(
            username,
            accountId,
            newAccount.getAlias()
        );
        return Response.buildSuccessResp(accountId);
    }

    @Override
    public Response updateAccount(String username,
                                  AppResourceScope appResourceScope,
                                  String scopeType,
                                  String scopeId,
                                  AccountCreateUpdateReq accountCreateUpdateReq) {
        Long accountId = accountCreateUpdateReq.getId();
        AccountDTO account = accountService.getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }
        checkManageAccountPermission(username, appResourceScope, accountId);
        if (!checkUpdateAccountParam(accountCreateUpdateReq)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        AccountDTO updateAccount = buildUpdateAccountDTO(account, username, accountCreateUpdateReq);
        accountService.updateAccount(updateAccount);
        return Response.buildSuccessResp(null);
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
    public Response<PageData<AccountVO>> listAppAccounts(String username,
                                                         AppResourceScope appResourceScope,
                                                         String scopeType,
                                                         String scopeId,
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
                                                         String keyword) {
        PageData<AccountDTO> pageData;
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        baseSearchCondition.setOrderField(orderField);
        baseSearchCondition.setOrder(order);
        if (keyword == null) {
            // 按字段搜索
            AccountDTO accountQuery = new AccountDTO();
            accountQuery.setAppId(appResourceScope.getAppId());
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
            pageData = accountService.searchPageAccount(appResourceScope.getAppId(), keyword, baseSearchCondition);
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
        List<Long> canManageIdList =
            accountAuthService.batchAuthManageAccount(username, appResourceScope,
                accountVOS.parallelStream().map(AccountVO::getId).collect(Collectors.toList()));
        accountVOS.forEach(it -> it.setCanManage(canManageIdList.contains(it.getId())));
        result.setData(accountVOS);
        result.setCanCreate(checkCreateAccountPermission(username, appResourceScope).isPass());
        return Response.buildSuccessResp(result);
    }

    private AccountVO convertToAccountVO(AccountDTO accountDTO) {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(accountDTO.getId());

        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(accountDTO.getAppId());
        accountVO.setScopeType(resourceScope.getType().getValue());
        accountVO.setScopeId(resourceScope.getId());
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
    public Response deleteAccount(String username,
                                  AppResourceScope appResourceScope,
                                  String scopeType,
                                  String scopeId,
                                  Long accountId) {

        log.info("Delete account, operator={}, scope={}, accountId={}", username, appResourceScope, accountId);
        AccountDTO account = accountService.getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }
        checkManageAccountPermission(username, appResourceScope, accountId);
        if (accountService.isAccountRefByAnyStep(accountId)) {
            log.info("Account:{} is ref by step, should not delete!", accountId);
            throw new FailedPreconditionException(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN);
        }
        if (account.getCategory() == AccountCategoryEnum.SYSTEM
            && accountService.isSystemAccountRefByDbAccount(accountId)) {
            log.info("Account:{} is ref by db account, should not delete!", accountId);
            throw new FailedPreconditionException(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN);
        }
        accountService.deleteAccount(accountId);
        return Response.buildSuccessResp(null);
    }

    @Override
    public Response<AccountVO> getAccountById(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Long accountId) {
        AccountDTO account = accountService.getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }

        checkManageAccountPermission(username, appResourceScope, accountId);

        return Response.buildSuccessResp(convertToAccountVO(account));
    }

    @Override
    @RecordHttpStatus(value = CommonMetricNames.WEB_API, extraTags = {"api_name", "list_accounts"})
    public Response<List<AccountVO>> listAccounts(String username,
                                                  AppResourceScope appResourceScope,
                                                  String scopeType,
                                                  String scopeId,
                                                  Integer category) {
        if (category != null && AccountCategoryEnum.valOf(category) == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        List<AccountDTO> accountDTOS =
            accountService.listAllAppAccount(appResourceScope.getAppId(), AccountCategoryEnum.valOf(category));
        List<AccountVO> accountVOS = new ArrayList<>();
        if (accountDTOS != null && !accountDTOS.isEmpty()) {
            List<Long> accountIdList = new ArrayList<>();
            for (AccountDTO accountDTO : accountDTOS) {
                AccountVO accountVO = convertToAccountVO(accountDTO);
                accountVO.setPassword("******");
                accountVO.setDbPassword("******");
                accountVO.setCanManage(true);
                accountVO.setCanUse(true);
                accountIdList.add(accountDTO.getId());
                accountVOS.add(accountVO);
            }
            // 批量鉴权
            List<Long> canManageIdList = accountAuthService.batchAuthManageAccount(username,
                appResourceScope, accountIdList);
            Set<Long> canManageIdSet = new HashSet<>(canManageIdList);
            accountVOS.forEach(accountVO ->
                accountVO.setCanManage(canManageIdSet.contains(accountVO.getId())));

            setUseAccountPermission(username, appResourceScope, accountVOS);
        }
        return Response.buildSuccessResp(accountVOS);
    }

    private AuthResult checkCreateAccountPermission(String username, AppResourceScope appResourceScope) {
        // 需要拥有在业务下创建账号的权限
        return accountAuthService.authCreateAccount(username, appResourceScope);
    }

    private void checkManageAccountPermission(String username,
                                              AppResourceScope appResourceScope,
                                              Long accountId) {
        AuthResult authResult = accountAuthService.authManageAccount(username, appResourceScope, accountId, null);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private void setUseAccountPermission(String username,
                                         AppResourceScope appResourceScope,
                                         List<AccountVO> accountVOS) {
        if (CollectionUtils.isEmpty(accountVOS)) {
            return;
        }
        if (shouldAuthAccount(appResourceScope.getAppId())) {
            List<Long> accountIdList = accountVOS.stream().map(AccountVO::getId).collect(Collectors.toList());
            List<Long> allowedIdList = accountAuthService.batchAuthUseAccount(username, appResourceScope,
                accountIdList);
            Set<Long> allowedIdSet = new HashSet<>(allowedIdList);
            accountVOS.forEach(accountVO ->
                accountVO.setCanUse(allowedIdSet.contains(accountVO.getId())));
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
