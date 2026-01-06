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
import com.tencent.bk.audit.annotations.AuditInstanceRecord;
import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.crypto.Encryptor;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.common.constants.OSTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.job.manage.dao.AccountDAO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.AccountDisplayDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRule;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.globalsetting.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
    public final String DEFAULT_LINUX_ACCOUNT = "root";
    public final String DEFAULT_WINDOWS_SYSTEM_ACCOUNT = "system";
    public final String DEFAULT_WINDOWS_ADMIN_ACCOUNT = "Administrator";
    private final AccountDAO accountDAO;
    private final Encryptor encryptor;
    private final GlobalSettingsService globalSettingsService;
    private final AccountAuthService accountAuthService;

    @Autowired
    public AccountServiceImpl(AccountDAO accountDAO,
                              @Qualifier("gseRsaEncryptor") Encryptor encryptor,
                              GlobalSettingsService globalSettingsService,
                              AccountAuthService accountAuthService) {
        this.accountDAO = accountDAO;
        this.encryptor = encryptor;
        this.globalSettingsService = globalSettingsService;
        this.accountAuthService = accountAuthService;
    }

    @Override
    public AccountDTO createAccount(AccountDTO account) throws ServiceException {
        log.info("Save account, account={}", account);
        AccountDTO existAccount = accountDAO.getAccount(account.getAppId(), account.getCategory(), account.getAlias());
        if (existAccount != null) {
            log.info("Account is exist, appId={}, category={}, alias={}", account.getAppId(), account.getCategory(),
                account.getAlias());
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_ALIAS_EXIST);
        }
        if (StringUtils.isNotEmpty(account.getPassword())) {
            account.setPassword(encryptor.encrypt(account.getPassword()));
        }

        if (account.getCategory() == AccountCategoryEnum.DB) {
            AccountDTO dbSystemAccount = accountDAO.getAccountById(account.getDbSystemAccountId());
            if (dbSystemAccount == null) {
                log.info("DB related system account is not exist, systemAccountId={}", account.getDbSystemAccountId());
                throw new NotFoundException(ErrorCode.DB_SYSTEM_ACCOUNT_IS_INVALID);
            }
            if (!dbSystemAccount.getAppId().equals(account.getAppId())) {
                log.warn("DB related system account is not in current app, systemAccountId={}, " +
                        "systemAccountAppId={}"
                    , account.getDbSystemAccountId(), dbSystemAccount.getAppId());
                throw new NotFoundException(ErrorCode.DB_SYSTEM_ACCOUNT_IS_INVALID);
            }
        }

        long accountId;
        if (account.getId() == null) {
            accountId = accountDAO.saveAccount(account);
        } else {
            accountId = accountDAO.saveAccountWithId(account);
        }

        return getAccountById(accountId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.CREATE_ACCOUNT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.ACCOUNT,
            instanceIds = "#$?.id",
            instanceNames = "#$?.alias"
        ),
        content = EventContentConstants.CREATE_ACCOUNT
    )
    public AccountDTO createAccount(User user, AccountDTO account) {
        authCreateAccount(user, account.getAppId());
        AccountDTO createdAccount = createAccount(account);
        accountAuthService.registerAccount(
            user,
            createdAccount.getId(),
            createdAccount.getAlias()
        );
        return createdAccount;
    }

    private void authCreateAccount(User user, long appId) throws PermissionDeniedException {
        accountAuthService.authCreateAccount(user, new AppResourceScope(appId)).denyIfNoPermission();
    }

    private void authUseAccount(User user, long appId, long accountId) throws PermissionDeniedException {
        accountAuthService.authUseAccount(user, new AppResourceScope(appId), accountId, null)
            .denyIfNoPermission();
    }

    private void authManageAccount(User user, long appId, long accountId) throws PermissionDeniedException {
        accountAuthService.authManageAccount(user, new AppResourceScope(appId), accountId, null)
            .denyIfNoPermission();
    }

    @Override
    public AccountDTO getAccountById(Long accountId) throws ServiceException {
        return accountDAO.getAccountById(accountId);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.USE_ACCOUNT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.ACCOUNT,
            instanceIds = "#accountId",
            instanceNames = "#$?.alias"
        ),
        content = EventContentConstants.USE_ACCOUNT
    )
    public AccountDTO getAccount(User user, long appId, Long accountId) {
        authUseAccount(user, appId, accountId);
        return getAccount(appId, accountId);
    }

    @Override
    public AccountDTO getAccount(long appId, Long accountId) {
        AccountDTO account = getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }
        if (!account.getAppId().equals(appId)) {
            log.info("Account is not in app, appId={}, accountId={}", appId, accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountId));
        }
        return account;
    }

    @Override
    public Map<Long, AccountDisplayDTO> getAccountDisplayInfoMapByIds(
        Collection<Long> accountIds) throws ServiceException {
        Map<Long, AccountDisplayDTO> map = new HashMap<>();
        List<AccountDisplayDTO> accountDisplayDTOList = accountDAO.listAccountDisplayInfoByIds(accountIds);
        for (AccountDisplayDTO accountDisplayDTO : accountDisplayDTOList) {
            map.put(accountDisplayDTO.getId(), accountDisplayDTO);
        }
        return map;
    }

    @Override
    public AccountDTO getAccountByAccount(Long appId, String account) throws ServiceException {
        return accountDAO.getAccountByAccount(appId, account);
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_ACCOUNT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.ACCOUNT,
            instanceIds = "#updateAccount?.id",
            instanceNames = "#updateAccount?.alias"
        ),
        content = EventContentConstants.EDIT_ACCOUNT
    )
    public AccountDTO updateAccount(User user, AccountDTO updateAccount) throws ServiceException {
        authManageAccount(user, updateAccount.getAppId(), updateAccount.getId());

        AccountDTO originAccount = getAccount(updateAccount.getAppId(), updateAccount.getId());

        checkAccountAliasExist(updateAccount.getAppId(), updateAccount.getId(),
            originAccount.getCategory(), updateAccount.getAlias());

        if (StringUtils.isNotEmpty(updateAccount.getPassword())) {
            updateAccount.setPassword(encryptor.encrypt(updateAccount.getPassword()));
        }
        if (updateAccount.getCategory() == AccountCategoryEnum.DB
            && StringUtils.isNotEmpty(updateAccount.getDbPassword())) {
            updateAccount.setDbPassword(encryptor.encrypt(updateAccount.getPassword()));
        }
        // 账号用途、账号类型、账号名称不允许修改
        updateAccount.setCategory(originAccount.getCategory());
        updateAccount.setType(originAccount.getType());
        updateAccount.setAccount(originAccount.getAccount());

        log.info("Update account, account={}", updateAccount);
        accountDAO.updateAccount(updateAccount);
        AccountDTO updatedAccount = getAccountById(updateAccount.getId());

        // 审计
        ActionAuditContext.current()
            .setOriginInstance(originAccount.toEsbAccountV3DTO())
            .setInstance(updatedAccount.toEsbAccountV3DTO());

        return updatedAccount;
    }

    private void checkAccountAliasExist(long appId, long accountId, AccountCategoryEnum category, String alias) {
        AccountDTO existAccount = accountDAO.getAccount(appId, category, alias);
        if (existAccount != null && !existAccount.getId().equals(accountId)) {
            log.info(
                "Another same alias exists:(appId={}, category={}, alias={})",
                existAccount.getAppId(),
                existAccount.getCategory(),
                existAccount.getAlias()
            );
            throw new AlreadyExistsException(ErrorCode.ACCOUNT_ALIAS_EXIST);
        }
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.MANAGE_ACCOUNT,
        instance = @AuditInstanceRecord(
            resourceType = ResourceTypeId.ACCOUNT,
            instanceIds = "#accountId"
        ),
        content = EventContentConstants.DELETE_ACCOUNT
    )
    public void deleteAccount(User user, long appId, Long accountId) throws ServiceException {
        log.info("Delete account, operator={}, appId={}, accountId={}", user, appId, accountId);
        AccountDTO account = getAccountById(accountId);
        if (account == null) {
            log.info("Account is not exist, accountId={}", accountId);
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
        }

        authManageAccount(user, account.getAppId(), accountId);

        if (isAccountRefByAnyStep(accountId)) {
            log.info("Account:{} is ref by step, should not delete!", accountId);
            throw new FailedPreconditionException(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN);
        }
        if (account.getCategory() == AccountCategoryEnum.SYSTEM && isSystemAccountRefByDbAccount(accountId)) {
            log.info("Account:{} is ref by db account, should not delete!", accountId);
            throw new FailedPreconditionException(ErrorCode.DELETE_REF_ACCOUNT_FORBIDDEN);
        }

        log.info("Delete account, accountId={}", accountId);
        accountDAO.deleteAccount(accountId);

        ActionAuditContext.current().setInstanceName(account.getAccount());
    }

    @Override
    public PageData<AccountDTO> listPageAccount(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition)
        throws ServiceException {
        return accountDAO.listPageAccount(accountQuery, baseSearchCondition);
    }

    @Override
    public PageData<AccountDTO> searchPageAccount(Long appId, String keyword,
                                                  BaseSearchCondition baseSearchCondition) throws ServiceException {
        return accountDAO.searchPageAccount(appId, keyword, baseSearchCondition);
    }

    @Override
    public AccountDTO getAccount(Long appId, AccountCategoryEnum category, String alias) {
        return accountDAO.getAccount(appId, category, alias);
    }

    @Override
    public List<AccountDTO> listAppAccount(Long appId, AccountCategoryEnum category) {
        return accountDAO.listAppAccount(appId, category, null, null, null);
    }

    @Override
    public List<AccountDTO> listAppAccount(Long appId,
                                           AccountCategoryEnum category,
                                           String account,
                                           String alias,
                                           BaseSearchCondition baseSearchCondition) {
        return accountDAO.listAppAccount(appId, category, account, alias, baseSearchCondition);
    }

    @Override
    public Integer countAppAccount(Long appId, AccountCategoryEnum category, String account, String alias) {
        return accountDAO.countAppAccount(appId, category, account, alias);
    }

    @Override
    public boolean isAccountRefByAnyStep(Long accountId) {
        return accountDAO.isAccountRefByAnyScriptStep(accountId) || accountDAO.isAccountRefByAnyFileStep(accountId)
            || accountDAO.isAccountRefByAnySourceFile(accountId);
    }

    @Override
    public boolean isSystemAccountRefByDbAccount(Long accountId) {
        return accountDAO.isAccountRefByDbAccount(accountId);
    }

    @Override
    public boolean checkCreateParam(AccountCreateUpdateReq req, boolean checkAlias, boolean checkAccountName) {
        // 检查账号别名
        if (checkAlias) {
            try {
                StringCheckHelper stringCheckHelper = new StringCheckHelper(
                    new TrimChecker(),
                    new NotEmptyChecker(),
                    new IlegalCharChecker(),
                    new MaxLengthChecker(32)
                );
                req.setAlias(stringCheckHelper.checkAndGetResult(req.getAlias()));
            } catch (StringCheckException e) {
                log.warn("Account alias is invalid:", e);
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"alias", "Account alias is invalid:" + e.getMessage()});
            }
        }
        if (req.getCategory() == null || AccountCategoryEnum.valOf(req.getCategory()) == null) {
            log.warn("Category is invalid, category={}", req.getCategory());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"category", "Account category is invalid, not in [1,2]"});
        }
        AccountTypeEnum accountType = AccountTypeEnum.valueOf(req.getType());
        if (accountType == null) {
            log.warn("Type is invalid, type={}", req.getType());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"type", "Account type is invalid, not in [1,2,9,10,11]"});
        }
        // 检查账号命名规则
        String account = req.getAccount();
        if (StringUtils.isBlank(account)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"account", "Parameter account cannot be blank"});
        }
        if (checkAccountName) {
            OSTypeEnum osType = accountType.getOsType();
            AccountNameRule accountNameRule = globalSettingsService.getCurrentAccountNameRule(osType);
            if (accountNameRule != null) {
                String expression = accountNameRule.getExpression();
                // 正则匹配
                Pattern pattern = Pattern.compile(expression);
                Matcher m = pattern.matcher(req.getAccount());
                if (!m.matches()) {
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                        new String[]{"account", "Parameter account invalid, expression:"
                            + expression
                            + ", rule:"
                            + accountNameRule.getDescription()});
                }
            } else {
                log.warn("Cannot find accountNameRule of osType:{}", osType.name());
            }
        }
        if (req.getCategory().equals(AccountCategoryEnum.DB.getValue())) {
            if (req.getDbPort() == null) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"dbPort", "dbPort cannot be null or empty"});
            }
            if (req.getDbSystemAccountId() == null) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"dbSystemAccountId", "dbSystemAccountId cannot be null or empty"});
            }
        }
        return true;
    }

    @Override
    public AccountDTO buildCreateAccountDTO(String username, long appId, AccountCreateUpdateReq req) {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setAppId(appId);
        accountDTO.setAccount(req.getAccount());
        accountDTO.setCategory(AccountCategoryEnum.valOf(req.getCategory()));
        accountDTO.setType(AccountTypeEnum.valueOf(req.getType()));
        if (StringUtils.isBlank(req.getAlias())) {
            accountDTO.setAlias(req.getAccount());
        } else {
            accountDTO.setAlias(req.getAlias());
        }
        accountDTO.setRemark(req.getRemark());
        accountDTO.setGrantees(Utils.concatStringWithSeperator(req.getGrantees(), ","));

        if (AccountCategoryEnum.SYSTEM.getValue().equals(req.getCategory())) {
            if (AccountTypeEnum.WINDOW.getType().equals(req.getType())) {
                accountDTO.setOs("Windows");
            } else {
                accountDTO.setOs("Linux");
            }
            accountDTO.setPassword(req.getPassword());
        }

        if (AccountCategoryEnum.DB.getValue().equals(req.getCategory())) {
            accountDTO.setDbPort(req.getDbPort());
            accountDTO.setDbPassword(req.getDbPassword());
            accountDTO.setDbSystemAccountId(req.getDbSystemAccountId());
        }

        accountDTO.setCreator(username);
        accountDTO.setCreateTime(DateUtils.currentTimeMillis());
        accountDTO.setLastModifyUser(username);
        accountDTO.setLastModifyTime(DateUtils.currentTimeMillis());

        return accountDTO;
    }

    @Override
    public void createDefaultAccounts(long appId) {
        AccountDTO linuxRoot = accountDAO.getAccount(appId, AccountCategoryEnum.SYSTEM, AccountTypeEnum.LINUX,
            DEFAULT_LINUX_ACCOUNT, DEFAULT_LINUX_ACCOUNT);
        if (linuxRoot == null) {
            linuxRoot = new AccountDTO();
            linuxRoot.setAppId(appId);
            linuxRoot.setCategory(AccountCategoryEnum.SYSTEM);
            linuxRoot.setType(AccountTypeEnum.LINUX);
            linuxRoot.setAccount(DEFAULT_LINUX_ACCOUNT);
            linuxRoot.setAlias(DEFAULT_LINUX_ACCOUNT);
            linuxRoot.setCreator("admin");
            linuxRoot.setCreateTime(DateUtils.currentTimeMillis());
            linuxRoot.setLastModifyUser("admin");
            try {
                accountDAO.saveAccount(linuxRoot);
                log.info("root account of appId={} created", appId);
            } catch (Exception e) {
                log.warn("Fail to create default root account", e);
            }
        }
        AccountDTO windowsSystem = accountDAO.getAccount(appId, AccountCategoryEnum.SYSTEM, AccountTypeEnum.WINDOW,
            DEFAULT_WINDOWS_SYSTEM_ACCOUNT, DEFAULT_WINDOWS_SYSTEM_ACCOUNT);
        if (windowsSystem == null) {
            windowsSystem = new AccountDTO();
            windowsSystem.setAppId(appId);
            windowsSystem.setCategory(AccountCategoryEnum.SYSTEM);
            windowsSystem.setType(AccountTypeEnum.WINDOW);
            windowsSystem.setAccount(DEFAULT_WINDOWS_SYSTEM_ACCOUNT);
            windowsSystem.setAlias(DEFAULT_WINDOWS_SYSTEM_ACCOUNT);
            windowsSystem.setCreator("admin");
            windowsSystem.setCreateTime(DateUtils.currentTimeMillis());
            windowsSystem.setLastModifyUser("admin");
            try {
                accountDAO.saveAccount(windowsSystem);
                log.info("system account of appId={} created", appId);
            } catch (Exception e) {
                log.warn("Fail to create default system account", e);
            }
        }
        AccountDTO windowsAdmin = accountDAO.getAccount(appId, AccountCategoryEnum.SYSTEM, AccountTypeEnum.WINDOW,
            DEFAULT_WINDOWS_ADMIN_ACCOUNT, DEFAULT_WINDOWS_ADMIN_ACCOUNT);
        if (windowsAdmin == null) {
            windowsAdmin = new AccountDTO();
            windowsAdmin.setAppId(appId);
            windowsAdmin.setCategory(AccountCategoryEnum.SYSTEM);
            windowsAdmin.setType(AccountTypeEnum.WINDOW);
            windowsAdmin.setAccount(DEFAULT_WINDOWS_ADMIN_ACCOUNT);
            windowsAdmin.setAlias(DEFAULT_WINDOWS_ADMIN_ACCOUNT);
            windowsAdmin.setCreator("admin");
            windowsAdmin.setCreateTime(DateUtils.currentTimeMillis());
            windowsAdmin.setLastModifyUser("admin");
            try {
                accountDAO.saveAccount(windowsAdmin);
                log.info("administrator account of appId={} created", appId);
            } catch (Exception e) {
                log.warn("Fail to create default administrator account", e);
            }
        }
    }

}
