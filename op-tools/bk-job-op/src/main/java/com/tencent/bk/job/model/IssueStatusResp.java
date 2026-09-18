package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class IssueStatusResp {
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
