package com.tencent.bk.job.common.esb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.job.common.esb.model.OpenApiError;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OpenApiErrorDeserializerTest {
    @Test
    public void testDeserializeWithoutDataField() {
        String errorStr = "{\"message\": \"invalid params\", \"code\": \"INVALID_ARGUMENT\"}";
        OpenApiError error = JsonUtils.fromJson(
            errorStr,
            new TypeReference<OpenApiError>() {
            }
        );
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(error.getMessage()).isEqualTo("invalid params");
    }

    @Test
    public void testDeserializeIamNoPermissionError() {
        String errorStr = "{\n" +
            "    \"code\": \"IAM_NO_PERMISSION\",\n" +
            "    \"message\": \"User no permission\",\n" +
            "    \"data\":\n" +
            "    {\n" +
            "        \"system_id\": \"bk-job\",\n" +
            "        \"system_name\": \"bk-job\",\n" +
            "        \"actions\":\n" +
            "        [\n" +
            "            {\n" +
            "                \"id\": \"execute_plan\",\n" +
            "                \"related_resource_types\":\n" +
            "                [\n" +
            "                    {\n" +
            "                        \"system_id\": \"bk-job\",\n" +
            "                        \"system_name\": \"bk-job\",\n" +
            "                        \"type\": \"plan\",\n" +
            "                        \"instances\":\n" +
            "                        [\n" +
            "                            [\n" +
            "                                {\n" +
            "                                    \"id\": \"1\",\n" +
            "                                    \"type\": \"plan\",\n" +
            "                                    \"name\": \"job_plan_test\"\n" +
            "                                }\n" +
            "                            ]\n" +
            "                        ]\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";
        OpenApiError error = JsonUtils.fromJson(
            errorStr,
            new TypeReference<OpenApiError>() {
            }
        );
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo("IAM_NO_PERMISSION");
        assertThat(error.getMessage()).isEqualTo("User no permission");
        assertThat(error.getData()).isInstanceOf(OpenApiApplyPermissionDTO.class);
        OpenApiApplyPermissionDTO permissionData = (OpenApiApplyPermissionDTO) error.getData();
        assertThat(permissionData.getSystemId()).isEqualTo("bk-job");
        assertThat(permissionData.getSystemName()).isEqualTo("bk-job");
        assertThat(permissionData.getActions().size()).isEqualTo(1);
        assertThat(permissionData.getActions().get(0).getId()).isEqualTo("execute_plan");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().size()).isEqualTo(1);
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getSystemId()).isEqualTo("bk" +
                "-job");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getSystemName()).isEqualTo("bk" +
                "-job");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getType()).isEqualTo("plan");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getInstance().size()).isEqualTo(1);
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getInstance().get(0).get(0).getId()).isEqualTo("1");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getInstance().get(0).get(0).getType()).isEqualTo("plan");
        assertThat(permissionData.getActions().get(0).getRelatedResourceTypes().get(0).getInstance().get(0).get(0).getName()).isEqualTo("job_plan_test");
    }

    @Test
    public void testDeserializeWithData() {
        String errorStr = "{\n" +
            "    \"code\": \"INVALID_ARGUMENT\",\n" +
            "    \"message\": \"Invalid param\",\n" +
            "    \"data\":\n" +
            "    [\n" +
            "        {\n" +
            "            \"paramName\": \"name\",\n" +
            "            \"reason\": \"missing\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";
        OpenApiError error = JsonUtils.fromJson(
            errorStr,
            new TypeReference<OpenApiError>() {
            }
        );
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo("INVALID_ARGUMENT");
        assertThat(error.getMessage()).isEqualTo("Invalid param");
        assertThat(error.getData()).isInstanceOf(JsonNode.class);
        JsonNode dataNode = (JsonNode) error.getData();
        assertThat(dataNode.isArray()).isTrue();
        assertThat(dataNode.get(0).isObject()).isTrue();
        assertThat(dataNode.get(0).get("paramName").asText()).isEqualTo("name");
        assertThat(dataNode.get(0).get("reason").asText()).isEqualTo("missing");
    }

}
