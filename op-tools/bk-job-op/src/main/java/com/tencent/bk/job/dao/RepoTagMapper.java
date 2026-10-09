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

import com.tencent.bk.job.model.RepoTagDTO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RepoTagMapper {

    /**
     * 批量插入Tag记录（忽略已存在的）
     */
    @Insert({
        "<script>",
        "INSERT IGNORE INTO repo_tag (tag, major, minor, patch, pre_type, pre_rank, pre_num, create_time)",
        "VALUES",
        "<foreach collection='tagList' item='item' separator=','>",
        "(#{item.tag}, #{item.major}, #{item.minor}, #{item.patch},",
        "#{item.preType}, #{item.preRank}, #{item.preNum}, #{now})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("tagList") List<RepoTagDTO> tagList, @Param("now") long now);

    /**
     * 批量删除Tag记录
     */
    @Delete({
        "<script>",
        "DELETE FROM repo_tag WHERE tag IN",
        "<foreach collection='tagList' item='tag' open='(' separator=',' close=')'>",
        "#{tag}",
        "</foreach>",
        "</script>"
    })
    int batchDelete(@Param("tagList") List<String> tagList);

    /**
     * 查询给定Tag中已存在于库中的部分
     */
    @Select({
        "<script>",
        "SELECT tag FROM repo_tag WHERE tag IN",
        "<foreach collection='tagList' item='tag' open='(' separator=',' close=')'>",
        "#{tag}",
        "</foreach>",
        "</script>"
    })
    List<String> selectExistingTags(@Param("tagList") List<String> tagList);

    /**
     * 按版本前缀查询Tag
     * <p>
     * 各段版本号是独立的整数列而非字符串前缀匹配，因为LIKE '3.1%'会错误命中3.10.x与3.11.x。
     * 入参为null的段代表不限定，查询条件逐段收窄，始终命中idx_series索引的最左前缀。
     *
     * @param major   主版本号，必填
     * @param minor   小版本号，为null时不限定
     * @param patch   修订号，为null时不限定
     * @param preRank 类型序，为null时不限定
     * @return 命中的Tag列表
     */
    @Select({
        "<script>",
        "SELECT tag, major, minor, patch, pre_type AS preType, pre_rank AS preRank, pre_num AS preNum",
        "FROM repo_tag",
        "WHERE major = #{major}",
        "<if test='minor != null'> AND minor = #{minor} </if>",
        "<if test='patch != null'> AND patch = #{patch} </if>",
        "<if test='preRank != null'> AND pre_rank = #{preRank} </if>",
        "ORDER BY major, minor, patch, pre_rank, pre_num",
        "</script>"
    })
    List<RepoTagDTO> selectSeries(@Param("major") Integer major,
                                  @Param("minor") Integer minor,
                                  @Param("patch") Integer patch,
                                  @Param("preRank") Integer preRank);
}
