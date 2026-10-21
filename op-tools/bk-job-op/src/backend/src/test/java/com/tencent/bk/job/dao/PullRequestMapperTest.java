package com.tencent.bk.job.dao;

import com.tencent.bk.job.model.PullRequestDTO;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PullRequestMapperTest {

    private static Configuration configuration;

    @BeforeAll
    static void setUpAll() {
        configuration = new Configuration();
        configuration.addMapper(PullRequestMapper.class);
    }

    @Test
    void shouldAliasAllSnakeCaseColumns() {
        String sql = sqlOf("selectByPrId", Map.of("prId", 4564L));

        assertThat(sql)
            .contains("p.`pr_id` AS prId")
            .contains("p.`issue_id` AS issueId")
            .contains("p.`code_dir` AS codeDir")
            .contains("p.`agent_name` AS agentName")
            .contains("p.`imate_conversation_id` AS imateConversationId")
            .contains("p.`create_time` AS createTime");
    }

    @Test
    void shouldBindAllInsertFields() {
        PullRequestDTO dto = new PullRequestDTO();
        BoundSql boundSql = statement("insert").getBoundSql(dto);

        assertThat(boundSql.getParameterMappings()).extracting("property").containsExactly(
            "prId", "issueId", "codeDir", "status", "agentName", "authorName",
            "imateConversationId", "imateConversationTitle", "createTime", "updateTime"
        );
    }

    @Test
    void shouldUpdateOnlyStatusAndUpdateTime() {
        String sql = sqlOf("updateStatusByPrId", Map.of(
            "prId", 4564L, "status", "HANDLING", "updateTime", 1L));

        assertThat(sql).isEqualTo(
            "UPDATE pull_request SET `status` = ?, `update_time` = ? WHERE `pr_id` = ?");
    }

    @Test
    void shouldDeleteByPrId() {
        BoundSql boundSql = statement("deleteByPrId").getBoundSql(Map.of("prId", 4564L));

        assertThat(boundSql.getSql().replaceAll("\\s+", " ").trim())
            .isEqualTo("DELETE FROM pull_request WHERE `pr_id` = ?");
        assertThat(boundSql.getParameterMappings()).extracting("property").containsExactly("prId");
    }

    @Test
    void shouldCountAndListByStableUpdateOrder() {
        assertThat(sqlOf("countAll", Map.of())).isEqualTo("SELECT COUNT(*) FROM pull_request");

        BoundSql boundSql = statement("listAll").getBoundSql(Map.of("start", 20, "pageSize", 10));
        assertThat(boundSql.getSql().replaceAll("\\s+", " ").trim())
            .contains("ORDER BY p.`update_time` DESC, p.`pr_id` DESC")
            .endsWith("LIMIT ?, ?");
        assertThat(boundSql.getParameterMappings()).extracting("property")
            .containsExactly("start", "pageSize");
    }

    @Test
    void shouldSelectLatestPullRequestWithoutMySql8Syntax() {
        String sql = sqlOf("selectLatestByIssueIds", Map.of("issueIds", List.of(4563L, 4564L)));

        assertThat(sql)
            .contains("p.`issue_id` IN ( ? , ? )")
            .contains("AND NOT EXISTS")
            .contains("newer.`create_time` > p.`create_time`")
            .contains("newer.`create_time` = p.`create_time`")
            .contains("newer.`pr_id` > p.`pr_id`")
            .doesNotContain("ROW_NUMBER", "OVER", "WITH ");
    }

    private String sqlOf(String methodName, Object params) {
        return statement(methodName).getBoundSql(params).getSql().replaceAll("\\s+", " ").trim();
    }

    private MappedStatement statement(String methodName) {
        return configuration.getMappedStatement(PullRequestMapper.class.getName() + "." + methodName);
    }
}
