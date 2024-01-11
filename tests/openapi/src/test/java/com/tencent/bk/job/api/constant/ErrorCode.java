package com.tencent.bk.job.api.constant;

/**
 * 蓝鲸标准化错误码
 */
public class ErrorCode {

    //==================================== 通用 ===================================================//
    /**
     * 成功
     */
    public static final int RESULT_OK = 0;

    //==================================== 业务级错误 ================================================//
    /*
     * 业务通用-1241xxx
     * 配置服务-1242xxx
     * 作业管理-1243xxx
     * 作业执行-1244xxx
     * 定时作业-1245xxx
     * 日志服务-1246xxx
     * 用户服务-1247xxx
     * 业务网关-1248xxx
     */

    public static final int MISSING_PARAM = 1241001; //请求参数缺失
    public static final int ILLEGAL_PARAM = 1241002; //请求参数不合法
    public static final int UNSUPPORTED_OPERATION = 1241003; //不支持的操作
    public static final int MISSING_PARAM_WITH_PARAM_NAME = 1241004; //请求参数[]缺失
    public static final int ILLEGAL_PARAM_WITH_PARAM_NAME = 1241005; //请求参数[]不合法
    public static final int MISSING_OR_ILLEGAL_PARAM = 1241006; //请求参数缺失或不合法
    public static final int MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME = 1241007; //请求参数[]缺失或不合法
    public static final int ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON = 1241010; // 请求参数[0]不合法，原因：[1]


    public static final int SCRIPT_NOT_EXIST = 1243001;//脚本不存在
    public static final int SCRIPT_NAME_DUPLICATE = 1243002;//脚本名称已存在
    public static final int SCRIPT_NAME_INVALID = 1243003;//脚本名称不合法
    public static final int SCRIPT_NOT_IN_APP = 1243004;//脚本不属于该业务
    public static final int DELETE_ONLINE_SCRIPT_FAIL = 1243005;//已上线脚本不支持删除
    public static final int DELETE_OFFLINE_SCRIPT_FAIL = 1243006;//已下线脚本不支持删除
    public static final int ACCOUNT_ALIAS_EXIST = 1243007;//账号别名已存在
    public static final int DB_SYSTEM_ACCOUNT_IS_INVALID = 1243008;//DB依赖的系统账号不合法
    public static final int TASK_PLAN_NOT_EXIST = 1243009;//执行方案不存在
    public static final int UPLOAD_SCRIPT_FILE_NAME_EMPTY = 1243010;//上传脚本文件名为空
    public static final int UPLOAD_SCRIPT_EXT_TYPE_ILLEGAL = 1243011;//上传脚本文件扩展名不合法
    public static final int UPLOAD_SCRIPT_CONTENT_ILLEGAL = 1243012;//上传脚本文件脚本内容不合法
    public static final int SCRIPT_VERSION_NAME_EXIST = 1243036;//脚本版本号已存在

    public static final int SERVER_EMPTY = 1244001;//主机为空
    public static final int SERVER_UNREGISTERED = 1244002;//主机未注册:{}
    public static final int ACCOUNT_NOT_EXIST = 1244003;//账号不存在
    public static final int ACCOUNT_NO_PERMISSION = 1244004;//没有该账号的权限
    public static final int STARTUP_TASK_FAIL = 1244005;//启动作业失败
    public static final int TASK_INSTANCE_NOT_EXIST = 1244006;//作业实例不存在
    public static final int STEP_INSTANCE_NOT_EXIST = 1244007;//作业步骤实例不存在
    public static final int TASK_INSTANCE_RELATED_HOST_VAR_NOT_EXIST = 1244008;//作业引用的主机列表变量[{}]不存在
    public static final int TASK_INSTANCE_RELATED_HOST_VAR_SERVER_EMPTY = 1244009;//作业引用的主机列表变量[{}]的主机列表为空
    public static final int EXPORT_STEP_EXECUTION_LOG_FAIL = 1244010;//导出日志文件失败
    public static final int EXECUTE_TASK_PLAN_ILLEGAL = 1244011;//执行方案不合法
    public static final int EXECUTE_TASK_PLAN_NOT_EXIST = 1244012;//执行方案不存在
    public static final int DOWNLOAD_LOG_FILE_FAIL = 1244013;//下载执行日志文件失败

    public static final int GET_JOB_EXECUTION_LOG_FAIL = 1246001; // 获取作业执执行日志失败
    public static final int SAVE_JOB_EXECUTION_LOG_FAIL = 1246002; // 保存作业执执行日志失败
    public static final int DELETE_JOB_EXECUTION_LOG_FAIL = 1246003; // 删除作业执执行日志失败

    public static final int USER_NOT_EXIST_OR_NOT_LOGIN_IN = 1247001; // 用户不存在或者未登录


    //==================================== 系统级错误 ================================================//
    //======== 系统错误-权限错误 ==================//
    public static final int USER_NO_PERMISSION_COMMON = 1238001; //用户权限不足
    public static final int USER_NO_PERMISSION_APP = 1238002; //用户无业务操作权限

    //========= 系统错误-请求 ====================//
    public static final int SERVICE_UNAVAILABLE = 1240001;//服务不可用
    public static final int SERVICE_INTERNAL_ERROR = 1240002;//内部服务异常
    public static final int BAD_REQUEST = 1240003; //错误的请求
    public static final int COOKIE_ILLEGAL = 1240004;// Cookie过期或者不存在


    //========= 系统错误-API通用 ==================//
    public static final int API_IP_NO_ACCESS = 1239001; //IP:{}无访问权限
    public static final int API_USER_NO_ACCESS = 1239002; //用户无访问权限
    public static final int API_UNSUPPORTED = 1239003; //不支持的API接口
    public static final int API_PARAM_NULL_OR_ILLEGAL = 1239004; //缺少请求报文或报文不合法
    public static final int API_INVOKE_TIMEOUT = 1239005;//异步调用的方法超时了
    public static final int API_INVOKE_ERROR = 1239006;//调用方法出错了
    public static final int API_DEPRECATED = 1239007;   //API接口已经过期废弃
    public static final int API_OVER_LOAD = 1239008;//API服务过载，拒绝服务请求
    public static final int API_CALLBACK_FAIL = 1239009;//API回调其他接口失败
    public static final int API_CERT_ERROR = 1239010;//Api.cert文件不可用

    //======= 系统错误-公共组件错误 =======//
    public static final int REDIS_CONNECT_FAIL = 1250001;//Redis服务不可用，连接不上 - IP不存在或者配置错误
    public static final int REDIS_DATA_EXCEPTION = 1250002; //Redis服务内存满或者其他问题 - 内存不足够
    public static final int NFS_ERROR = 1259001;//NFS存储 不可用
    public static final int DB_ERROR = 1252001;//DB 不可用
    public static final int MQ_ERROR = 1255001;//MQ 不可用


    //======= 系统错误-平台服务错误 =======//
    public static final int LICENSE_ERROR = 1210101;//LICENSE 不可用
    public static final int GSE_ERROR = 1210001;//GSE 不可用
    public static final int CMDB_UNREACHABLE_SERVER = 1211001; //CMDB服务状态不可达 - 地址配置错误或者地址无法正确解析
    public static final int CMDB_API_DATA_ERROR = 1211002; //CMDB接口返回数据结构异常- 一般是被网关防火墙重定向返回统一登录页面
    public static final int PAAS_UNREACHABLE_SERVER = 1213001; //PAAS服务不可达 - 地址配置错误或者地址无法正确解析
    public static final int PAAS_API_DATA_ERROR = 1213002; //paas接口返回数据结构异常-一般是被网关防火墙重定向返回统一登录页面
    public static final int PAAS_MSG_CHANNEL_DATA_ERROR = 1213003; //paas通知渠道接口数据获取异常
}
