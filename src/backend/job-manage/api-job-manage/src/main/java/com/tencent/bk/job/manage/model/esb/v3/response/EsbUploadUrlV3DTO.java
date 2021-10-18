package com.tencent.bk.job.manage.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("上传地址信息")
public class EsbUploadUrlV3DTO {

    @JsonProperty("url_map")
    private Map<String, Map<String, String>> urlMap;

}
