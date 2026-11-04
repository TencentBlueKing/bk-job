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
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue状态 Mapper 生成的SQL。
 *
 * <p>不依赖真实数据库，直接从MyBatis的 {@link MappedStatement} 取出最终SQL做断言，
 * 守住三件事：本工程未开启 mapUnderscoreToCamelCase 因此必须写列别名、分页与模糊过滤下推到SQL、
 * 排序按最后修改时间倒序且使用id作为稳定次级键。</p>
 */
class IssueStatusMapperTest {

    private static final String MAPPER = IssueStatusMapper.class.getName();

    private static Configuration configuration;

    @BeforeAll
    static void setUpAll() {
        configuration = new Configuration();
        configuration.addMapper(IssueStatusMapper.class);
    }

    private String sqlOf(String methodName, Map<String, Object> params) {
        MappedStatement statement = configuration.getMappedStatement(MAPPER + "." + methodName);
        BoundSql boundSql = statement.getBoundSql(params);
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }

    private Map<String, Object> pageParams(String keyword) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("start", 40);
        params.put("pageSize", 20);
        return params;
    }

    @Test
    @DisplayName("工程未开启下划线转驼峰，因此SELECT必须为每列写camelCase别名")
    void shouldAliasAllColumnsToCamelCase() {
        assertThat(configuration.isMapUnderscoreToCamelCase()).isFalse();

        String sql = sqlOf("selectById", Map.of("id", "issue_4523"));

        assertThat(sql).contains("`id` AS id")
            .contains("`status` AS status")
            .contains("`status_description` AS statusDescription")
            .contains("`update_time` AS updateTime")
            .doesNotContain("issue_no");
    }

    @Test
    @DisplayName("分页用LIMIT/OFFSET下推到SQL，不在内存里切列表")
    void shouldPaginateInSql() {
        String sql = sqlOf("listByKeyword", pageParams(null));

        assertThat(sql).endsWith("LIMIT ? OFFSET ?");
    }

    @Test
    @DisplayName("按最后修改时间倒序，同一修改时间按id倒序稳定排序")
    void shouldOrderByUpdateTimeAndStableId() {
        String sql = sqlOf("listByKeyword", pageParams(null));

        assertThat(sql).contains("ORDER BY `update_time` DESC, `id` DESC")
            .doesNotContain("issue_no");
    }

    @Test
    @DisplayName("关键字为空时不拼WHERE条件")
    void shouldOmitWhereClauseWhenKeywordIsNull() {
        assertThat(sqlOf("listByKeyword", pageParams(null))).doesNotContain("WHERE");
        assertThat(sqlOf("countByKeyword", nullKeyword())).isEqualTo("SELECT COUNT(*) FROM issue_status");
    }

    @Test
    @DisplayName("关键字非空时用带ESCAPE的LIKE做模糊过滤")
    void shouldFilterByLikeWithExplicitEscape() {
        String listSql = sqlOf("listByKeyword", pageParams("issue_45"));
        String countSql = sqlOf("countByKeyword", Map.of("keyword", "issue_45"));

        assertThat(listSql).contains("WHERE `id` LIKE CONCAT('%', ?, '%') ESCAPE '\\\\'");
        assertThat(countSql).isEqualTo(
            "SELECT COUNT(*) FROM issue_status WHERE `id` LIKE CONCAT('%', ?, '%') ESCAPE '\\\\'");
    }

    @Test
    @DisplayName("total走COUNT(*)，与列表用同一套过滤条件，因此是精确值")
    void shouldCountWithSameFilterAsList() {
        String countSql = sqlOf("countByKeyword", Map.of("keyword", "issue_45"));
        String listSql = sqlOf("listByKeyword", pageParams("issue_45"));

        assertThat(countSql).startsWith("SELECT COUNT(*)");
        String filter = "WHERE `id` LIKE CONCAT('%', ?, '%') ESCAPE '\\\\'";
        assertThat(countSql).contains(filter);
        assertThat(listSql).contains(filter);
    }

    @Test
    @DisplayName("写入走INSERT ... ON DUPLICATE KEY UPDATE，避免先查后写的竞态")
    void shouldUpsertByPrimaryKey() {
        String sql = sqlOf("upsert", null);

        assertThat(sql).startsWith("INSERT INTO issue_status")
            .contains("ON DUPLICATE KEY UPDATE")
            .contains("`update_time` = VALUES(`update_time`)");
    }

    @Test
    @DisplayName("upsert的参数占位与DTO属性一一对应")
    void shouldBindUpsertParametersFromDto() {
        MappedStatement statement = configuration.getMappedStatement(MAPPER + ".upsert");
        IssueStatusDTO dto = new IssueStatusDTO("issue_4523", "issue_todo", "待处理", 1758542400000L);

        BoundSql boundSql = statement.getBoundSql(dto);

        assertThat(boundSql.getParameterMappings()).extracting("property")
            .containsExactly("id", "status", "statusDescription", "updateTime");
        assertThat(boundSql.getSql()).doesNotContain("issue_no");
    }

    @Test
    @DisplayName("删除按主键执行物理DELETE")
    void shouldPhysicallyDeleteById() {
        String sql = sqlOf("deleteById", Map.of("id", "issue_4523"));

        assertThat(sql).isEqualTo("DELETE FROM issue_status WHERE `id` = ?");
    }

    private Map<String, Object> nullKeyword() {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", null);
        return params;
    }
}
