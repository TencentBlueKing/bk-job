package com.tencent.bk.job.api.v3.constants;

/**
 * V3 版本 API URL
 */
public interface APIV3Urls {
    String CREATE_SCRIPT = "/api/job/v3/job-manage/create_script";
    String CREATE_PUBLIC_SCRIPT = "/api/job/v3/job-manage/create_public_script";
    String GET_SCRIPT_LIST = "/api/job/v3/job-manage/get_script_list";
    String UPDATE_SCRIPT_BASIC = "/api/job/v3/job-manage/update_script_basic";
    String UPDATE_PUBLIC_SCRIPT_BASIC = "/api/job/v3/job-manage/update_public_script_basic";
    String DELETE_SCRIPT = "/api/job/v3/job-manage/delete_script";
    String DELETE_PUBLIC_SCRIPT = "/api/job/v3/job-manage/delete_public_script";
    String CREATE_SCRIPT_VERSION = "/api/job/v3/job-manage/create_script_version";
    String CREATE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/create_public_script_version";
    String GET_SCRIPT_VERSION_LIST = "/api/job/v3/job-manage/get_script_version_list";
    String GET_PUBLIC_SCRIPT_VERSION_LIST = "/api/job/v3/job-manage/get_public_script_version_list";
    String GET_SCRIPT_VERSION_DETAIL = "/api/job/v3/job-manage/get_script_version_detail";
    String UPDATE_SCRIPT_VERSION = "/api/job/v3/job-manage/update_script_version";
    String UPDATE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/update_public_script_version";
    String DELETE_SCRIPT_VERSION = "/api/job/v3/job-manage/delete_script_version";
    String DELETE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/delete_public_script_version";
    String PUBLISH_SCRIPT_VERSION = "/api/job/v3/job-manage/publish_script_version";
    String PUBLISH_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/publish_public_script_version";
    String DISABLE_SCRIPT_VERSION = "/api/job/v3/job-manage/disable_script_version";
    String DISABLE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/disable_public_script_version";

    String CREATE_DANGEROUS_RULE = "/api/job/v3/job-manage/create_dangerous_rule";
    String DELETE_DANGEROUS_RULE = "/api/job/v3/job-manage/delete_dangerous_rule";
    String GET_DANGEROUS_RULE_LIST = "/api/job/v3/job-manage/get_dangerous_rule_list";
    String UPDATE_DANGEROUS_RULE = "/api/job/v3/job-manage/update_dangerous_rule";
    String ENABLE_DANGEROUS_RULE = "/api/job/v3/job-manage/enable_dangerous_rule";
    String DISABLE_DANGEROUS_RULE = "/api/job/v3/job-manage/disable_dangerous_rule";
    String CHECK_SCRIPT = "/api/job/v3/job-manage/check_script";

    String FAST_EXECUTE_SCRIPT = "/api/job/v3/job-execute/get_script_list";
    String FAST_TRANSFER_FILE = "/api/job/v3/job-execute/get_script_list";
    String EXECUTE_JOB_PLAN = "/api/job/v3/job-execute/get_script_list";
}
