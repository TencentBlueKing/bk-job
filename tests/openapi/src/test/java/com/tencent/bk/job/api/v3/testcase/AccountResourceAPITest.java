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
import com.tencent.bk.job.api.v3.model.request.EsbUpdateAccountV3Req;
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
class AccountResourceAPITest extends BaseTest {

    private static final List<EsbAccountV3DTO> createdAccountList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
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
        @DisplayName("创建账号异常场景测试")
        void givenInvalidCreateAccountThenFail() {
            EsbCreateAccountV3Req req = new EsbCreateAccountV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCategory(AccountCategoryEnum.SYSTEM.getValue());
            req.setType(AccountTypeEnum.LINUX.getType());
            // 账号名称为空
            req.setAccount(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 账号分类不对
            req.setAccount(TestValueGenerator.generateUniqueStrValue("account", 50));
            req.setCategory(0);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_ACCOUNT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 账号类型不对
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
    }

    @Nested
    class AccountUpdateTest {
        // 更新账号
        @Test
        @DisplayName("测试更新账号信息")
        void testUpdateAccount() {
            EsbAccountV3DTO linuxAccount = Operations.createAccount(null);
            if (linuxAccount == null) {
                return;
            }
            createdAccountList.add(linuxAccount);

            EsbUpdateAccountV3Req req = new EsbUpdateAccountV3Req();
            req.setId(linuxAccount.getId());
            req.setScopeId(linuxAccount.getScopeId());
            req.setScopeType(linuxAccount.getScopeType());
            req.setAlias(TestValueGenerator.generateUniqueStrValue("update_alias", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.UPDATE_ACCOUNT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", equalTo(req.getId().intValue()));

            req.setAlias(null);
            req.setRemark(TestValueGenerator.generateUniqueStrValue("update_remark", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_ACCOUNT)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId().intValue()));

            EsbAccountV3DTO windowsAccount = Operations.createAccount(AccountTypeEnum.WINDOW);
            if (windowsAccount == null) {
                return;
            }
            createdAccountList.add(windowsAccount);
            req = new EsbUpdateAccountV3Req();
            req.setId(linuxAccount.getId());
            req.setScopeId(linuxAccount.getScopeId());
            req.setScopeType(linuxAccount.getScopeType());
            req.setPassword(TestValueGenerator.generateUniqueStrValue("update_pwd", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_ACCOUNT)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId().intValue()));
        }
    }

    @Nested
    class AccountDeleteTest {
        @Test
        @DisplayName("测试账号删除")
        void testDeleteAccount() {
            EsbAccountV3DTO createdScript = Operations.createAccount(null);
            EsbDeleteAccountV3Req req = new EsbDeleteAccountV3Req();
            req.setScopeId(createdScript.getScopeId());
            req.setScopeType(createdScript.getScopeType());
            req.setId(createdScript.getId());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_ACCOUNT)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }
}
