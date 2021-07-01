package com.tencent.bk.job.upgrader.task.param;

public class ParamNameConsts {
    // 需要用户手动输入的参数
    // 集群中任意一个job-manage实例对应的IP加端口地址，格式：ip:port，例如：127.0.0.1:10505
    public static final String INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS = "job.manage.server.address";

    // 自动从配置文件读取的参数
    public static final String CONFIG_PROPERTY_APP_CODE = "app.code";
    public static final String CONFIG_PROPERTY_APP_SECRET = "app.secret";
    public static final String CONFIG_PROPERTY_IAM_BASE_URL = "iam.base-url";
    public static final String CONFIG_PROPERTY_ESB_SERVICE_URL = "esb.service.url";
    public static final String CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64 = "job.security.public-key-base64";
    public static final String CONFIG_PROPERTY_JOB_ENCRYPT_PASSWORD = "job.encrypt.password";
}
