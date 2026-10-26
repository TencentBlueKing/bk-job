package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除Issue状态时使用的请求体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteIssueStatusReq {

    /**
     * Issue标识
     */
    private String id;
}
