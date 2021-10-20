package com.tencent.bk.job.manage.model.web.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("生成上传目标请求报文")
public class GenUploadTargetReq {

    @ApiModelProperty(value = "文件名列表", required = true)
    private List<String> fileNameList;

}
