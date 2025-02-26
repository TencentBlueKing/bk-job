package com.tencent.bk.job.common.sharding.mysql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DataNodesParser {

    /**
     * 参数名 - 数据源分组名称
     */
    private static final String FIELD_DATA_SOURCE_GROUP_NAME = "ds_group";

    /**
     * 参数名 - db 分片节点个数
     */
    private static final String FIELD_DB_NODE_COUNT = "db_node_count";

    /**
     * 参数名 - 每个 db 上的表分片节点个数
     */
    private static final String FIELD_TB_NODE_COUNT = "tb_node_count";

    public static List<DataSourceGroupShardingNode> parseDataNodes(String dataNodes) {
        List<DataSourceGroupShardingNode> dataSourceGroupShardingNodes = new ArrayList<>();
        try {
            String[] dataSourceGroups = dataNodes.split(";");
            for (String dataSourceGroup : dataSourceGroups) {
                String dsGroupName = null;
                int dbNodeCount = 0;
                int tbNodeCount = 0;
                String[] shardingParams = dataSourceGroup.split(",");
                for (String param : shardingParams) {
                    String[] fieldNameAndValue = param.split("=");
                    String fieldName = fieldNameAndValue[0].trim();
                    String fieldValue = fieldNameAndValue[1].trim();
                    switch (fieldName) {
                        case FIELD_DATA_SOURCE_GROUP_NAME:
                            dsGroupName = fieldValue;
                            break;
                        case FIELD_DB_NODE_COUNT:
                            dbNodeCount = Integer.parseInt(fieldValue);
                            break;
                        case FIELD_TB_NODE_COUNT:
                            tbNodeCount = Integer.parseInt(fieldValue);
                            break;
                        default:
                            log.error("Invalid dataNodes param : {}", param);
                            throw new IllegalArgumentException("Invalid dataNodes param : " + param);
                    }
                }
                DataSourceGroupShardingNode dataSourceGroupShardingNode =
                    new DataSourceGroupShardingNode(dsGroupName, dbNodeCount, tbNodeCount);
                if (!dataSourceGroupShardingNode.validate()) {
                    throw new IllegalArgumentException("Invalid dataNodes : " + dataNodes);
                }
                dataSourceGroupShardingNodes.add(dataSourceGroupShardingNode);
            }

            if (CollectionUtils.isEmpty(dataSourceGroupShardingNodes)) {
                log.error("Empty data nodes");
                throw new IllegalArgumentException("Empty data nodes");
            }
            return dataSourceGroupShardingNodes;
        } catch (Throwable e) {
            log.error("Parse data nodes error, dataNodes: {}", dataNodes);
            throw new IllegalArgumentException("Parse data nodes error", e);
        }
    }
}
