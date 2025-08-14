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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.crypto.scenario.DbPasswordCryptoService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.common.constants.account.AccountTypeEnum;
import com.tencent.bk.job.manage.dao.AccountDAO;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import com.tencent.bk.job.manage.model.dto.AccountDisplayDTO;
import com.tencent.bk.job.manage.model.tables.Account;
import com.tencent.bk.job.manage.model.tables.Application;
import com.tencent.bk.job.manage.model.tables.TaskTemplate;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStep;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepFile;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepFileList;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepScript;
import com.tencent.bk.job.manage.model.tables.records.AccountRecord;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SortField;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class AccountDAOImpl implements AccountDAO {
    private static final Account TB_ACCOUNT = Account.ACCOUNT;
    private static final Application TB_APP = Application.APPLICATION;
    private static final TableField[] ALL_FILED = {
        TB_ACCOUNT.ID,
        TB_ACCOUNT.ACCOUNT_,
        TB_ACCOUNT.ALIAS,
        TB_ACCOUNT.CATEGORY,
        TB_ACCOUNT.TYPE,
        TB_ACCOUNT.APP_ID,
        TB_ACCOUNT.GRANTEE,
        TB_ACCOUNT.REMARK,
        TB_ACCOUNT.OS,
        TB_ACCOUNT.PASSWORD,
        TB_ACCOUNT.DB_PASSWORD,
        TB_ACCOUNT.DB_PORT,
        TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID,
        TB_ACCOUNT.CREATOR,
        TB_ACCOUNT.CREATE_TIME,
        TB_ACCOUNT.LAST_MODIFY_USER,
        TB_ACCOUNT.LAST_MODIFY_TIME
    };
    private final DSLContext ctx;
    private final DbPasswordCryptoService dbPasswordCryptoService;

    @Autowired
    public AccountDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext create,
                          DbPasswordCryptoService dbPasswordCryptoService) {
        this.ctx = create;
        this.dbPasswordCryptoService = dbPasswordCryptoService;
    }

    @Override
    public long saveAccountWithId(AccountDTO account) {
        Record record = ctx.insertInto(
            TB_ACCOUNT,
            TB_ACCOUNT.ID,
            TB_ACCOUNT.ACCOUNT_,
            TB_ACCOUNT.ALIAS,
            TB_ACCOUNT.CATEGORY,
            TB_ACCOUNT.TYPE,
            TB_ACCOUNT.APP_ID,
            TB_ACCOUNT.GRANTEE,
            TB_ACCOUNT.REMARK,
            TB_ACCOUNT.OS,
            TB_ACCOUNT.PASSWORD,
            TB_ACCOUNT.DB_PASSWORD,
            TB_ACCOUNT.DB_PORT,
            TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID,
            TB_ACCOUNT.CREATOR,
            TB_ACCOUNT.CREATE_TIME,
            TB_ACCOUNT.LAST_MODIFY_USER,
            TB_ACCOUNT.LAST_MODIFY_TIME
        ).values(
            account.getId(),
            account.getAccount(),
            account.getAlias(),
            JooqDataTypeUtil.getByteFromInteger(account.getCategory().getValue()),
            JooqDataTypeUtil.getByteFromInteger(account.getType().getType()),
            account.getAppId(),
            account.getGrantees(),
            account.getRemark(),
            account.getOs(),
            account.getPassword(),
            dbPasswordCryptoService.encryptDbPasswordIfNeeded(account.getCategory(), account.getDbPassword()),
            account.getDbPort(),
            account.getDbSystemAccountId(),
            account.getCreator(),
            ULong.valueOf(account.getCreateTime()),
            account.getLastModifyUser(),
            ULong.valueOf(account.getLastModifyTime())
        ).returning(TB_ACCOUNT.ID)
            .fetchOne();
        assert record != null;
        return record.get(TB_ACCOUNT.ID);
    }

    @Override
    public long saveAccount(AccountDTO account) {
        Record record = ctx.insertInto(TB_ACCOUNT,
            TB_ACCOUNT.ACCOUNT_,
            TB_ACCOUNT.ALIAS,
            TB_ACCOUNT.CATEGORY,
            TB_ACCOUNT.TYPE,
            TB_ACCOUNT.APP_ID,
            TB_ACCOUNT.GRANTEE,
            TB_ACCOUNT.REMARK,
            TB_ACCOUNT.OS,
            TB_ACCOUNT.PASSWORD,
            TB_ACCOUNT.DB_PASSWORD,
            TB_ACCOUNT.DB_PORT,
            TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID,
            TB_ACCOUNT.CREATOR,
            TB_ACCOUNT.CREATE_TIME,
            TB_ACCOUNT.LAST_MODIFY_USER,
            TB_ACCOUNT.LAST_MODIFY_TIME
        ).values(account.getAccount(),
            account.getAlias(),
            JooqDataTypeUtil.getByteFromInteger(account.getCategory().getValue()),
            JooqDataTypeUtil.getByteFromInteger(account.getType().getType()),
            account.getAppId(),
            account.getGrantees(),
            account.getRemark(),
            account.getOs(),
            account.getPassword(),
            dbPasswordCryptoService.encryptDbPasswordIfNeeded(account.getCategory(), account.getDbPassword()),
            account.getDbPort(),
            account.getDbSystemAccountId(),
            account.getCreator(),
            ULong.valueOf(account.getCreateTime()),
            account.getLastModifyUser(),
            ULong.valueOf(DateUtils.currentTimeMillis())
        ).returning(TB_ACCOUNT.ID)
            .fetchOne();
        assert record != null;
        return record.get(TB_ACCOUNT.ID);
    }

    @Override
    public AccountDTO getAccountById(Long accountId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_ACCOUNT.ID.eq(accountId));
        return getOnlyOneAccountByConditions(conditions);
    }

    @Override
    public List<AccountDisplayDTO> listAccountDisplayInfoByIds(Collection<Long> accountIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_ACCOUNT.ID.in(accountIds));
        val records = ctx.select(
            TB_ACCOUNT.ID,
            TB_ACCOUNT.ACCOUNT_,
            TB_ACCOUNT.ALIAS,
            TB_ACCOUNT.CATEGORY,
            TB_ACCOUNT.TYPE,
            TB_ACCOUNT.APP_ID
        ).from(TB_ACCOUNT)
            .where(conditions)
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetch();
        if (records.size() == 0) {
            return Collections.emptyList();
        } else {
            return records.map(this::extractDisplayInfo);
        }
    }

    @Override
    public AccountDTO getAccountByAccount(Long appId, String account) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_ACCOUNT.APP_ID.eq(appId));
        conditions.add(TB_ACCOUNT.ACCOUNT_.eq(account));
        List<AccountDTO> accountDTOList = getAccountsByConditions(conditions);
        if (accountDTOList == null || accountDTOList.isEmpty()) {
            return null;
        } else {
            return accountDTOList.get(0);
        }
    }

    public AccountDTO getOnlyOneAccountByConditions(Collection<Condition> conditions) {
        Record record = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(conditions)
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne();
        return extract(record);
    }

    public List<AccountDTO> getAccountsByConditions(Collection<Condition> conditions) {
        val records = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(conditions)
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetch();
        if (records.size() == 0) {
            return Collections.emptyList();
        } else {
            return records.map(this::extract);
        }
    }

    private AccountDisplayDTO extractDisplayInfo(Record record) {
        if (record == null) {
            return null;
        }
        AccountDisplayDTO accountDisplayDTO = new AccountDisplayDTO();
        accountDisplayDTO.setId(record.get(TB_ACCOUNT.ID));
        accountDisplayDTO.setAccount(record.get(TB_ACCOUNT.ACCOUNT_));
        accountDisplayDTO.setAlias(record.get(TB_ACCOUNT.ALIAS));
        accountDisplayDTO.setCategory(AccountCategoryEnum.valOf(record.get(TB_ACCOUNT.CATEGORY).intValue()));
        accountDisplayDTO.setType(AccountTypeEnum.valueOf(record.get(TB_ACCOUNT.TYPE).intValue()));
        accountDisplayDTO.setAppId(record.get(TB_ACCOUNT.APP_ID));
        return accountDisplayDTO;
    }

    private AccountDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        AccountDTO account = new AccountDTO();
        account.setId(record.get(TB_ACCOUNT.ID));
        account.setAccount(record.get(TB_ACCOUNT.ACCOUNT_));
        account.setAlias(record.get(TB_ACCOUNT.ALIAS));
        account.setCategory(AccountCategoryEnum.valOf(record.get(TB_ACCOUNT.CATEGORY).intValue()));
        account.setType(AccountTypeEnum.valueOf(record.get(TB_ACCOUNT.TYPE).intValue()));
        account.setAppId(record.get(TB_ACCOUNT.APP_ID));
        account.setGrantees(record.get(TB_ACCOUNT.GRANTEE));
        account.setRemark(record.get(TB_ACCOUNT.REMARK));
        account.setOs(record.get(TB_ACCOUNT.OS));
        account.setPassword(record.get(TB_ACCOUNT.PASSWORD));

        // 解密DB账号密码
        String encryptedDbPassword = record.get(TB_ACCOUNT.DB_PASSWORD);
        String dbPassword = dbPasswordCryptoService.decryptDbPasswordIfNeeded(
            account.getCategory(),
            encryptedDbPassword
        );

        account.setDbPassword(dbPassword);
        account.setDbPort(record.get(TB_ACCOUNT.DB_PORT));
        account.setDbSystemAccountId(record.get(TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID));
        account.setCreator(record.get(TB_ACCOUNT.CREATOR));
        account.setCreateTime(record.get(TB_ACCOUNT.CREATE_TIME).longValue());
        account.setLastModifyUser(record.get(TB_ACCOUNT.LAST_MODIFY_USER));
        account.setLastModifyTime(record.get(TB_ACCOUNT.LAST_MODIFY_TIME).longValue());
        return account;
    }

    @Override
    public void updateAccount(AccountDTO account) {
        UpdateSetMoreStep<AccountRecord> update = ctx.update(TB_ACCOUNT)
            .set(TB_ACCOUNT.GRANTEE, account.getGrantees())
            .set(TB_ACCOUNT.REMARK, account.getRemark())
            .set(TB_ACCOUNT.DB_PORT, account.getDbPort())
            .set(TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID, account.getDbSystemAccountId())
            .set(TB_ACCOUNT.LAST_MODIFY_USER, account.getLastModifyUser())
            .set(TB_ACCOUNT.LAST_MODIFY_TIME, ULong.valueOf(DateUtils.currentTimeMillis()));
        if (StringUtils.isNotEmpty(account.getAlias())) {
            update.set(TB_ACCOUNT.ALIAS, account.getAlias());
        }
        if (StringUtils.isNotEmpty(account.getPassword())) {
            update.set(TB_ACCOUNT.PASSWORD, account.getPassword());
        }
        if (StringUtils.isNotEmpty(account.getDbPassword())) {
            update.set(TB_ACCOUNT.DB_PASSWORD, dbPasswordCryptoService.encryptDbPasswordIfNeeded(
                account.getCategory(),
                account.getDbPassword()
            ));
        }
        update.where(TB_ACCOUNT.ID.eq(account.getId()))
            .execute();
    }

    @Override
    public void deleteAccount(Long accountId) {
        deleteAccountHardly(accountId);
    }

    private void deleteAccountHardly(Long accountId) {
        ctx.deleteFrom(TB_ACCOUNT)
            .where(TB_ACCOUNT.ID.eq(accountId)).execute();
    }

    @Override
    public PageData<AccountDTO> searchPageAccount(Long appId, String keyword,
                                                  BaseSearchCondition baseSearchCondition) {
        long count = getPageAccountCount(appId, keyword, baseSearchCondition);
        List<Condition> conditions = buildConditionList(appId, keyword, baseSearchCondition);
        return listPageAccountByConditions(baseSearchCondition, conditions, count);
    }

    @Override
    public PageData<AccountDTO> listPageAccount(AccountDTO accountQuery,
                                                BaseSearchCondition baseSearchCondition) {
        long count = getPageAccountCount(accountQuery, baseSearchCondition);
        List<Condition> conditions = buildConditionList(accountQuery, baseSearchCondition);
        return listPageAccountByConditions(baseSearchCondition, conditions, count);
    }

    public PageData<AccountDTO> listPageAccountByConditions(BaseSearchCondition baseSearchCondition,
                                                            List<Condition> conditions, long count) {

        Collection<SortField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(TB_ACCOUNT.LAST_MODIFY_TIME.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("alias".equals(orderField)) {
                //升序
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_ACCOUNT.ALIAS.asc());
                } else {
                    orderFields.add(TB_ACCOUNT.ALIAS.desc());
                }
            } else if ("account".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_ACCOUNT.ACCOUNT_.asc());
                } else {
                    orderFields.add(TB_ACCOUNT.ACCOUNT_.desc());
                }
            } else if ("lastModifyTime".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(TB_ACCOUNT.LAST_MODIFY_TIME.asc());
                } else {
                    orderFields.add(TB_ACCOUNT.LAST_MODIFY_TIME.desc());
                }
            }
        }

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result<Record> result =
            ctx.select(ALL_FILED)
                .from(TB_ACCOUNT)
                .where(conditions)
                .orderBy(orderFields)
                .limit(start, length).fetch();
        List<AccountDTO> accounts = new ArrayList<>();
        if (result.size() != 0) {
            result.map(record -> {
                accounts.add(extract(record));
                return null;
            });
        }

        PageData<AccountDTO> accountPageData = new PageData<>();
        accountPageData.setTotal(count);
        accountPageData.setPageSize(length);
        accountPageData.setData(accounts);
        accountPageData.setStart(start);
        return accountPageData;
    }


    /**
     * 查询符合条件的账号数量
     */
    private long getPageAccountCount(Long appId, String keyword, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(appId, keyword, baseSearchCondition);
        Long count = ctx.selectCount().from(TB_ACCOUNT).where(conditions).fetchOne(0, Long.class);
        assert count != null;
        return count;
    }


    /**
     * 查询符合条件的账号数量
     */
    private long getPageAccountCount(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(accountQuery, baseSearchCondition);
        return countAccountByConditions(conditions);
    }

    private long countAccountByConditions(List<Condition> conditions) {
        Long count = ctx.selectCount().from(TB_ACCOUNT).where(conditions).fetchOne(0, Long.class);
        assert count != null;
        return count;
    }

    private List<Condition> buildConditionList(AccountDTO accountQuery, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        if (accountQuery.getId() != null) {
            conditions.add(TB_ACCOUNT.ID.eq(accountQuery.getId()));
        }
        if (accountQuery.getCategory() != null) {
            conditions.add(TB_ACCOUNT.CATEGORY.eq(JooqDataTypeUtil.getByteFromInteger(accountQuery.getCategory().getValue())));
        }
        if (accountQuery.getType() != null) {
            conditions.add(TB_ACCOUNT.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(accountQuery.getType().getType())));
        }
        if (StringUtils.isNotBlank(accountQuery.getAccount())) {
            conditions.add(TB_ACCOUNT.ACCOUNT_.like("%" + accountQuery.getAccount() + "%"));
        }
        if (StringUtils.isNotBlank(accountQuery.getAlias())) {
            conditions.add(TB_ACCOUNT.ALIAS.like("%" + accountQuery.getAlias() + "%"));
        }
        if (StringUtils.isNotBlank(accountQuery.getCreator())) {
            conditions.add(TB_ACCOUNT.CREATOR.like("%" + accountQuery.getCreator() + "%"));
        }
        if (StringUtils.isNotBlank(accountQuery.getLastModifyUser())) {
            conditions.add(TB_ACCOUNT.LAST_MODIFY_USER.like("%" + accountQuery.getLastModifyUser() + "%"));
        }
        if (StringUtils.isNotBlank(accountQuery.getRemark())) {
            conditions.add(TB_ACCOUNT.REMARK.like("%" + accountQuery.getRemark() + "%"));
        }
        conditions.add(TB_ACCOUNT.APP_ID.eq(accountQuery.getAppId()));
        conditions.add(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)));

        return conditions;
    }

    private List<Condition> buildConditionList(Long appId, String keyword, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        Condition condition;
        if (keyword != null) {
            String likeKeyword = "%" + keyword + "%";
            condition = TB_ACCOUNT.ID.like(likeKeyword);
            condition.or(TB_ACCOUNT.ACCOUNT_.like(likeKeyword));
            condition.or(TB_ACCOUNT.ALIAS.like(likeKeyword));
            condition.or(TB_ACCOUNT.CREATOR.like(likeKeyword));
            condition.or(TB_ACCOUNT.LAST_MODIFY_USER.like(likeKeyword));
            condition.or(TB_ACCOUNT.REMARK.like(likeKeyword));
            conditions.add(condition);
        }
        conditions.add(TB_ACCOUNT.APP_ID.eq(appId));
        conditions.add(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)));
        return conditions;
    }

    private List<Condition> genBaseConditions(Long appId, AccountCategoryEnum category, String account, String alias) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)));
        if (appId != null) {
            conditions.add(TB_ACCOUNT.APP_ID.eq(appId));
        }
        if (category != null) {
            conditions.add(TB_ACCOUNT.CATEGORY.eq(JooqDataTypeUtil.getByteFromInteger(category.getValue())));
        }
        if (StringUtils.isNotBlank(account)) {
            conditions.add(TB_ACCOUNT.ACCOUNT_.eq(account));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(TB_ACCOUNT.ALIAS.eq(alias));
        }
        return conditions;
    }

    @Override
    public List<AccountDTO> listAppAccount(Long appId,
                                           AccountCategoryEnum category,
                                           String account,
                                           String alias,
                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = genBaseConditions(appId, category, account, alias);
        return listAllAppAccount(conditions, baseSearchCondition);
    }

    @Override
    public Integer countAppAccount(Long appId, AccountCategoryEnum category, String account, String alias) {
        List<Condition> conditions = genBaseConditions(appId, category, account, alias);
        return countAllAppAccount(conditions);
    }

    public Integer countAllAppAccount(Collection<Condition> conditions) {
        Record1<Integer> record = ctx.selectCount()
            .from(TB_ACCOUNT)
            .where(conditions).fetchOne();
        if (record != null) {
            return record.value1();
        }
        return 0;
    }

    public List<AccountDTO> listAllAppAccount(Collection<Condition> conditions,
                                              BaseSearchCondition baseSearchCondition) {
        SelectConditionStep<Record> select = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(conditions);
        Result<Record> result;
        if (baseSearchCondition == null) {
            result = select.fetch();
        } else {
            Integer start = baseSearchCondition.getStart();
            if (start == null || start < 0) {
                start = 0;
            }
            Integer length = baseSearchCondition.getLength();
            if (length == null || length < 0) {
                length = 20;
            }
            result = select.limit(start, length).fetch();
        }
        List<AccountDTO> accountDTOS = new ArrayList<>();
        if (result.size() != 0) {
            result.into(record -> accountDTOS.add(extract(record)));
        }
        return accountDTOS;
    }

    @Override
    public AccountDTO getAccount(Long appId, AccountCategoryEnum category, String alias) {
        Record record = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(TB_ACCOUNT.APP_ID.eq(appId))
            .and(TB_ACCOUNT.CATEGORY.eq(JooqDataTypeUtil.getByteFromInteger(category.getValue())))
            .and(TB_ACCOUNT.ALIAS.eq(alias))
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetchOne();
        return extract(record);
    }

    @Override
    public AccountDTO getAccount(Long appId, AccountCategoryEnum category, AccountTypeEnum type, String account,
                                 String alias) {
        List<Condition> conditions = genBaseConditions(appId, category, account, alias);
        if (type != null) {
            conditions.add(TB_ACCOUNT.TYPE.eq(JooqDataTypeUtil.getByteFromInteger(type.getType())));
        }
        Record record = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(conditions)
            .fetchOne();
        return extract(record);
    }

    @Override
    public boolean isAccountRefByAnyScriptStep(Long accountId) {
        TaskTemplateStepScript tbStepScript = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;
        TaskTemplateStep tbStep = TaskTemplateStep.TASK_TEMPLATE_STEP;
        TaskTemplate tbTemplate = TaskTemplate.TASK_TEMPLATE;
        Record record = ctx.select(tbStepScript.STEP_ID)
            .from(tbStepScript)
            .join(tbStep)
            .on(tbStepScript.STEP_ID.eq(tbStep.ID))
            .join(tbTemplate)
            .on(tbStep.TEMPLATE_ID.eq(tbTemplate.ID))
            .where(tbStepScript.EXECUTE_ACCOUNT.eq(ULong.valueOf(accountId)))
            .and(tbTemplate.IS_DELETED.eq(UByte.valueOf(0)))
            .limit(1)
            .fetchOne();
        return record != null;
    }

    @Override
    public boolean isAccountRefByAnyFileStep(Long accountId) {
        TaskTemplateStepFile tbStepFile = TaskTemplateStepFile.TASK_TEMPLATE_STEP_FILE;
        TaskTemplateStep tbStep = TaskTemplateStep.TASK_TEMPLATE_STEP;
        TaskTemplate tbTemplate = TaskTemplate.TASK_TEMPLATE;
        Record record = ctx.select(tbStepFile.STEP_ID)
            .from(tbStepFile)
            .join(tbStep)
            .on(tbStepFile.STEP_ID.eq(tbStep.ID))
            .join(tbTemplate)
            .on(tbStep.TEMPLATE_ID.eq(tbTemplate.ID))
            .where(tbStepFile.EXECUTE_ACCOUNT.eq(ULong.valueOf(accountId)))
            .and(tbTemplate.IS_DELETED.eq(UByte.valueOf(0)))
            .limit(1)
            .fetchOne();
        return record != null;
    }

    @Override
    public boolean isAccountRefByAnySourceFile(Long accountId) {
        TaskTemplateStepFileList tbStepFileList = TaskTemplateStepFileList.TASK_TEMPLATE_STEP_FILE_LIST;
        TaskTemplateStep tbStep = TaskTemplateStep.TASK_TEMPLATE_STEP;
        TaskTemplate tbTemplate = TaskTemplate.TASK_TEMPLATE;
        Record record = ctx.select(tbStepFileList.STEP_ID)
            .from(tbStepFileList)
            .join(tbStep)
            .on(tbStepFileList.STEP_ID.eq(tbStep.ID))
            .join(tbTemplate)
            .on(tbStep.TEMPLATE_ID.eq(tbTemplate.ID))
            .where(tbStepFileList.HOST_ACCOUNT.eq(ULong.valueOf(accountId)))
            .and(tbTemplate.IS_DELETED.eq(UByte.valueOf(0)))
            .limit(1)
            .fetchOne();
        return record != null;
    }

    @Override
    public boolean isAccountRefByDbAccount(Long accountId) {
        Record record = ctx.select(TB_ACCOUNT.ID)
            .from(TB_ACCOUNT)
            .where(TB_ACCOUNT.DB_SYSTEM_ACCOUNT_ID.eq(accountId))
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .limit(1)
            .fetchOne();
        return record != null;
    }

    @Override
    public Integer countAccounts(AccountTypeEnum accountType) {
        List<Condition> conditions = buildConditions(accountType);
        return countByConditions(conditions);
    }

    @Override
    public Integer countAccounts(String tenantId, AccountTypeEnum accountType) {
        List<Condition> conditions = buildConditions(accountType);
        conditions.add(TB_APP.TENANT_ID.eq(tenantId));
        return ctx.selectCount()
            .from(TB_ACCOUNT)
            .join(TB_APP)
            .on(TB_ACCOUNT.APP_ID.eq(TB_APP.APP_ID.cast(Long.class)))
            .where(conditions)
            .fetchOne(0, Integer.class);
    }

    List<Condition> buildConditions(AccountTypeEnum accountType) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)));
        if (accountType != null) {
            conditions.add(TB_ACCOUNT.TYPE.eq(accountType.getType().byteValue()));
        }
        return conditions;
    }

    private Integer countByConditions(List<Condition> conditions) {
        return ctx.selectCount()
            .from(TB_ACCOUNT)
            .where(conditions)
            .fetchOne(0, Integer.class);
    }

    @Override
    public List<AccountDTO> listAccountByAccountCategory(AccountCategoryEnum accountCategoryEnum) {
        Result<?> records = ctx.select(ALL_FILED)
            .from(TB_ACCOUNT)
            .where(TB_ACCOUNT.CATEGORY.eq(JooqDataTypeUtil.getByteFromInteger(accountCategoryEnum.getValue())))
            .and(TB_ACCOUNT.IS_DELETED.eq(UByte.valueOf(0)))
            .fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::extract);
        }
    }

    public void batchUpdateDbAccountPassword(List<AccountDTO> updatedAccounts) {
        ctx.transaction(configuration -> {
            for (AccountDTO account : updatedAccounts) {
                updateDbPasswordAccount(configuration.dsl(), account);
            }
        });
    }

    private void updateDbPasswordAccount(DSLContext dsl, AccountDTO account) {
        dsl.update(TB_ACCOUNT).set(
            TB_ACCOUNT.DB_PASSWORD, dbPasswordCryptoService.encryptDbPasswordIfNeeded(
                account.getCategory(),
                account.getDbPassword()
            )
        ).where(TB_ACCOUNT.ID.eq(account.getId()))
            .and(TB_ACCOUNT.CATEGORY.eq(JooqDataTypeUtil.getByteFromInteger(AccountCategoryEnum.DB.getValue())))
            .execute();
    }
}
