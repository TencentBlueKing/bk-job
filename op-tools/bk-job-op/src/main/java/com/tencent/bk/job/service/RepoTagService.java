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

package com.tencent.bk.job.service;

import com.tencent.bk.job.model.BatchAddTagResp;
import com.tencent.bk.job.model.BatchDeleteTagResp;
import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.TagListResp;

import java.util.List;

/**
 * 仓库Tag管理服务接口
 */
public interface RepoTagService {

    /**
     * 批量写入Tag
     *
     * @param tagList 原始Tag列表
     * @return 写入结果，含新增/已存在/不纳管/非法4个分桶明细
     */
    BatchAddTagResp batchAddTags(List<String> tagList);

    /**
     * 批量删除Tag
     *
     * @param tagList 原始Tag列表
     * @return 删除结果，含已删除/库中不存在/不纳管/非法4个分桶明细
     */
    BatchDeleteTagResp batchDeleteTags(List<String> tagList);

    /**
     * 按版本前缀查询Tag
     *
     * @param prefix 版本前缀，支持x、x.y、x.y.z、x.y.z-{alpha|beta|rc}四种形态
     * @param order  排序方向，asc为最旧在前，其余值均按desc（最新在前）处理
     * @return 查询结果
     */
    TagListResp listTags(String prefix, String order);

    /**
     * 求某版本系列的下一个Tag
     *
     * @param series 版本系列，只接受x.y.z与x.y.z-{alpha|beta|rc}两种形态
     * @return 下一个Tag的建议值，不承诺唯一性分配
     */
    NextTagResp getNextTag(String series);
}
