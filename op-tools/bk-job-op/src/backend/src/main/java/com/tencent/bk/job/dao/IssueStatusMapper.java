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

import com.tencent.bk.job.model.IssueStatusDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Issue状态 Mapper。
 * 当前工程未开启 MyBatis mapUnderscoreToCamelCase，SELECT 必须使用列别名映射到 Java camelCase 属性。
 */
@Mapper
public interface IssueStatusMapper {

    String SELECT_COLUMNS = "`id` AS id, "
        + "`issue_no` AS issueNo, "
        + "`status` AS status, "
        + "`status_description` AS statusDescription, "
        + "`update_time` AS updateTime";

    /**
     * 列表排序：能解析出编号的按编号降序在前，解析不出编号的按id升序排在后面。
     * 三段排序键保证全序，翻页时不会出现记录重复或遗漏。
     */
    String ORDER_BY = "ORDER BY (`issue_no` IS NULL) ASC, `issue_no` DESC, `id` ASC";

    /**
     * 关键字为空时不加过滤条件。keyword中的 % _ \ 已由调用方转义，这里显式声明转义符，
     * 避免调用方传入的通配符被当作模式解释
     */
    String KEYWORD_CONDITION = "<if test=\"keyword != null\">"
        + " WHERE `id` LIKE CONCAT('%', #{keyword}, '%') ESCAPE '\\\\'"
        + "</if>";

    @Select("SELECT " + SELECT_COLUMNS + " FROM issue_status WHERE `id` = #{id}")
    IssueStatusDTO selectById(@Param("id") String id);

    @Select("<script>"
        + "SELECT COUNT(*) FROM issue_status"
        + KEYWORD_CONDITION
        + "</script>")
    int countByKeyword(@Param("keyword") String keyword);

    @Select("<script>"
        + "SELECT " + SELECT_COLUMNS + " FROM issue_status"
        + KEYWORD_CONDITION
        + " " + ORDER_BY
        + " LIMIT #{pageSize} OFFSET #{start}"
        + "</script>")
    List<IssueStatusDTO> listByKeyword(@Param("keyword") String keyword,
                                       @Param("start") int start,
                                       @Param("pageSize") int pageSize);

    /**
     * 按主键upsert。外部脚本对同一个Issue反复写状态，走INSERT ... ON DUPLICATE KEY UPDATE
     * 而不是先查后写，避免并发下的竞态
     */
    @Insert("INSERT INTO issue_status ("
        + "`id`, `issue_no`, `status`, `status_description`, `update_time`"
        + ") VALUES ("
        + "#{id}, #{issueNo}, #{status}, #{statusDescription}, #{updateTime}"
        + ") ON DUPLICATE KEY UPDATE "
        + "`issue_no` = VALUES(`issue_no`), "
        + "`status` = VALUES(`status`), "
        + "`status_description` = VALUES(`status_description`), "
        + "`update_time` = VALUES(`update_time`)")
    int upsert(IssueStatusDTO dto);
}
