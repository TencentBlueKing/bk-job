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

package com.tencent.bk.job.execute.model.esb.gse.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class EsbGseManageProcRequest extends EsbReq {
    @JsonProperty("bk_biz_id")
    private Long appId;

    @JsonProperty("op_type")
    private Integer opType;

    @JsonProperty("process_infos")
    private List<ManageProcessInfoDTO> processInfos;


    @Setter
    @Getter
    @ToString
    public static class ManageProcessInfoDTO {
        /**
         * IP对象数组 Y
         */
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonProperty("ip_list")
        private List<EsbIpDTO> ipList;
        /**
         * 进程名称, 例如bk_gse Y
         */
        @JsonProperty("proc_name")
        private String procName;
        /**
         * 进程属主（联系人） Y
         */
        @JsonProperty("proc_owner")
        private String procOwner;
        /**
         * 进程路径, 例如/usr/local/gse/sbin N
         */
        @JsonProperty("setup_path")
        private String setupPath;
        /**
         * 进程配置文件路径 N
         */
        @JsonProperty("cfg_path")
        private String cfgPath;
        /**
         * 进程日志路径 N
         */
        @JsonProperty("log_path")
        private String logPath;
        /**
         * 进程的pid文件所在路径 N
         */
        @JsonProperty("pid_path")
        private String pidPath;
        /**
         * 系统帐号，默认不传为root N
         */
        private String account;
        /**
         * 进程启动命令 N
         */
        @JsonProperty("start_cmd")
        private String startCmd;
        /**
         * 进程停止命令 N
         */
        @JsonProperty("stop_cmd")
        private String stopCmd;
        /**
         * 进程重启命令 N
         */
        @JsonProperty("restart_cmd")
        private String restartCmd;
        /**
         * 进程reload命令 N
         */
        @JsonProperty("reload_cmd")
        private String reloadCmd;
        /**
         * 进程kill命令 N
         */
        @JsonProperty("kill_cmd")
        private String killCmd;
        /**
         * 进程kill命令 N
         */
        @JsonProperty("func_id")
        private String funcId;
        /**
         * CC定义的进程实例ID N
         */
        @JsonProperty("instance_id")
        private String instanceId;
        /**
         * 进程索引 N
         */
        @JsonProperty("value_key")
        private String valueKey;

        /**
         * 进程托管类型，必填字段。0为周期执行进程，1为常驻进程，2为单次执行进程
         */
        @JsonProperty("type")
        private Integer type;
        /**
         * 进程使用cpu限制[0-100]。 N
         */
        @JsonProperty("cpu_lmt")
        private Integer cpuLmt;
        /**
         * 进程使用mem限制[0-100]。 N
         */
        @JsonProperty("mem_lmt")
        private Integer memLmt;
        /**
         * type为 0，cycle_time需要指定
         */
        @JsonProperty("cycle_time")
        private Integer cycleTime;
        /**
         * 进程实例个数 N
         */
        @JsonProperty("instance_num")
        private Integer instanceNum;
        /**
         * 进程启动后开始检查时间 N
         */
        @JsonProperty("start_check_begin_time")
        private Integer startCheckBeginTime;
        /**
         * 进程启动后结束检查时间 N
         */
        @JsonProperty("start_check_end_time")
        private Integer startCheckEndTime;
        /**
         * 进程操作超时时间 N
         */
        @JsonProperty("op_timeout")
        private Integer opTimeout;
    }

}
