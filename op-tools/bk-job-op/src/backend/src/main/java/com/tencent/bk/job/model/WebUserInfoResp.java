package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OP Web页面当前登录用户信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebUserInfoResp {

    /**
     * 当前登录用户名
     */
    private String username;
}
