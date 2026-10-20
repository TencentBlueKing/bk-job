package com.tencent.bk.job.controller;

import com.tencent.bk.job.consts.ApiPaths;
import com.tencent.bk.job.consts.WebPaths;
import com.tencent.bk.job.model.DeleteIssueStatusReq;
import com.tencent.bk.job.model.DeleteIssueStatusResp;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import com.tencent.bk.job.service.model.PageData;
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
    @GetMapping(ApiPaths.ISSUE_STATUS)
    IssueStatusResp getIssueStatus(
        @RequestParam(value = "id") String id
    );

    /**
     * 设置指定Issue的状态
     *
     * @param req Issue状态设置请求体
     * @return 更新后的Issue状态响应
     */
    @PostMapping(ApiPaths.ISSUE_STATUS)
    IssueStatusResp setIssueStatus(
        @RequestBody SetIssueStatusReq req
    );

    /**
     * 分页查询Issue状态列表
     *
     * @param idKeyword Issue标识关键字，对id做大小写不敏感的模糊匹配，为空表示不过滤
     * @param start     分页起始位置，从0开始，非法值被纠正为0
     * @param pageSize  每页大小，非法值被纠正为默认值20，超过200时收敛为200
     * @return 分页后的Issue状态列表
     */
    @GetMapping(WebPaths.ISSUE_STATUS_LIST)
    PageData<IssueStatusResp> listIssueStatus(
        @RequestParam(value = "idKeyword", required = false) String idKeyword,
        @RequestParam(value = "start", required = false, defaultValue = "0") Integer start,
        @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize
    );

    /**
     * 物理删除指定Issue的状态数据
     *
     * @param req Issue状态删除请求体
     * @return 实际删除行数
     */
    @PostMapping(WebPaths.ISSUE_STATUS_DELETE)
    DeleteIssueStatusResp deleteIssueStatus(
        @RequestBody DeleteIssueStatusReq req
    );
}
