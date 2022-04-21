package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.common.model.dto.IpDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 滚动表达式解析上下文
 */
@Getter
@Setter
@ToString
public class RollingExprResolveContext {
    /**
     * 需要分批的服务器
     */
    private List<IpDTO> servers;
    /**
     * 未分批的服务器
     */
    private List<IpDTO> remainedServers;
    /**
     * 滚动策略表达式
     */
    private String rollingExpr;
    /**
     * 需要分批的服务器数量
     */
    private int total;
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
     * @param rollingExpr 滚动表达式
     */
    public RollingExprResolveContext(List<IpDTO> servers, String rollingExpr) {
        this.servers = servers;
        this.remainedServers = new ArrayList<>(this.servers);
        this.rollingExpr = rollingExpr;
        this.total = servers.size();
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
    public void removeResolvedServers(Collection<IpDTO> resolvedServers) {
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
