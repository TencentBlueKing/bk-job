package com.tencent.bk.job.controller.api.impl;

import com.tencent.bk.job.controller.api.PullRequestResource;
import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;
import com.tencent.bk.job.service.PullRequestService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PullRequestResourceImpl implements PullRequestResource {

    private final PullRequestService pullRequestService;

    public PullRequestResourceImpl(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @Override
    public PullRequestResp create(CreatePullRequestReq req) {
        return pullRequestService.create(req);
    }

    @Override
    public PullRequestResp getByPrId(Long prId) {
        return pullRequestService.getByPrId(prId);
    }

    @Override
    public PullRequestResp updateStatus(UpdatePullRequestStatusReq req) {
        return pullRequestService.updateStatus(req);
    }
}
