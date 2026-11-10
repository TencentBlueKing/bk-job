package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除Issue状态响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteIssueStatusResp {

    /**
     * 实际删除行数
     */
    private int deleted;
}
