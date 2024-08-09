package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.AccountCategoryEnum;
import com.tencent.bk.job.api.constant.AccountTypeEnum;
import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbAccountV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateAccountV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteAccountV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetAccountListV3Req;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 账号管理 API 测试
 */
@DisplayName("v3.AccountResourceAPITest")
class EsbAccountV3ResourceAPITest extends BaseTest {

    private static final List<EsbAccountV3DTO> createdAccountList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        if (CollectionUtils.isNotEmpty(createdAccountList)) {
            createdAccountList.forEach(Account -> {
                EsbDeleteAccountV3Req req = new EsbDeleteAccountV3Req();
                req.setScopeId(Account.getScopeId());
                req.setScopeType(Account.getScopeType());
                req.setId(Account.getId());
                Operations.deleteAccount(req);
            });
        }
    }

    @Nested
    class AccountCreateTest {
        @Test
        @DisplayName("测试windows账号正常创建")
        void testCreateWindowsAccount() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccount(TestValueGenerator.generateUniqueStrValue("account", 50));
            req.setAlias(TestValueGenerator.generateUniqueStrValue("alias", 50));
            req.setRemark(TestValueGenerator.generateUniqueStrValue("remark", 50));
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.WINDOW.getType());
            req.setPassword(TestValueGenerator.generateUniqueStrValue("password", 50));

            EsbAccountV3DTO createdAccount =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_ACCOUNT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.account", equalTo(req.getAccount()))
                    .body("data.alias", equalTo(req.getAlias()))
                    .body("data.category", equalTo(req.getCategory().intValue()))
                    .body("data.type", equalTo(req.getType().intValue()))
                    .body("data.os", equalTo("Windows"))
                    .body("data.description", equalTo(req.getRemark()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", notNullValue())
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbAccountV3DTO>>() {
                    })
                    .getData();
            createdAccountList.add(createdAccount);
        }

        @Test
        @DisplayName("测试Linux账号正常创建")
        void testCreateLinuxAccount() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccount(TestValueGenerator.generateUniqueStrValue("account", 50));
            req.setAlias(TestValueGenerator.generateUniqueStrValue("alias", 50));
            req.setRemark(TestValueGenerator.generateUniqueStrValue("remark", 50));
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.LINUX.getType());

            EsbAccountV3DTO createdAccount =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_ACCOUNT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.account", equalTo(req.getAccount()))
                    .body("data.alias", equalTo(req.getAlias()))
                    .body("data.category", equalTo(req.getCategory().intValue()))
                    .body("data.type", equalTo(req.getType().intValue()))
                    .body("data.os", equalTo("Linux"))
                    .body("data.description", equalTo(req.getRemark()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", notNullValue())
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbAccountV3DTO>>() {
                    })
                    .getData();
            createdAccountList.add(createdAccount);
        }

        @Test
        @DisplayName("测试创建账号-账号异常")
        void testCreateAccountWithWrongAccount() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.LINUX.getType());
            req.setAccount(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试创建账号-分类异常")
        void testCreateAccountWithWrongCategory() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.LINUX.getType());
            req.setAccount(TestValueGenerator.generateUniqueStrValue("account", 50));
            req.setCategory(0);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试创建账号-账号类型异常")
        void testCreateAccountWithWrongType() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.LINUX.getType());
            req.setAccount(TestValueGenerator.generateUniqueStrValue("account", 50));
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(0);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    class AccountGetTest {
        @Test
        @DisplayName("测试获取账号列表")
        void testGetAccountList() {
            EsbAccountV3DTO createdAccount = Operations.createAccount(null);
            if (createdAccount == null) {
                return;
            }
            createdAccountList.add(createdAccount);
            EsbGetAccountListV3Req req = new EsbGetAccountListV3Req();
            req.setScopeId(createdAccount.getScopeId());
            req.setScopeType(createdAccount.getScopeType());
            req.setAccount(req.getAccount());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_ACCOUNT_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0));
            req.setAccount(null);
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_ACCOUNT_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0));
        }

        @Test
        @DisplayName("测试获取账号列表-账号分类异常")
        void testGetAccountListWithWrongCategory() {
            EsbAccountV3DTO createdAccount = Operations.createAccount(null);
            if (createdAccount == null) {
                return;
            }
            createdAccountList.add(createdAccount);
            EsbGetAccountListV3Req req = new EsbGetAccountListV3Req();
            req.setScopeId(createdAccount.getScopeId());
            req.setScopeType(createdAccount.getScopeType());
            req.setCategory(0);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_ACCOUNT_LIST)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    class AccountDeleteTest {
        @Test
        @DisplayName("测试账号删除")
        void testDeleteAccount() {
            EsbAccountV3DTO createdAccount = Operations.createAccount(null);
            EsbDeleteAccountV3Req req = new EsbDeleteAccountV3Req();
            req.setScopeId(createdAccount.getScopeId());
            req.setScopeType(createdAccount.getScopeType());
            req.setId(createdAccount.getId());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_ACCOUNT)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }

        @Test
        @DisplayName("测试账号删除-账号ID异常")
        void testDeleteAccountWithWrongId() {
            EsbAccountV3DTO createdAccount = Operations.createAccount(null);
            createdAccountList.add(createdAccount);
            EsbDeleteAccountV3Req req = new EsbDeleteAccountV3Req();
            req.setScopeId(createdAccount.getScopeId());
            req.setScopeType(createdAccount.getScopeType());
            req.setId(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
