package com.tencent.bk.job.execute.engine.rolling;

import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 执行对象计算滚动分批上下文
 */
@Getter
@Setter
@ToString
public class RollingExecuteObjectBatchContext {
    /**
     * 需要分批的执行对象
     */
    private List<ExecuteObject> executeObjects;
    /**
     * 未分批的执行对象
     */
    private List<ExecuteObject> remainedExecuteObjects;
    /**
     * 需要分批的执行对象数量
     */
    private int totalExecuteObjectSize;
    /**
     * 分批数量
     */
    private int batchCount;
    /**
     * 分批结果
     */
    private List<RollingExecuteObjectBatch> executeObjectBatches;

    /**
     * Constructor
     *
     * @param executeObjects 所有参与滚动的执行对象
     */
    public RollingExecuteObjectBatchContext(List<ExecuteObject> executeObjects) {
        this.executeObjects = executeObjects;
        this.remainedExecuteObjects = new ArrayList<>(this.executeObjects);
        this.totalExecuteObjectSize = executeObjects.size();
        this.executeObjectBatches = new ArrayList<>();
    }

    /**
     * 增加已经解析的分批数量
     */
    public void increaseBatchCount() {
        this.batchCount++;
    }

    /**
     * 是否还有剩余执行对象没有被分批
     */
    public boolean hasRemainedExecuteObject() {
        return this.remainedExecuteObjects.size() > 0;
    }

    /**
     * 移除已经解析过的执行对象
     *
     * @param resolvedExecuteObjects 已经解析(分批)过的执行对象
     */
    public void removeResolvedServers(Collection<ExecuteObject> resolvedExecuteObjects) {
        if (resolvedExecuteObjects instanceof Set) {
            this.remainedExecuteObjects.removeAll(resolvedExecuteObjects);
        } else {
            // 非 Set 集合，List.removeAll() 的实现有性能瓶颈
            this.remainedExecuteObjects.removeAll(new HashSet<>(resolvedExecuteObjects));
        }
    }

    /**
     * 新增一批滚动主机
     *
     * @param rollingExecuteObjectBatch 一个滚动批次的主机
     */
    public void addExecuteObjectBatch(RollingExecuteObjectBatch rollingExecuteObjectBatch) {
        this.executeObjectBatches.add(rollingExecuteObjectBatch);
    }
}
