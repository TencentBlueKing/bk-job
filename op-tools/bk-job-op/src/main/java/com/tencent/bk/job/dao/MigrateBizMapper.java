/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.dao;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MigrateBizMapper {

    /**
     * 批量插入biz记录（忽略已存在的）
     */
    @Insert({
        "<script>",
        "INSERT IGNORE INTO migrate_biz (biz_id, create_time, row_create_time, row_update_time)",
        "VALUES",
        "<foreach collection='bizIdList' item='bizId' separator=','>",
        "(#{bizId}, #{now}, NOW(), NOW())",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("bizIdList") List<String> bizIdList, @Param("now") long now);

    /**
     * 批量删除biz记录
     */
    @Delete({
        "<script>",
        "DELETE FROM migrate_biz WHERE biz_id IN",
        "<foreach collection='bizIdList' item='bizId' open='(' separator=',' close=')'>",
        "#{bizId}",
        "</foreach>",
        "</script>"
    })
    int batchDelete(@Param("bizIdList") List<String> bizIdList);

    /**
     * 查询某个bizId是否存在
     */
    @Select("SELECT COUNT(1) FROM migrate_biz WHERE biz_id = #{bizId}")
    int countByBizId(@Param("bizId") String bizId);
}
