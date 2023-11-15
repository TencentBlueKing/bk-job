package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class EsbJobInstanceStatusV3DTO {

    @JsonProperty("finished")
    private Boolean finished;

    @JsonProperty("job_instance")
    private JobInstance jobInstance;

    @JsonProperty("step_instance_list")
    private List<StepInst> stepInstances;

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobInstance {
        @JsonProperty("job_instance_id")
        private Long id;
        /**
         * 业务id
         */
        @JsonProperty("bk_biz_id")
        private Long appId;
        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        @JsonProperty("status")
        private Integer status;
        /**
         * 作业实例名称
         */
        @JsonProperty("name")
        private String name;
        /**
         * 脚本创建时间
         */
        @JsonProperty("create_time")
        private Long createTime;
        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;
        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        private Long endTime;
        /**
         * 总耗时，单位：秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StepInst {
        /**
         * id
         */
        @JsonProperty("step_instance_id")
        private Long id;
        /**
         * 名称
         */
        @JsonProperty("name")
        private String name;
        /**
         * 状态： 1.未执行、2.正在执行、3.执行完成且成功、4.执行失败
         */
        @JsonProperty("status")
        private Integer status;

        /**
         * 步骤类型：1.脚本步骤; 2.文件步骤; 4.SQL步骤
         */
        @JsonProperty("type")
        private Integer type;

        /**
         * 执行次数
         */
        @JsonProperty("execute_count")
        private Integer executeCount;
        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;
        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        private Long endTime;
        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
        /**
         * 创建时间
         */
        @JsonProperty("create_time")
        private Long createTime;

        @JsonProperty("step_ip_result_list")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<IpResult> stepIpResult;
    }

    @Setter
    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class IpResult {
        private String ip;
        @JsonProperty("bk_cloud_id")
        private Long cloudAreaId;
        private Integer status;
        private String tag;
        @JsonProperty("exit_code")
        private Integer exitCode;

        @JsonProperty("error_code")
        private Integer errorCode;

        /**
         * 开始时间
         */
        @JsonProperty("start_time")
        private Long startTime;
        /**
         * 结束时间
         */
        @JsonProperty("end_time")
        private Long endTime;
        /**
         * 总耗时，单位：毫秒
         */
        @JsonProperty("total_time")
        private Long totalTime;
    }
}
