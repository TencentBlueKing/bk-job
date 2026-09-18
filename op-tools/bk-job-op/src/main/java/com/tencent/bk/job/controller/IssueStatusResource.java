package com.tencent.bk.job.controller;

import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Issue状态管理接口
 */
public interface IssueStatusResource {

    /**
     * 查询指定Issue的状态
     *
     * @param id Issue标识
     * @return Issue状态响应
     */
    @GetMapping("/issueStatus")
    IssueStatusResp getIssueStatus(
        @RequestParam(value = "id") String id
    );

    /**
     * 设置指定Issue的状态
     *
     * @param req Issue状态设置请求体
     * @return 更新后的Issue状态响应
     */
    @PostMapping("/issueStatus")
    IssueStatusResp setIssueStatus(
        @RequestBody SetIssueStatusReq req
    );
}
