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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountTypeEnum;
import com.tencent.bk.job.manage.model.dto.AccountDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_account_data.sql"})
@DisplayName("账号管理DAO集成测试")
public class AccountDAOImplIntegrationTest {
    @Autowired
    private AccountDAO accountDAO;


    @Test
    @DisplayName("测试根据ID获取账号详情")
    public void testGetAccountById() {
        long accountId = 1L;

        AccountDTO actual = accountDAO.getAccountById(accountId);

        assertThat(actual.getId()).isEqualTo(accountId);
        assertThat(actual.getAlias()).isEqualTo("root");
        assertThat(actual.getAccount()).isEqualTo("root");
        assertThat(actual.getCategory()).isEqualTo(AccountCategoryEnum.SYSTEM);
        assertThat(actual.getType()).isEqualTo(AccountTypeEnum.LINUX);
        assertThat(actual.getAppId()).isEqualTo(2L);
        assertThat(actual.getGrantees()).isEqualTo("user1,user2");
        assertThat(actual.getRemark()).isEqualTo("root-linux");
        assertThat(actual.getOs()).isEqualTo("Linux");
        assertThat(actual.getPassword()).isNullOrEmpty();
        assertThat(actual.getDbPassword()).isNullOrEmpty();
        assertThat(actual.getDbPort()).isNull();
        assertThat(actual.getDbSystemAccountId()).isNull();
        assertThat(actual.getCreator()).isEqualTo("admin");
        assertThat(actual.getCreateTime()).isEqualTo(1569550210000L);
        assertThat(actual.getLastModifyUser()).isEqualTo("admin");
        assertThat(actual.getLastModifyTime()).isEqualTo(1569550210000L);
    }

    @Test
    @DisplayName("测试保存系统账号")
    public void testSaveSystemAccount() {
        AccountDTO osAccount = new AccountDTO();
        osAccount.setAccount("user1");
        osAccount.setAlias("user1");
        osAccount.setCategory(AccountCategoryEnum.SYSTEM);
        osAccount.setType(AccountTypeEnum.LINUX);
        osAccount.setAppId(2L);
        osAccount.setGrantees("user1");
        osAccount.setRemark("linux-user1");
        osAccount.setOs("Linux");
        osAccount.setCreator("admin");
        osAccount.setCreateTime(1569550210000L);
        osAccount.setLastModifyUser("admin");
        osAccount.setLastModifyTime(1569550210000L);

        long accountId = accountDAO.saveAccount(osAccount);
        AccountDTO actual = accountDAO.getAccountById(accountId);

        assertThat(actual.getAccount()).isEqualTo(osAccount.getAccount());
        assertThat(actual.getAlias()).isEqualTo(osAccount.getAlias());
        assertThat(actual.getCategory()).isEqualTo(osAccount.getCategory());
        assertThat(actual.getType()).isEqualTo(osAccount.getType());
        assertThat(actual.getAppId()).isEqualTo(osAccount.getAppId());
        assertThat(actual.getGrantees()).isEqualTo(osAccount.getGrantees());
        assertThat(actual.getRemark()).isEqualTo(osAccount.getRemark());
        assertThat(actual.getOs()).isEqualTo(osAccount.getOs());
        assertThat(actual.getCreator()).isEqualTo(osAccount.getCreator());
        assertThat(actual.getCreateTime()).isEqualTo(osAccount.getCreateTime());
        assertThat(actual.getLastModifyUser()).isEqualTo(osAccount.getLastModifyUser());
    }

    @Test
    @DisplayName("测试保存DB账号")
    public void testSaveDbAccount() {
        AccountDTO dbAccount = new AccountDTO();
        dbAccount.setAccount("job_manage");
        dbAccount.setAlias("job_manage");
        dbAccount.setCategory(AccountCategoryEnum.DB);
        dbAccount.setType(AccountTypeEnum.MYSQL);
        dbAccount.setAppId(2L);
        dbAccount.setGrantees("user1");
        dbAccount.setRemark("db-mysql-job_manage");
        dbAccount.setDbPort(3500);
        dbAccount.setDbPassword("dbpassword");
        dbAccount.setDbSystemAccountId(1L);
        dbAccount.setCreator("admin");
        dbAccount.setCreateTime(1569550210000L);
        dbAccount.setLastModifyUser("admin");
        dbAccount.setLastModifyTime(1569550210000L);

        long accountId = accountDAO.saveAccount(dbAccount);
        AccountDTO actual = accountDAO.getAccountById(accountId);

        assertThat(actual.getAccount()).isEqualTo(dbAccount.getAccount());
        assertThat(actual.getAlias()).isEqualTo(dbAccount.getAlias());
        assertThat(actual.getCategory()).isEqualTo(dbAccount.getCategory());
        assertThat(actual.getType()).isEqualTo(dbAccount.getType());
        assertThat(actual.getAppId()).isEqualTo(dbAccount.getAppId());
        assertThat(actual.getGrantees()).isEqualTo(dbAccount.getGrantees());
        assertThat(actual.getRemark()).isEqualTo(dbAccount.getRemark());
        assertThat(actual.getDbPort()).isEqualTo(dbAccount.getDbPort());
        assertThat(actual.getDbPassword()).isEqualTo(dbAccount.getDbPassword());
        assertThat(actual.getDbSystemAccountId()).isEqualTo(dbAccount.getDbSystemAccountId());
        assertThat(actual.getCreator()).isEqualTo(dbAccount.getCreator());
        assertThat(actual.getCreateTime()).isEqualTo(dbAccount.getCreateTime());
        assertThat(actual.getLastModifyUser()).isEqualTo(dbAccount.getLastModifyUser());
    }

    @Test
    @DisplayName("测试删除账号")
    public void testDeleteAccount() {
        long accountId = 1L;

        accountDAO.deleteAccount(accountId);

        AccountDTO account = accountDAO.getAccountById(accountId);

        assertThat(account).isNull();
    }

    @Test
    @DisplayName("测试更新系统账号")
    public void testUpdateSystemAccount() {
        long accountId = 1L;

        AccountDTO osAccount = new AccountDTO();
        osAccount.setId(accountId);
        osAccount.setGrantees("user1,user2,user3");
        osAccount.setRemark("linux-root-new");
        osAccount.setPassword("new-password");
        osAccount.setLastModifyUser("admin");

        accountDAO.updateAccount(osAccount);

        AccountDTO actual = accountDAO.getAccountById(accountId);

        assertThat(actual.getId()).isEqualTo(accountId);
        assertThat(actual.getGrantees()).isEqualTo(osAccount.getGrantees());
        assertThat(actual.getRemark()).isEqualTo(osAccount.getRemark());
        assertThat(actual.getPassword()).isEqualTo(osAccount.getPassword());
        assertThat(actual.getLastModifyUser()).isEqualTo(osAccount.getLastModifyUser());
    }

    @Test
    @DisplayName("测试更新DB账号")
    public void testUpdateDBAccount() {
        long accountId = 1L;

        AccountDTO dbAccount = new AccountDTO();
        dbAccount.setId(accountId);
        dbAccount.setGrantees("user1,user2,user3");
        dbAccount.setRemark("linux-root-new");
        dbAccount.setLastModifyUser("admin");
        dbAccount.setLastModifyTime(1569550210000L);
        dbAccount.setDbPassword("new_password");
        dbAccount.setDbPort(3700);
        dbAccount.setDbSystemAccountId(2L);

        accountDAO.updateAccount(dbAccount);

        AccountDTO actual = accountDAO.getAccountById(accountId);

        assertThat(actual.getId()).isEqualTo(accountId);
        assertThat(actual.getGrantees()).isEqualTo(dbAccount.getGrantees());
        assertThat(actual.getRemark()).isEqualTo(dbAccount.getRemark());
        assertThat(actual.getDbPassword()).isEqualTo(dbAccount.getDbPassword());
        assertThat(actual.getDbPort()).isEqualTo(dbAccount.getDbPort());
        assertThat(actual.getDbSystemAccountId()).isEqualTo(dbAccount.getDbSystemAccountId());
        assertThat(actual.getLastModifyUser()).isEqualTo(dbAccount.getLastModifyUser());
    }

    @Test
    @DisplayName("测试分页获取账号列表")
    public void testListPageAccount() {
        String alias = "root";
        AccountCategoryEnum category = AccountCategoryEnum.SYSTEM;
        AccountTypeEnum type = AccountTypeEnum.LINUX;
        String lastModifyUser = "admin";

        String orderField = "lastModifyTime";
        Integer order = 1;

        AccountDTO accountQuery = new AccountDTO();
        accountQuery.setAppId(2L);
        accountQuery.setAlias(alias);
        accountQuery.setCategory(category);
        accountQuery.setType(type);
        accountQuery.setLastModifyUser(lastModifyUser);

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(1);
        baseSearchCondition.setOrder(order);
        baseSearchCondition.setOrderField(orderField);

        PageData<AccountDTO> pageData = accountDAO.listPageAccount(accountQuery, baseSearchCondition);

        assertThat(pageData.getTotal()).isEqualTo(2);
        assertThat(pageData.getStart()).isEqualTo(0);
        assertThat(pageData.getPageSize()).isEqualTo(1);
        assertThat(pageData.getData()).hasSize(1);
        AccountDTO returnAccount = pageData.getData().get(0);
        assertThat(returnAccount.getId()).isEqualTo(1L);
        assertThat(returnAccount.getAlias()).isEqualTo("root");
        assertThat(returnAccount.getAccount()).isEqualTo("root");
        assertThat(returnAccount.getCategory()).isEqualTo(AccountCategoryEnum.SYSTEM);
        assertThat(returnAccount.getType()).isEqualTo(AccountTypeEnum.LINUX);
        assertThat(returnAccount.getAppId()).isEqualTo(2L);
        assertThat(returnAccount.getGrantees()).isEqualTo("user1,user2");
        assertThat(returnAccount.getRemark()).isEqualTo("root-linux");
        assertThat(returnAccount.getOs()).isEqualTo("Linux");
        assertThat(returnAccount.getPassword()).isNullOrEmpty();
        assertThat(returnAccount.getDbPassword()).isNullOrEmpty();
        assertThat(returnAccount.getDbPort()).isNull();
        assertThat(returnAccount.getDbSystemAccountId()).isNull();
        assertThat(returnAccount.getCreator()).isEqualTo("admin");
        assertThat(returnAccount.getCreateTime()).isEqualTo(1569550210000L);
        assertThat(returnAccount.getLastModifyUser()).isEqualTo("admin");
        assertThat(returnAccount.getLastModifyTime()).isEqualTo(1569550210000L);
    }

    @Test
    @DisplayName("测试根据别名获取账号信息")
    public void testGetAccountByAlias() {
        long appId = 2L;
        AccountCategoryEnum category = AccountCategoryEnum.SYSTEM;
        String alias = "root-v2";

        AccountDTO returnAccount = accountDAO.getAccount(appId, category, alias);

        assertThat(returnAccount.getId()).isEqualTo(4L);
        assertThat(returnAccount.getAlias()).isEqualTo("root-v2");
        assertThat(returnAccount.getAccount()).isEqualTo("root");
        assertThat(returnAccount.getCategory()).isEqualTo(AccountCategoryEnum.SYSTEM);
        assertThat(returnAccount.getType()).isEqualTo(AccountTypeEnum.LINUX);
        assertThat(returnAccount.getAppId()).isEqualTo(2L);
        assertThat(returnAccount.getGrantees()).isEqualTo("user1,user2");
        assertThat(returnAccount.getRemark()).isEqualTo("root-linux");
        assertThat(returnAccount.getOs()).isEqualTo("Linux");
        assertThat(returnAccount.getPassword()).isNullOrEmpty();
        assertThat(returnAccount.getDbPassword()).isNullOrEmpty();
        assertThat(returnAccount.getDbPort()).isNull();
        assertThat(returnAccount.getDbSystemAccountId()).isNull();
        assertThat(returnAccount.getCreator()).isEqualTo("admin");
        assertThat(returnAccount.getCreateTime()).isEqualTo(1569550210000L);
        assertThat(returnAccount.getLastModifyUser()).isEqualTo("admin");
        assertThat(returnAccount.getLastModifyTime()).isEqualTo(1569636611000L);
    }

    @Test
    @DisplayName("测试获取业务下指定分类的账号")
    public void testListAllAppAccount() {
        List<AccountDTO> accounts = accountDAO.listAllAppAccount(2L, AccountCategoryEnum.SYSTEM, null);
        assertThat(accounts).hasSize(3).extracting("id").containsOnly(1L, 2L, 4L);
    }

    @Test
    @DisplayName("测试系统账号是否被DB账号依赖")
    void testIsAccountRefByDbAccount() {
        long accountId = 1L;
        boolean isRefByDbAccount = accountDAO.isAccountRefByDbAccount(accountId);
        assertThat(isRefByDbAccount).isEqualTo(true);

        accountId = 2L;
        isRefByDbAccount = accountDAO.isAccountRefByDbAccount(accountId);
        assertThat(isRefByDbAccount).isEqualTo(false);
    }

    @Test
    void testBatchUpdateDbAccountPassword() {
        List<AccountDTO> updateAccounts = new ArrayList<>();
        AccountDTO account1 = new AccountDTO();
        account1.setId(3L);
        account1.setDbPassword("ax798sdfs");
        updateAccounts.add(account1);
        accountDAO.batchUpdateDbAccountPassword(updateAccounts);

        AccountDTO account = accountDAO.getAccountById(3L);
        assertThat(account).isNotNull();
        assertThat(account.getId()).isEqualTo(3L);
        assertThat(account.getDbPassword()).isEqualTo("ax798sdfs");
    }

}
