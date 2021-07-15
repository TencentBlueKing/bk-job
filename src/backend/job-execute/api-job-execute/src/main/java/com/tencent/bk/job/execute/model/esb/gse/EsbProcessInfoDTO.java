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

package com.tencent.bk.job.execute.model.esb.gse;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import lombok.Data;

import java.util.List;

@Data
public class EsbProcessInfoDTO {
    /**
     * 进程路径, 例如/usr/local/gse/sbin
     */
    @JsonProperty("setup_path")
    private String setupPath;
    /**
     * 进程名称, 例如bk_gse
     */
    @JsonProperty("proc_name")
    private String procName;
    /**
     * 进程的pid文件所在路径
     */
    @JsonProperty("pid_path")
    private String pidPath;

    @JsonProperty("cfg_path")
    private String cfgPath;

    @JsonProperty("log_path")
    private String logPath;

    @JsonProperty("contact")
    private String contact;

    @JsonProperty("start_cmd")
    private String startCmd;

    @JsonProperty("stop_cmd")
    private String stopCmd;

    @JsonProperty("restart_cmd")
    private String restartCmd;

    @JsonProperty("reload_cmd")
    private String reloadCmd;

    @JsonProperty("kill_cmd")
    private String killCmd;

    @JsonProperty("func_id")
    private String funcId;

    @JsonProperty("instance_id")
    private String instanceId;

    @JsonProperty("value_key")
    private String valueKey;


    /**
     * IP对象数组
     */
    @JsonProperty("ip_list")
    private List<EsbIpDTO> ipList;
    /**
     * 系统帐号，默认不传为root
     */
    private String account;


    private int type;

    /**
     * 进程操作控制脚本的扩展名. sh:默认值shell适于Linux或cygwin; bat:windows的dos脚本; ps1:windows的Powershell脚本;
     */
    @JsonProperty("cmd_shell_ext")
    private String cmdShellExt = "sh";
    /**
     * 进程使用cpu限制， 超过限制agent会根据配置的cmd_shell_ext 调用相应类型的stop Cmd停止进程。
     */
    @JsonProperty("cpu_lmt")
    private int cpuLmt;
    /**
     * 进程使用mem限制， 超过限制agent会根据配置的cmd_shell_ext 调用相应类型的stop Cmd停止进程。
     */
    @JsonProperty("mem_lmt")
    private int memLmt;

    @JsonProperty("cycle_time")
    private int cycleTime;

    @JsonProperty("instance_num")
    private int instanceNum;

    @JsonProperty("start_check_begin_time")
    private int startCheckBeginTime;

    @JsonProperty("start_check_end_time")
    private int startCheckEndTime;

    @JsonProperty("op_timeout")
    private int opTimeout;

}
