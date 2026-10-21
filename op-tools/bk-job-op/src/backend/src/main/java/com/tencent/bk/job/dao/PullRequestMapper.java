package com.tencent.bk.job.dao;

import com.tencent.bk.job.model.PullRequestDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PullRequestMapper {

    String SELECT_COLUMNS = "p.`id` AS id, "
        + "p.`pr_id` AS prId, "
        + "p.`issue_id` AS issueId, "
        + "p.`code_dir` AS codeDir, "
        + "p.`status` AS status, "
        + "p.`agent_name` AS agentName, "
        + "p.`author_name` AS authorName, "
        + "p.`imate_conversation_id` AS imateConversationId, "
        + "p.`imate_conversation_title` AS imateConversationTitle, "
        + "p.`create_time` AS createTime, "
        + "p.`update_time` AS updateTime";

    @Insert("INSERT INTO pull_request ("
        + "`pr_id`, `issue_id`, `code_dir`, `status`, `agent_name`, `author_name`, "
        + "`imate_conversation_id`, `imate_conversation_title`, `create_time`, `update_time`"
        + ") VALUES ("
        + "#{prId}, #{issueId}, #{codeDir}, #{status}, #{agentName}, #{authorName}, "
        + "#{imateConversationId}, #{imateConversationTitle}, #{createTime}, #{updateTime})")
    int insert(PullRequestDTO dto);

    @Select("SELECT " + SELECT_COLUMNS + " FROM pull_request p WHERE p.`pr_id` = #{prId}")
    PullRequestDTO selectByPrId(@Param("prId") Long prId);

    @Select("SELECT COUNT(*) FROM pull_request")
    int countAll();

    @Select("SELECT " + SELECT_COLUMNS + " FROM pull_request p "
        + "ORDER BY p.`update_time` DESC, p.`pr_id` DESC "
        + "LIMIT #{start}, #{pageSize}")
    List<PullRequestDTO> listAll(@Param("start") int start, @Param("pageSize") int pageSize);

    @Update("UPDATE pull_request SET `status` = #{status}, `update_time` = #{updateTime} "
        + "WHERE `pr_id` = #{prId}")
    int updateStatusByPrId(@Param("prId") Long prId,
                           @Param("status") String status,
                           @Param("updateTime") Long updateTime);

    @Select("<script>"
        + "SELECT " + SELECT_COLUMNS + " FROM pull_request p "
        + "WHERE p.`issue_id` IN "
        + "<foreach collection=\"issueIds\" item=\"issueId\" open=\"(\" separator=\",\" close=\")\">"
        + "#{issueId}"
        + "</foreach> "
        + "AND NOT EXISTS ("
        + "SELECT 1 FROM pull_request newer "
        + "WHERE newer.`issue_id` = p.`issue_id` "
        + "AND (newer.`create_time` &gt; p.`create_time` "
        + "OR (newer.`create_time` = p.`create_time` AND newer.`pr_id` &gt; p.`pr_id`))"
        + ")"
        + "</script>")
    List<PullRequestDTO> selectLatestByIssueIds(@Param("issueIds") List<Long> issueIds);
}
