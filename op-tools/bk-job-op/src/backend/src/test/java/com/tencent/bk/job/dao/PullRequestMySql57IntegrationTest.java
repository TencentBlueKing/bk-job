package com.tencent.bk.job.dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("mysqlIntegration")
@Testcontainers
class PullRequestMySql57IntegrationTest {

    private static final String LATEST_QUERY = """
        SELECT p.`issue_id`, p.`pr_id`
        FROM `pull_request` p
        WHERE p.`issue_id` IN (?, ?, ?)
          AND NOT EXISTS (
              SELECT 1
              FROM `pull_request` newer
              WHERE newer.`issue_id` = p.`issue_id`
                AND (
                    newer.`create_time` > p.`create_time`
                    OR (
                        newer.`create_time` = p.`create_time`
                        AND newer.`pr_id` > p.`pr_id`
                    )
                )
          )
        """;

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
        DockerImageName.parse("mysql:5.7.44"))
        .withDatabaseName("job_op")
        .withUsername("job")
        .withPassword("job");

    @BeforeAll
    static void initializeSchema() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            for (Path script : sqlScripts()) {
                executeScript(connection, script);
            }
            executeScript(connection, sqlScripts().get(sqlScripts().size() - 1));
        }
    }

    @Test
    void shouldCreateExpectedColumnsAndIndexes() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            assertThat(columnNames(connection)).containsExactly(
                "id", "pr_id", "issue_id", "code_dir", "status", "agent_name", "author_name",
                "imate_conversation_id", "imate_conversation_title", "create_time", "update_time",
                "row_create_time", "row_update_time"
            );
            assertThat(indexNames(connection)).contains("PRIMARY", "uk_pr_id", "idx_issue_id_create_time_pr_id");
        }
    }

    @Test
    void shouldRejectDuplicatePrIdWithMysql1062() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            insert(connection, 4601, 4563, 100);
            assertThatThrownBy(() -> insert(connection, 4601, 4564, 200))
                .isInstanceOfSatisfying(SQLException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(1062));
        }
    }

    @Test
    void shouldSelectLatestByCreateTimeThenPrIdForEachIssue() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            insert(connection, 4610, 4570, 100);
            insert(connection, 4611, 4570, 200);
            insert(connection, 4620, 4580, 300);
            insert(connection, 4621, 4580, 300);

            try (PreparedStatement statement = connection.prepareStatement(LATEST_QUERY)) {
                statement.setLong(1, 4570);
                statement.setLong(2, 4580);
                statement.setLong(3, 4590);
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<String> rows = new ArrayList<>();
                    while (resultSet.next()) {
                        rows.add(resultSet.getLong("issue_id") + ":" + resultSet.getLong("pr_id"));
                    }
                    assertThat(rows).containsExactlyInAnyOrder("4570:4611", "4580:4621");
                }
            }
        }
    }

    @Test
    void latestQueryShouldExposeCompositeIndexToMysql57Optimizer() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             PreparedStatement statement = connection.prepareStatement("EXPLAIN " + LATEST_QUERY)) {
            statement.setLong(1, 4570);
            statement.setLong(2, 4580);
            statement.setLong(3, 4590);
            try (ResultSet resultSet = statement.executeQuery()) {
                boolean compositeIndexAvailable = false;
                while (resultSet.next()) {
                    String possibleKeys = resultSet.getString("possible_keys");
                    compositeIndexAvailable |= possibleKeys != null
                        && possibleKeys.contains("idx_issue_id_create_time_pr_id");
                }
                assertThat(compositeIndexAvailable).isTrue();
            }
        }
    }

    private static List<Path> sqlScripts() throws IOException {
        Path sqlDirectory = Path.of("..", "..", "support-files", "sql").normalize();
        try (Stream<Path> paths = Files.list(sqlDirectory)) {
            return paths
                .filter(path -> path.getFileName().toString().matches("\\d{4}.*_mysql\\.sql"))
                .sorted()
                .toList();
        }
    }

    private static void executeScript(Connection connection, Path script) throws Exception {
        String sql = Files.readString(script);
        for (String statementSql : sql.split(";")) {
            if (!statementSql.isBlank()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(statementSql);
                }
            }
        }
    }

    private static List<String> columnNames(Connection connection) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COLUMN_NAME FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = 'job_op' AND TABLE_NAME = 'pull_request' ORDER BY ORDINAL_POSITION");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                columns.add(resultSet.getString(1));
            }
        }
        return columns;
    }

    private static List<String> indexNames(Connection connection) throws SQLException {
        List<String> indexes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = 'job_op' AND TABLE_NAME = 'pull_request'");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                indexes.add(resultSet.getString(1));
            }
        }
        return indexes;
    }

    private static void insert(Connection connection, long prId, long issueId, long createTime) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO pull_request "
                + "(pr_id, issue_id, code_dir, status, agent_name, author_name, create_time, update_time) "
                + "VALUES (?, ?, 'C:/repo', 'WAIT_FOR_REVIEW', 'agent', 'author', ?, ?)")) {
            statement.setLong(1, prId);
            statement.setLong(2, issueId);
            statement.setLong(3, createTime);
            statement.setLong(4, createTime);
            statement.executeUpdate();
        }
    }
}
