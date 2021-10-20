package com.tencent.bk.job.common.artifactory.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserDetail {
    // 用户Id
    @JsonProperty("uid")
    String userId;
    // 是否管理员
    boolean admin;
    // 是否锁定
    boolean locked;
    // 用户名
    String name;
    // 用户密码
    String pwd;
    // 用户角色
    List<String> roles;
    // 用户Tokens
    List<Token> tokens;
}
