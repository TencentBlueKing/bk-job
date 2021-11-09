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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.encrypt.Encryptor;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.common.consts.globalsetting.OSTypeEnum;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.AccountDAO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.web.request.AccountCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.globalsetting.AccountNameRule;
import com.tencent.bk.job.manage.service.AccountService;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
    public final String DEFAULT_LINUX_ACCOUNT = "root";
    public final String DEFAULT_WINDOWS_ACCOUNT = "system";
    private final AccountDAO accountDAO;
    private final Encryptor encryptor;
    private final GlobalSettingsService globalSettingsService;
    private final JobManageConfig jobManageConfig;

    public AccountServiceImpl(@Autowired AccountDAO accountDAO, @Qualifier("gseRsaEncryptor") Encryptor encryptor,
                              GlobalSettingsService globalSettingsService,
                              JobManageConfig jobManageConfig) {
        this.accountDAO = accountDAO;
        this.encryptor = encryptor;
        this.globalSettingsService = globalSettingsService;
        this.jobManageConfig = jobManageConfig;
    }

    @Override
    public long saveAccount(AccountDTO account) throws ServiceException {
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
                log.warn("DB related system account is not in current app, systemAccountId={}, systemAccountAppId={}"
                    , account.getDbSystemAccountId(), dbSystemAccount.getAppId());
                throw new NotFoundException(ErrorCode.DB_SYSTEM_ACCOUNT_IS_INVALID);
            }
            if (StringUtils.isNotEmpty(account.getDbPassword())) {
                account.setDbPassword(encryptPassword(account.getDbPassword()));
            }
        }
        if (account.getId() == null) {
            return accountDAO.saveAccount(account);
        } else {
            return accountDAO.saveAccountWithId(account);
        }
    }

    @Override
    public AccountDTO getAccountById(Long accountId) throws ServiceException {
        return accountDAO.getAccountById(accountId);
    }

    @Override
    public AccountDTO getAccountByAccount(Long appId, String account) throws ServiceException {
        return accountDAO.getAccountByAccount(appId, account);
    }

    @Override
    public void updateAccount(AccountDTO account) throws ServiceException {
        if (StringUtils.isNotEmpty(account.getPassword())) {
            account.setPassword(encryptor.encrypt(account.getPassword()));
        }
        if (account.getCategory() == AccountCategoryEnum.DB && StringUtils.isNotEmpty(account.getDbPassword())) {
            account.setDbPassword(encryptPassword(account.getDbPassword()));
        }
        log.info("Update account, account={}", account);
        accountDAO.updateAccount(account);
    }

    private String encryptPassword(String text) throws ServiceException {
        try {
            return AESUtils.encryptToBase64EncodedCipherText(text, jobManageConfig.getEncryptPassword());
        } catch (Exception e) {
            log.error("Encrypt password error", e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteAccount(Long accountId) throws ServiceException {
        log.info("Delete account, accountId={}", accountId);
        accountDAO.deleteAccount(accountId);
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
    public List<AccountDTO> listAllAppAccount(Long appId, AccountCategoryEnum category) {
        return accountDAO.listAllAppAccount(appId, category, null);
    }

    @Override
    public List<AccountDTO> listAllAppAccount(Long appId, AccountCategoryEnum category,
                                              BaseSearchCondition baseSearchCondition) {
        return accountDAO.listAllAppAccount(appId, category, baseSearchCondition);
    }

    @Override
    public Integer countAllAppAccount(Long appId, AccountCategoryEnum category) {
        return accountDAO.countAllAppAccount(appId, category);
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
                    new String[] {"alias", "Account alias is invalid:" + e.getMessage()});
            }
        }
        if (req.getCategory() == null || AccountCategoryEnum.valOf(req.getCategory()) == null) {
            log.warn("Category is invalid, category={}", req.getCategory());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[] {"category",  "Account category is invalid, not in [1,2]"});
        }
        AccountTypeEnum accountType = AccountTypeEnum.valueOf(req.getType());
        if (accountType == null) {
            log.warn("Type is invalid, type={}", req.getType());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[] {"type",  "Account type is invalid, not in [1,2,9,10,11]"});
        }
        // 检查账号命名规则
        String account = req.getAccount();
        if (StringUtils.isBlank(account)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[] {"account",  "Parameter account cannot be blank"});
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
                        new String[] {"account",  "Parameter account invalid, expression:"
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
                    new String[] {"dbPort", "dbPort cannot be null or empty"});
            }
            if (req.getDbSystemAccountId() == null) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[] {"dbSystemAccountId",  "dbSystemAccountId cannot be null or empty"});
            }
        }
        return true;
    }

    @Override
    public AccountDTO buildCreateAccountDTO(String operator, long appId, AccountCreateUpdateReq req) {
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

        accountDTO.setCreator(operator);
        accountDTO.setCreateTime(DateUtils.currentTimeMillis());
        accountDTO.setLastModifyUser(operator);
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
            DEFAULT_WINDOWS_ACCOUNT, DEFAULT_WINDOWS_ACCOUNT);
        if (windowsSystem == null) {
            windowsSystem = new AccountDTO();
            windowsSystem.setAppId(appId);
            windowsSystem.setCategory(AccountCategoryEnum.SYSTEM);
            windowsSystem.setType(AccountTypeEnum.WINDOW);
            windowsSystem.setAccount(DEFAULT_WINDOWS_ACCOUNT);
            windowsSystem.setAlias(DEFAULT_WINDOWS_ACCOUNT);
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
    }

    @Override
    public Integer countAccounts(AccountTypeEnum accountType) {
        return accountDAO.countAccounts(accountType);
    }
}
