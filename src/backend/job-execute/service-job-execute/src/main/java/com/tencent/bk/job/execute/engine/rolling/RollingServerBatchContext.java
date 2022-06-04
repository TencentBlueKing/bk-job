package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 主机计算滚动分批上下文
 */
@Getter
@Setter
@ToString
public class RollingServerBatchContext {
    /**
     * 需要分批的服务器
     */
    private List<HostDTO> servers;
    /**
     * 未分批的服务器
     */
    private List<HostDTO> remainedServers;
    /**
     * 需要分批的服务器数量
     */
    private int totalServersSize;
    /**
     * 分批数量
     */
    private int batchCount;
    /**
     * 分批结果
     */
    private List<RollingServerBatch> serverBatches;

    /**
     * Constructor
     *
     * @param servers     所有参与滚动的主机
     */
    public RollingServerBatchContext(List<HostDTO> servers) {
        this.servers = servers;
        this.remainedServers = new ArrayList<>(this.servers);
        this.totalServersSize = servers.size();
        this.serverBatches = new ArrayList<>();
    }

    /**
     * 增加已经解析的分批数量
     */
    public void increaseBatchCount() {
        this.batchCount++;
    }

    /**
     * 是否还有剩余服务器没有被分批
     */
    public boolean hasRemainedServer() {
        return this.remainedServers.size() > 0;
    }

    /**
     * 移除已经解析过的主机
     *
     * @param resolvedServers 已经解析(分批)过的主机
     */
    public void removeResolvedServers(Collection<HostDTO> resolvedServers) {
        this.remainedServers.removeAll(resolvedServers);
    }

    /**
     * 新增一批滚动主机
     *
     * @param rollingServerBatch 一个滚动批次的主机
     */
    public void addServerBatch(RollingServerBatch rollingServerBatch) {
        this.serverBatches.add(rollingServerBatch);
    }
}
