package com.tencent.bk.job.service;

import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;

import java.util.Collection;
import java.util.Map;

public interface PullRequestService {

    PullRequestResp create(CreatePullRequestReq req);

    PullRequestResp getByPrId(Long prId);

    PullRequestResp updateStatus(UpdatePullRequestStatusReq req);

    Map<Long, Long> findLatestPrIdsByIssueIds(Collection<Long> issueIds);
}
