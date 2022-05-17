/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.api.common;

import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.manage.common.consts.script.ScriptCategoryEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScriptDTOBuilder {

    public ScriptDTO buildFromCreateUpdateReq(ScriptCreateUpdateReq req) {
        ScriptDTO scriptDTO = new ScriptDTO();
        scriptDTO.setId(req.getId());
        scriptDTO.setScriptVersionId(req.getScriptVersionId());
        scriptDTO.setName(req.getName());
        scriptDTO.setType(req.getType());
        if (req.getType() != null) {
            if (!req.getType().equals(ScriptTypeEnum.SQL.getValue())) {
                scriptDTO.setCategory(ScriptCategoryEnum.SYSSCRIPT.getValue());
            } else {
                scriptDTO.setCategory(ScriptCategoryEnum.SQLSCRIPT.getValue());
            }
        }
        scriptDTO.setContent(Base64Util.decodeContentToStr(req.getContent()));
        if (req.getTags() != null && !req.getTags().isEmpty()) {
            List<TagDTO> tags = new ArrayList<>();
            for (TagVO tagVO : req.getTags()) {
                TagDTO tag = new TagDTO();
                tag.setId(tagVO.getId());
                tag.setName(tagVO.getName());
                tags.add(tag);
            }
            scriptDTO.setTags(tags);
        }
        scriptDTO.setVersion(req.getVersion());
        scriptDTO.setDescription(req.getDescription());
        scriptDTO.setVersionDesc(req.getVersionDesc());
        return scriptDTO;
    }
}
