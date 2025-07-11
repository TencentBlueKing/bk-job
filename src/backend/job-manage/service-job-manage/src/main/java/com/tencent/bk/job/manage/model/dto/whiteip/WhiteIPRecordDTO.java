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

package com.tencent.bk.job.manage.model.dto.whiteip;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * IP白名单DTO
 */
@Getter
@Setter
@ToString
public class WhiteIPRecordDTO {
    private Long id;
    /**
     * 多个业务id列表
     */
    private List<Long> appIdList;
    /**
     * 多个业务列表
     */
    private List<ApplicationDTO> appList;
    /**
     * 备注
     */
    private String remark;
    /**
     * IP列表
     */
    private List<WhiteIPIPDTO> ipList;
    /**
     * 生效范围列表
     */
    private List<WhiteIPActionScopeDTO> actionScopeList;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 更新人
     */
    private String lastModifier;
    /**
     * 更新时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    public WhiteIPRecordDTO(Long id,
                            List<Long> appIdList,
                            String remark,
                            List<WhiteIPIPDTO> ipList,
                            List<WhiteIPActionScopeDTO> actionScopeList,
                            String creator,
                            Long createTime,
                            String lastModifier,
                            Long lastModifyTime) {
        this.id = id;
        this.appIdList = appIdList;
        this.remark = remark;
        this.ipList = ipList;
        this.actionScopeList = actionScopeList;
        this.creator = creator;
        this.createTime = createTime;
        this.lastModifier = lastModifier;
        this.lastModifyTime = lastModifyTime;
    }

    public WhiteIPRecordVO toVO() {
        WhiteIPRecordVO vo = new WhiteIPRecordVO();
        vo.setId(id);

        if (CollectionUtils.isNotEmpty(ipList)) {
            vo.setCloudAreaId(ipList.get(0).getCloudAreaId());
            vo.setHostList(ipList.stream().map(WhiteIPIPDTO::extractWhiteIPHostVO)
                .collect(Collectors.toList()));
        }

        vo.setAllScope(isAllScope(appIdList));
        if (CollectionUtils.isNotEmpty(appList)) {
            vo.setScopeList(appList.stream().map(app ->
                new ScopeVO(app.getScope().getType().getValue(),
                    app.getScope().getId(), app.getName())).collect(Collectors.toList()));
        }
        vo.setRemark(remark);
        vo.setCreator(creator);
        vo.setCreateTime(createTime);
        vo.setLastModifier(lastModifier);
        vo.setLastModifyTime(lastModifyTime);

        return vo;
    }

    private boolean isAllScope(List<Long> appIdList) {
        return CollectionUtils.isNotEmpty(appIdList) && appIdList.contains(JobConstants.PUBLIC_APP_ID);
    }
}
