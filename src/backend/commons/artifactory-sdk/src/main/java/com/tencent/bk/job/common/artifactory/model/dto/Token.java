package com.tencent.bk.job.common.artifactory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Token {
    // token id
    String id;
    // token创建时间
    String createdAt;
    // token失效时间
    String expiredAt;
}
