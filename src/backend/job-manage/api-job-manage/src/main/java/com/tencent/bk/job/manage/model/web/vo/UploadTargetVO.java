package com.tencent.bk.job.manage.model.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "上传目标地址信息")
public class UploadTargetVO {

    @Schema(description = "带Token的上传地址列表，有效期为半小时，使用次数为1次，上传失败后需要重新获取该url重新上传")
    private List<String> urlList;

}
