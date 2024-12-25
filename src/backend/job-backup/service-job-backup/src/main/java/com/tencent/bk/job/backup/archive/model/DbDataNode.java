/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.backup.archive.model;

import com.tencent.bk.job.backup.constant.DbDataNodeTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * db 数据节点信息
 */
@Data
@NoArgsConstructor
public class DbDataNode {
    /**
     * db 数据节点类型
     */
    private DbDataNodeTypeEnum type;
    /**
     * 分库分表数据源
     */
    private String dataSource;
    /**
     * 分库分表 db 实例位置索引，从 0 开始
     */
    private Integer dbIndex;

    /**
     * 分库分表表位置索引，从 0 开始
     */
    private Integer tableIndex;

    public static final String STANDALONE_DS_NAME = "ds_standalone";

    public DbDataNode(DbDataNodeTypeEnum type, String dataSource, Integer dbIndex, Integer tableIndex) {
        this.type = type;
        this.dataSource = dataSource;
        this.dbIndex = dbIndex;
        this.tableIndex = tableIndex;
    }

    public String toDataNodeId() {
        switch (type) {
            case STANDALONE:
                return type.getValue() + ":" + STANDALONE_DS_NAME;
            case SHARDING:
                return type.getValue() + ":" + dataSource + ":" + dbIndex + ":" + tableIndex;
            default:
                throw new IllegalArgumentException("Invalid DbDataNodeTypeEnum");
        }
    }

    public String toDbNodeId() {
        switch (type) {
            case STANDALONE:
                return STANDALONE_DS_NAME;
            case SHARDING:
                return dataSource + ":" + dbIndex;
            default:
                throw new IllegalArgumentException("Invalid DbDataNodeTypeEnum");
        }
    }

    public static DbDataNode fromDataNodeId(String dataNodeId) {
        String[] dataNodeParts = dataNodeId.split(":");
        DbDataNodeTypeEnum dbDataNodeType = DbDataNodeTypeEnum.valOf(Integer.parseInt(dataNodeParts[0]));
        switch (dbDataNodeType) {
            case STANDALONE:
                return new DbDataNode(dbDataNodeType, STANDALONE_DS_NAME, null, null);
            case SHARDING:
                return new DbDataNode(
                    dbDataNodeType,
                    dataNodeParts[1],
                    Integer.parseInt(dataNodeParts[2]),
                    Integer.parseInt(dataNodeParts[3])
                );
            default:
                throw new IllegalArgumentException("Invalid DbDataNodeId");
        }
    }

    public static DbDataNode standaloneDbDataNode() {
        return new DbDataNode(DbDataNodeTypeEnum.STANDALONE, STANDALONE_DS_NAME, null, null);
    }

    public static DbDataNode shardingDbDataNode(String dataSource, Integer dbIndex, Integer tableIndex) {
        return new DbDataNode(DbDataNodeTypeEnum.SHARDING, dataSource, dbIndex, tableIndex);
    }

    @Override
    public DbDataNode clone() {
        return new DbDataNode(type, dataSource, dbIndex, tableIndex);
    }
}
