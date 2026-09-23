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

package com.tencent.bk.job.controller.impl;

import com.tencent.bk.job.controller.RepoTagResource;
import com.tencent.bk.job.model.BatchAddTagResp;
import com.tencent.bk.job.model.BatchDeleteTagResp;
import com.tencent.bk.job.model.BatchTagReq;
import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.TagListResp;
import com.tencent.bk.job.service.RepoTagService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RepoTagResourceImpl implements RepoTagResource {

    private final RepoTagService repoTagService;

    public RepoTagResourceImpl(RepoTagService repoTagService) {
        this.repoTagService = repoTagService;
    }

    @Override
    public BatchAddTagResp batchAddTags(BatchTagReq req) {
        if (req == null) {
            return BatchAddTagResp.fail("request body can not be empty");
        }
        return repoTagService.batchAddTags(req.getTagList());
    }

    @Override
    public BatchDeleteTagResp batchDeleteTags(BatchTagReq req) {
        if (req == null) {
            return BatchDeleteTagResp.fail("request body can not be empty");
        }
        return repoTagService.batchDeleteTags(req.getTagList());
    }

    @Override
    public TagListResp listTags(String prefix, String order) {
        return repoTagService.listTags(prefix, order);
    }

    @Override
    public NextTagResp nextTag(String series) {
        return repoTagService.getNextTag(series);
    }
}
