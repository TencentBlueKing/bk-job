package com.tencent.bk.job.controller.api.impl;

import com.tencent.bk.job.controller.api.IssueStatusResource;
import com.tencent.bk.job.dao.IssueStatusMapper;
import com.tencent.bk.job.model.IssueStatusDTO;
import com.tencent.bk.job.model.IssueStatusResp;
import com.tencent.bk.job.model.SetIssueStatusReq;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IssueStatusResourceImpl implements IssueStatusResource {

    private final IssueStatusMapper issueStatusMapper;

    public IssueStatusResourceImpl(IssueStatusMapper issueStatusMapper) {
        this.issueStatusMapper = issueStatusMapper;
    }

    @Override
    public IssueStatusResp getIssueStatus(String id) {
        IssueStatusDTO dto = issueStatusMapper.selectById(id);
        if (dto == null) {
            return new IssueStatusResp(id, null, null, null);
        }
        return toResp(dto);
    }

    @Override
    public IssueStatusResp setIssueStatus(SetIssueStatusReq req) {
        // 更新时间由服务端生成，不从请求体接收：调用方的时钟不可信
        IssueStatusDTO dto = new IssueStatusDTO(
            req.getId(),
            req.getStatus(),
            req.getStatusDescription(),
            System.currentTimeMillis()
        );
        issueStatusMapper.upsert(dto);
        return toResp(dto);
    }

    private IssueStatusResp toResp(IssueStatusDTO dto) {
        return new IssueStatusResp(dto.getId(), dto.getStatus(), dto.getStatusDescription(), dto.getUpdateTime());
    }

}
