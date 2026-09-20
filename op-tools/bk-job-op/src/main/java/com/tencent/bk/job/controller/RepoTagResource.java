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

package com.tencent.bk.job.controller;

import com.tencent.bk.job.model.BatchAddTagResp;
import com.tencent.bk.job.model.BatchDeleteTagResp;
import com.tencent.bk.job.model.BatchTagReq;
import com.tencent.bk.job.model.NextTagResp;
import com.tencent.bk.job.model.TagListResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 代码仓库Tag管理接口
 * <p>
 * 纳管范围：{@code v{major}.{minor}.{patch}} 稳定版，以及 {@code -alpha.N}/{@code -beta.N}/{@code -rc.N}
 * 三类先行版，N越大越新。其余先行版（如-devgray.N、-json.N）是开发自测的临时版本，不纳入Tag管理。
 * <p>
 * 大小写：Tag的v前缀不区分大小写，先行标识区分大小写（{@code v3.10.1-Alpha.1} 判为非法）；
 * 响应中的Tag一律为小写v前缀的归一化形态。
 * <p>
 * 路径挂在/api下是为了复用ApiWebMvcConfig为/api/**注册的鉴权拦截器，调用方须携带请求头
 * {@code X-Job-Op-Api-Key}，缺失或错误时返回401。
 * <p>
 * 错误约定：除鉴权失败外一律返回HTTP 200，业务与参数错误体现为响应体的 {@code result=false} + {@code message}。
 */
@RequestMapping("/api/repoTag")
public interface RepoTagResource {

    /**
     * 批量写入Tag
     * <p>
     * 允许部分成功：不纳管的Tag进ignoredTags，格式非法的进invalidTags，整批继续写入其余Tag。
     * 其中四段式历史Tag（如v3.3.4.1）会稳定出现在ignoredTags中，属预期噪声，调用方不应据此告警。
     * 单批上限1000条，超限直接拒绝而不截断。重复写入幂等，重复项进existedTags。
     *
     * @param req Tag列表请求体
     * @return 写入结果，含新增/已存在/不纳管/非法4个分桶明细
     */
    @PostMapping("/batch_add_tags")
    BatchAddTagResp batchAddTags(@RequestBody BatchTagReq req);

    /**
     * 批量删除Tag
     * <p>
     * 只支持按完整Tag列表删除，不支持按版本前缀删除——前缀删除等价于一键清空整个版本系列，
     * 调用方可先list_tags再按列表删除。删除幂等，库中不存在的合法Tag进notFoundTags。
     *
     * @param req Tag列表请求体
     * @return 删除结果，含已删除/库中不存在/不纳管/非法4个分桶明细
     */
    @PostMapping("/batch_delete_tags")
    BatchDeleteTagResp batchDeleteTags(@RequestBody BatchTagReq req);

    /**
     * 按版本前缀查询Tag
     * <p>
     * 这里的前缀粒度由调用方决定：传3.10.1返回该修订号下的全部Tag（含稳定版与各类先行版），
     * 传3.10.1-alpha只返回alpha先行版。注意与next_tag中"系列"的含义区别（见该接口说明）。
     * 命中数超过1000条时截断返回并置truncated=true，此时应收窄prefix重查。
     *
     * @param prefix 版本前缀，支持x、x.y、x.y.z、x.y.z-{alpha|beta|rc}四种形态，可带v/V前缀
     * @param order  排序方向，asc为最旧在前，缺省或其余值均按desc（最新在前）处理
     * @return 按语义序排列的Tag列表
     */
    @GetMapping("/list_tags")
    TagListResp listTags(@RequestParam(value = "prefix", required = false) String prefix,
                         @RequestParam(value = "order", required = false) String order);

    /**
     * 求某版本系列的下一个Tag
     * <p>
     * 入参只接受x.y.z与x.y.z-{alpha|beta|rc}两种形态：两位版本号（如3.10）的"下一个"无法判定是
     * 下一个修订号还是下一个小版本，属真歧义，直接返回result=false。
     * <p>
     * 响应中的series字段是本次取最大值的实际检索范围，两个分支粒度不同：
     * <ul>
     *     <li>传3.10.3-alpha：series为3.10.3-alpha，在该修订号的alpha子集内取最大先行号+1；
     *     已有alpha.2与alpha.11时返回v3.10.3-alpha.12（整数比较，不是字典序）；该子集为空时返回.1</li>
     *     <li>传3.10.3：series为3.10（注意是x.y而不是x.y.z），检索范围是整个3.10小版本的稳定版子集，
     *     返回v3.10.{max(入参修订号, 最大稳定版修订号+1)}。已有v3.10.7时传3.10.3会返回v3.10.8而不是
     *     v3.10.4，避免给出一个已被占用的Tag；这种顺延会在message中提示。该小版本尚无稳定版时返回入参本身</li>
     * </ul>
     * 修订号无上限也不向小版本进位，v3.10.9的下一个是v3.10.10而非v3.11.0。
     * <p>
     * 本接口是只读的，只返回<b>建议值</b>，不承诺唯一性分配：Tag的权威唯一性由Git打Tag那一步保证，
     * 服务端不加锁也不做冲突保护。调用方在Git打Tag失败时应重新调用本接口取新的建议值。
     *
     * @param series 版本系列，可带v/V前缀
     * @return 下一个Tag的建议值，以及推导所依据的当前最大Tag
     */
    @GetMapping("/next_tag")
    NextTagResp nextTag(@RequestParam(value = "series", required = false) String series);
}
