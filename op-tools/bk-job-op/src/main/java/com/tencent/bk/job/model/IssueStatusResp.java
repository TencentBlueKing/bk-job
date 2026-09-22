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
     * 该字段为后续新增，此前写入的存量数据中不存在，反序列化后为null，前端展示为“--”，
     * 随Redis中30天的TTL自然收敛。
     */
    private Long updateTime;
}
