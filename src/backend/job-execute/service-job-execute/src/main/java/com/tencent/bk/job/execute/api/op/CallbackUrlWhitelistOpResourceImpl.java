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

package com.tencent.bk.job.execute.api.op;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.execute.dao.CallbackUrlWhiteInfoDAO;
import com.tencent.bk.job.execute.model.CallbackUrlWhiteInfoDTO;
import com.tencent.bk.job.execute.model.op.req.BatchAddCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.req.BatchDeleteCallbackUrlWhitelistReq;
import com.tencent.bk.job.execute.model.op.vo.CallbackUrlWhitelistVO;
import com.tencent.bk.job.execute.service.validation.CallbackUrlValidateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController("jobExecuteCallbackUrlWhitelistOpResourceImpl")
public class CallbackUrlWhitelistOpResourceImpl implements CallbackUrlWhitelistOpResource {

    private final CallbackUrlWhiteInfoDAO callbackUrlWhiteInfoDAO;
    private final CallbackUrlValidateService callbackUrlValidateService;

    @Autowired
    public CallbackUrlWhitelistOpResourceImpl(CallbackUrlWhiteInfoDAO callbackUrlWhiteInfoDAO,
                                              CallbackUrlValidateService callbackUrlValidateService) {
        this.callbackUrlWhiteInfoDAO = callbackUrlWhiteInfoDAO;
        this.callbackUrlValidateService = callbackUrlValidateService;
    }

    @Override
    public Response<Integer> batchAdd(String username, BatchAddCallbackUrlWhitelistReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getItems())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        // 1. 入参逐条格式校验（http(s)://...）
        for (BatchAddCallbackUrlWhitelistReq.Item item : req.getItems()) {
            callbackUrlValidateService.validateWhitelistBaseUrl(item == null ? null : item.getBaseUrl());
        }
        // 2. 入参内部去重，避免同一批次重复 baseUrl 触发 DB 唯一键冲突
        Set<String> requestedBaseUrls = req.getItems().stream()
            .map(BatchAddCallbackUrlWhitelistReq.Item::getBaseUrl)
            .map(String::trim)
            .collect(Collectors.toCollection(HashSet::new));
        // 3. DB 存在性检查（命中任一已存在 baseUrl 即整体拒绝，避免部分成功/失败的歧义）
        List<String> existing = callbackUrlWhiteInfoDAO.filterExistingBaseUrls(requestedBaseUrls);
        if (!existing.isEmpty()) {
            throw new InvalidParamException(
                ErrorCode.CALLBACK_URL_WHITELIST_ALREADY_EXISTS, String.join(", ", existing));
        }
        // 4. 构造 DTO 批量插入
        List<CallbackUrlWhiteInfoDTO> records = new ArrayList<>(req.getItems().size());
        for (BatchAddCallbackUrlWhitelistReq.Item item : req.getItems()) {
            CallbackUrlWhiteInfoDTO dto = new CallbackUrlWhiteInfoDTO();
            dto.setBaseUrl(item.getBaseUrl().trim());
            dto.setDescription(item.getDescription());
            dto.setCreator(username);
            dto.setLastModifyUser(username);
            records.add(dto);
        }
        int affected = callbackUrlWhiteInfoDAO.batchInsert(records);
        // 5. 立即失效缓存，保证下一次校验立刻命中新增项
        callbackUrlValidateService.invalidateCache();
        log.info("[OP] user={} batchAdd callback url whitelist, items={}, affected={}",
            username, requestedBaseUrls, affected);
        return Response.buildSuccessResp(affected);
    }

    @Override
    public Response<PageData<CallbackUrlWhitelistVO>> list(String username, Integer start, Integer length) {
        PageData<CallbackUrlWhiteInfoDTO> pageData = callbackUrlWhiteInfoDAO.listByPage(start, length);
        PageData<CallbackUrlWhitelistVO> result = new PageData<>();
        result.setStart(pageData.getStart());
        result.setPageSize(pageData.getPageSize());
        result.setTotal(pageData.getTotal());
        result.setData(pageData.getData().stream().map(this::toVO).collect(Collectors.toList()));
        return Response.buildSuccessResp(result);
    }

    @Override
    public Response<Integer> batchDelete(String username, BatchDeleteCallbackUrlWhitelistReq req) {
        if (req == null || CollectionUtils.isEmpty(req.getIdList())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        int affected = callbackUrlWhiteInfoDAO.deleteByIds(req.getIdList());
        callbackUrlValidateService.invalidateCache();
        log.info("[OP] user={} batchDelete callback url whitelist, ids={}, affected={}",
            username, req.getIdList(), affected);
        return Response.buildSuccessResp(affected);
    }

    private CallbackUrlWhitelistVO toVO(CallbackUrlWhiteInfoDTO dto) {
        CallbackUrlWhitelistVO vo = new CallbackUrlWhitelistVO();
        vo.setId(dto.getId());
        vo.setBaseUrl(dto.getBaseUrl());
        vo.setDescription(dto.getDescription());
        vo.setCreator(dto.getCreator());
        vo.setLastModifyUser(dto.getLastModifyUser());
        vo.setCreateTime(TimeUtil.getTimeStr(dto.getCreateTime(), TimeUtil.DEFAULT_TIME_FORMAT));
        vo.setLastModifyTime(TimeUtil.getTimeStr(dto.getLastModifyTime(), TimeUtil.DEFAULT_TIME_FORMAT));
        return vo;
    }
}
