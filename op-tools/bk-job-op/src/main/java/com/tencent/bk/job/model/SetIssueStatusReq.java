package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设置Issue状态时使用的请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetIssueStatusReq {

    /**
     * Issue标识
     */
    private String id;

    /**
     * Issue状态
     */
    private String status;

    /**
     * Issue状态描述
     */
    private String statusDescription;
}
