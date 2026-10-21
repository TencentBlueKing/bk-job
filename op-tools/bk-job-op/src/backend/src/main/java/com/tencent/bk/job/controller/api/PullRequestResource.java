package com.tencent.bk.job.controller.api;

import com.tencent.bk.job.model.CreatePullRequestReq;
import com.tencent.bk.job.model.PullRequestResp;
import com.tencent.bk.job.model.UpdatePullRequestStatusReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/pullRequest")
public interface PullRequestResource {

    @PostMapping
    PullRequestResp create(@RequestBody CreatePullRequestReq req);

    @GetMapping
    PullRequestResp getByPrId(@RequestParam("prId") Long prId);

    @PostMapping("/status")
    PullRequestResp updateStatus(@RequestBody UpdatePullRequestStatusReq req);
}
