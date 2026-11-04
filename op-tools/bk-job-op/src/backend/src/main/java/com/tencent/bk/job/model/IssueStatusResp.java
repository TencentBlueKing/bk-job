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

    /**
     * 状态最后更新时间（毫秒时间戳），由服务端在写入时生成。
     * 查询不存在的Issue时该字段为null，前端展示为“--”。
     */
    private Long updateTime;
}
