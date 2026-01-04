package com.tencent.bk.job.manage.model.web.vo;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("加密信息")
public class EncryptionInfoVO {

    /**
     * Base64公钥
     */
    private String publicKey;

    /**
     * 加密算法
     */
    private String algorithm;
}
