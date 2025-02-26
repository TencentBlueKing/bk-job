package com.tencent.bk.job.common.sharding.mysql.algorithm;

import com.tencent.bk.job.common.sharding.mysql.DataNodesParser;
import com.tencent.bk.job.common.sharding.mysql.DataSourceGroupShardingNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Properties;

@Slf4j
public class ShardingAlgorithmBase {

    /**
     * 属性名称 - 数据节点
     */
    private static final String PROP_DATA_NODES = "dataNodes";

    protected List<DataSourceGroupShardingNode> parseDataNodes(Properties props) {
        if (!props.containsKey(PROP_DATA_NODES)) {
            log.error("Empty sharding algorithm init prop {}", PROP_DATA_NODES);
            throw new IllegalArgumentException("Init prop : " + PROP_DATA_NODES + " is required");
        }

        String dataNodes = props.getProperty(PROP_DATA_NODES);
        return DataNodesParser.parseDataNodes(dataNodes);
    }
}
