package com.tencent.bk.job.manage.model.web.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "加密信息")
public class EncryptionInfoVO {

    /**
     * PEM格式的公钥
     */
    private String pemPublicKey;

    /**
     * 加密算法
     */
    private String algorithm;
}
