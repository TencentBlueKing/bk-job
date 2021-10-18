package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("生成本地文件上传URL请求报文")
public class EsbGenLocalFileUploadUrlV3Req extends EsbReq {

    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 文件名列表
     */
    @JsonProperty("file_name_list")
    private List<String> fileNameList;

}
