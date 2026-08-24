package com.tencent.bk.job.manage.model.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "生成上传目标请求报文")
public class GenUploadTargetReq {

    @Schema(description = "文件名列表", required = true)
    private List<String> fileNameList;

}
