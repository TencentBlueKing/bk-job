package com.tencent.bk.job.common.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * host状态数量统计
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class HostStatusNumStatisticsDTO {
    public static final String KEY_AGENT_ALIVE = "gseAgentAlive";
    public static final String KEY_HOST_NUM = "hostNum";

    /**
     * 主机Agent状态
     */
    private int gseAgentAlive;

    /**
     * 主机数量
     */
    private int hostNum;

}
