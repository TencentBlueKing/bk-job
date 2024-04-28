package com.tencent.bk.job.api.util;

import com.tencent.bk.job.api.constant.CredentialTypeEnum;
import com.tencent.bk.job.api.constant.FileSourceTypeEnum;
import com.tencent.bk.job.api.constant.HighRiskGrammarActionEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbAccountV3BasicDTO;
import com.tencent.bk.job.api.v3.model.EsbCredentialSimpleInfoV3DTO;
import com.tencent.bk.job.api.v3.model.EsbCronInfoV3DTO;
import com.tencent.bk.job.api.v3.model.EsbDangerousRuleV3DTO;
import com.tencent.bk.job.api.v3.model.EsbFileSourceSimpleInfoV3DTO;
import com.tencent.bk.job.api.v3.model.EsbFileSourceV3DTO;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.EsbPageDataV3;
import com.tencent.bk.job.api.v3.model.EsbPlanBasicInfoV3DTO;
import com.tencent.bk.job.api.v3.model.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateDangerousRuleV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreateOrUpdateCredentialV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreatePublicScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreateScriptV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteCronV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbDeletePublicScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeletePublicScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbExecuteJobV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbFastExecuteScriptV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbFastTransferFileV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbGetPlanListV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbManageDangerousRuleV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbSaveCronV3Request;
import io.restassured.common.mapper.TypeRef;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tencent.bk.job.api.constant.Constant.SHELL_SCRIPT_CONTENT_BASE64;
import static io.restassured.RestAssured.given;

/**
 * 常用操作
 */
public class Operations {

    public static EsbScriptVersionDetailV3DTO createScript() {
        EsbCreateScriptV3Request req = new EsbCreateScriptV3Request();
        req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
        req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
        req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
        req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
        req.setType(ScriptTypeEnum.SHELL.getValue());
        req.setVersion("v1");
        req.setVersionDesc("v1_desc");

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.CREATE_SCRIPT)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
            })
            .getData();
    }

    public static EsbScriptVersionDetailV3DTO createPublicScript() {
        EsbCreatePublicScriptV3Req req = new EsbCreatePublicScriptV3Req();
        req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
        req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
        req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
        req.setType(ScriptTypeEnum.SHELL.getValue());
        req.setVersion("v1");
        req.setVersionDesc("v1_desc");

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
            })
            .getData();
    }

    public static void deleteScript(EsbDeleteScriptV3Req req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_SCRIPT)
            .then()
            .statusCode(200);
    }

    public static void deleteScriptVersion(EsbDeleteScriptVersionV3Req req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_SCRIPT_VERSION)
            .then()
            .statusCode(200);
    }

    public static void deletePublicScript(EsbDeletePublicScriptV3Req req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_PUBLIC_SCRIPT)
            .then()
            .statusCode(200);
    }

    public static void deletePublicScriptVersion(EsbDeletePublicScriptVersionV3Req req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_PUBLIC_SCRIPT_VERSION)
            .then()
            .statusCode(200);
    }

    public static void deleteDangerousRule(EsbManageDangerousRuleV3Req req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_DANGEROUS_RULE)
            .then()
            .statusCode(200);
    }

    public static EsbDangerousRuleV3DTO createDangerousRule() {
        EsbCreateDangerousRuleV3Req req = new EsbCreateDangerousRuleV3Req();
        req.setExpression("rm ");
        req.setDescription(TestValueGenerator.generateUniqueStrValue("dangerous_rule_desc", 50));
        req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SHELL.getValue().byteValue()));
        req.setAction(HighRiskGrammarActionEnum.SCAN.getCode());
        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.CREATE_DANGEROUS_RULE)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbDangerousRuleV3DTO>>() {
            })
            .getData();
    }

    public static EsbJobExecuteV3DTO fastExecuteScriptTask() {
        EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();

        req.setBizId(TestProps.DEFAULT_BIZ);
        req.setContent("IyEvYmluL2Jhc2gKZWNobyAnMTIzJwo=");
        req.setScriptLanguage(1);
        req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
        req.setName(buildJobName());
        EsbServerV3DTO targetServer = new EsbServerV3DTO();
        List<HostDTO> ips = new ArrayList<>();
        ips.add(TestProps.HOST_1_DEFAULT_BIZ);
        ips.add(TestProps.HOST_2_DEFAULT_BIZ);
        targetServer.setIps(ips);
        req.setTargetServer(targetServer);

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
            .then()
            .extract()
            .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
            })
            .getData();
    }

    public static EsbJobExecuteV3DTO fastTransferFileTask() {
        EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();

        req.setBizId(TestProps.DEFAULT_BIZ);
        req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
        req.setTargetPath("/tmp/");

        req.setFileSources(buildCommonFileSource());
        req.setTargetServer(buildCommonTargetServer());
        req.setName(buildJobName());

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.FAST_TRANSFER_FILE)
            .then()
            .extract()
            .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
            })
            .getData();
    }

    private static List<EsbFileSourceV3DTO> buildCommonFileSource() {
        List<EsbFileSourceV3DTO> fileSources = new ArrayList<>();
        EsbFileSourceV3DTO fileSource = new EsbFileSourceV3DTO();
        fileSource.setAccount(buildCommonFileSourceAccount());
        List<String> files = new ArrayList<>();
        files.add("/tmp/1.log");
        fileSource.setFiles(files);
        fileSource.setServer(buildCommonSourceServer());
        fileSources.add(fileSource);
        return fileSources;
    }

    private static EsbAccountV3BasicDTO buildCommonFileSourceAccount() {
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
        return account;
    }

    private static EsbServerV3DTO buildCommonSourceServer() {
        EsbServerV3DTO sourceServer = new EsbServerV3DTO();
        List<HostDTO> sourceIps = new ArrayList<>();
        sourceIps.add(TestProps.HOST_1_DEFAULT_BIZ);
        sourceServer.setIps(sourceIps);
        return sourceServer;
    }

    private static EsbServerV3DTO buildCommonTargetServer() {
        EsbServerV3DTO targetServer = new EsbServerV3DTO();
        List<HostDTO> sourceIps = new ArrayList<>();
        sourceIps.add(TestProps.HOST_2_DEFAULT_BIZ);
        targetServer.setIps(sourceIps);
        return targetServer;
    }

    public static EsbJobExecuteV3DTO fastExecuteFailScriptTask() {
        EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();

        req.setBizId(TestProps.DEFAULT_BIZ);
        // #!/bin/bash
        // exit 1
        req.setContent("IyEvYmluL2Jhc2gKZXhpdCAxCg==");
        req.setScriptLanguage(1);
        req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
        req.setName(buildJobName());
        EsbServerV3DTO targetServer = new EsbServerV3DTO();
        List<HostDTO> ips = new ArrayList<>();
        ips.add(TestProps.HOST_1_DEFAULT_BIZ);
        targetServer.setIps(ips);
        req.setTargetServer(targetServer);

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
            .then()
            .extract()
            .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
            })
            .getData();
    }

    public static EsbJobExecuteV3DTO fastExecuteLongTimeScriptTask() {
        EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();

        req.setBizId(TestProps.DEFAULT_BIZ);
        // #!/bin/bash
        // sleep 100
        req.setContent("IyEvYmluL2Jhc2gKc2xlZXAgMTAwCg==");
        req.setScriptLanguage(1);
        req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
        req.setName("api-test-long-time-task." + DateUtils.formatLocalDateTime(LocalDateTime.now(),
            "yyyyMMddhhmmssSSS"));
        EsbServerV3DTO targetServer = new EsbServerV3DTO();
        List<HostDTO> ips = new ArrayList<>();
        ips.add(TestProps.HOST_1_DEFAULT_BIZ);
        targetServer.setIps(ips);
        req.setTargetServer(targetServer);

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
            .then()
            .extract()
            .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
            })
            .getData();
    }

    public static EsbJobExecuteV3DTO executePlanTask() {
        EsbExecuteJobV3Request request = new EsbExecuteJobV3Request();
        request.setAppId(TestProps.DEFAULT_BIZ);
//        request.setTaskId(TASK_PLAN_ID);

        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(request))
            .post(APIV3Urls.EXECUTE_JOB_PLAN)
            .then()
            .extract()
            .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
            })
            .getData();
    }

    public static EsbCronInfoV3DTO createCron() {
        Long planId = getTaskPlanId();
        if (planId == null) {
            return null;
        }
        EsbSaveCronV3Request req = new EsbSaveCronV3Request();
        req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
        req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        req.setName(TestValueGenerator.generateUniqueStrValue("cron_task", 50));
        req.setCronExpression("* * * * *");
        req.setPlanId(planId);
        return given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.SAVE_CRON)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbCronInfoV3DTO>>() {
            })
            .getData();
    }

    public static void deleteCron(EsbDeleteCronV3Request req) {
        given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.DELETE_CRON)
            .then()
            .statusCode(200);
    }

    public static Long getTaskPlanId() {
        EsbGetPlanListV3Request req = new EsbGetPlanListV3Request();
        req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
        req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        req.setLength(1);
        EsbPageDataV3<EsbPlanBasicInfoV3DTO> data = given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.GET_JOB_PLAN_LIST)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbPageDataV3<EsbPlanBasicInfoV3DTO>>>() {
            })
            .getData();
        List<EsbPlanBasicInfoV3DTO> planList = data.getData();
        if (planList != null && planList.size() > 0) {
            EsbPlanBasicInfoV3DTO planBasicInfoV3DTO = planList.get(0);
            return planBasicInfoV3DTO.getId();
        }
        return null;
    }

    public static EsbFileSourceSimpleInfoV3DTO createFileSource() {
        EsbCreateOrUpdateFileSourceV3Req req = new EsbCreateOrUpdateFileSourceV3Req();
        req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
        req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        req.setCode(TestValueGenerator.generateUniqueStrValue("file_source_code", 50));
        req.setAlias(TestValueGenerator.generateUniqueStrValue("file_source_alias", 50));
        req.setType(FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name());
        Map<String, Object> accessParams = new HashMap<>();
        accessParams.put("base_url", "https://bkrepo.com");
        req.setAccessParams(accessParams);
        req.setCredentialId(TestValueGenerator.generateUniqueStrValue("credential_id", 50));
        EsbFileSourceSimpleInfoV3DTO fileSourceSimpleInfoV3DTO = given()
            .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
            .body(JsonUtil.toJson(req))
            .post(APIV3Urls.CREATE_FILE_SOURCE)
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(new TypeRef<EsbResp<EsbFileSourceSimpleInfoV3DTO>>() {
            })
            .getData();
        fileSourceSimpleInfoV3DTO.setCode(req.getCode());
        return fileSourceSimpleInfoV3DTO;
    }

    public static EsbCredentialSimpleInfoV3DTO createCredential() {
        EsbCreateOrUpdateCredentialV3Req req = new EsbCreateOrUpdateCredentialV3Req();
        req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
        req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
        req.setName(TestValueGenerator.generateUniqueStrValue("credential_name", 50));
        req.setDescription(TestValueGenerator.generateUniqueStrValue("credential_desc", 50));
        req.setType(CredentialTypeEnum.USERNAME_PASSWORD.name());
        req.setCredentialUsername("admin");
        req.setCredentialPassword("password");
        return given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_CREDENTIAL)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbCredentialSimpleInfoV3DTO>>() {
                })
                .getData();
    }

    private static String buildJobName() {
        return "api.test." + DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyyMMddhhmmssSSS");
    }
}
