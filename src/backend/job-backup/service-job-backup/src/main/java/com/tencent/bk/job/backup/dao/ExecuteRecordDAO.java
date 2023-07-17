package com.tencent.bk.job.backup.dao;

import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.List;

/**
 * job-execute 微服务的表数据DAO
 *
 * @param <T> 表记录
 */
public interface ExecuteRecordDAO<T extends Record> {

    /**
     * 获取表
     *
     * @return 表
     */
    Table<T> getTable();

    /**
     * 获取用于查询归档记录的ID字段
     *
     * @return ID字段
     */
    TableField<T, Long> getArchiveIdField();

    /**
     * 根据起始/结束ID获取表记录
     *
     * @param start 起始ID(exclude)
     * @param end   结束ID(include)
     * @return 表记录
     */
    List<T> listRecords(Long start, Long end);

    /**
     * 根据起始/结束ID删除表记录
     *
     * @param start 起始ID(exclude)
     * @param end   结束ID(include)
     * @return 删除的记录数量
     */
    int deleteRecords(Long start, Long end);

    /**
     * 获取表中最小归档ID
     *
     * @return id值。如果表中没有数据，那么返回 null
     */
    Long getMinArchiveId();
}
