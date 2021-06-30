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

package com.tencent.bk.job.common.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * 白名单IP的缓存对象
 *
 * @version 1.0
 * @time 2017/6/20.
 */
@Setter
@Getter
@ToString
public class WhiteListIpDTO {

    private int id;// id
    private int appId;// 业务id
    private transient int companyId;// 开发商id,缓存中查出供使用，不持久化到表中
    private String ip;// 白名单IP摘要，首个IP，用于展示
    private String ipList;// 白名单IP列表 ，如1:10.1.1.1,6:10.9.2.1 多个ip用分隔，持久到数据库
    private byte rule; // 白名单生效范围的标志位:0不生效，1快速脚本执行，2快速分发文件，3所有操作
    private String reqPerson;// 需求人
    private String creater;// 创建人
    private Timestamp createTime; // 创建时间
    private String lastModifyUser;// 最后修改人
    private Date lastModifyTime; // 最后修改时间
    private String description;// 描述
    private long source; // ip平台id，非持久字段，用于白名单缓存单IP匹配用。
    private List<String> showIpList; // 白名单IP列表，非持久字段，没有子网ID，纯IP列表，多个IP用换行符号分隔，用于前缀查询列表展开展示用的。

}
