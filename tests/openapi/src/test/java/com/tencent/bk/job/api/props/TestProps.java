package com.tencent.bk.job.api.props;

import com.tencent.bk.job.api.exception.TestInitialException;
import com.tencent.bk.job.api.v3.model.HostDTO;
import org.junit.platform.commons.util.StringUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class TestProps {
    private static final Properties props = new Properties();

    static {
        load();
    }

    private static void load() {
        try {
            InputStream in;
            String apiTestPropertyFile = System.getProperty("apiTestPropertyFile");
            if (StringUtils.isNotBlank(apiTestPropertyFile)) {
                in = new FileInputStream(apiTestPropertyFile);
            } else {
                in = TestProps.class.getResourceAsStream("/test.properties");
            }
            props.load(in);

            String httpProxyEnable = System.getProperty("http.proxy.enable");
            if (StringUtils.isNotBlank(httpProxyEnable)) {
                props.put("http.proxy.enable", httpProxyEnable);
            }
            String httpProxy = System.getProperty("http.proxy");
            if (StringUtils.isNotBlank(httpProxy)) {
                props.put("http.proxy", httpProxy);
            }
            String httpProxyPort = System.getProperty("http.proxy.port");
            if (StringUtils.isNotBlank(httpProxyPort)) {
                props.put("http.proxy.port", httpProxyPort);
            }
        } catch (Exception e) {
            throw new TestInitialException(e);
        }
    }


    // ESB设置
    public static final String APP_CODE = getPropString("app.code");
    public static final String API_HOST_BASE_URL = getPropString("api.host.base.url");
    public static final String ESB_PRIVATE_KEY_BASE64 = getPropString("esb.private.key.base64");

    // http proxy 配置
    public static final boolean IS_PROXY_ENABLE = getPropBoolean("http.proxy.enable", false);
    public static final String HTTP_PROXY = getPropString("http.proxy");
    public static final Integer HTTP_PROXY_PORT = getPropInteger("http.proxy.port");

    // 测试数据配置
    // 定义用于测试的服务器
    // 静态IP
    // 默认测试业务下的主机 1
    public static final HostDTO HOST_1_DEFAULT_BIZ = new HostDTO(
        Long.valueOf(getPropString("host.1.default.biz").split(":")[0]),
        Long.valueOf(getPropString("host.1.default.biz").split(":")[1]),
        getPropString("host.1.default.biz").split(":")[2]);
    // 默认测试业务下的主机 2
    public static final HostDTO HOST_2_DEFAULT_BIZ = new HostDTO(
        Long.valueOf(getPropString("host.2.default.biz").split(":")[0]),
        Long.valueOf(getPropString("host.2.default.biz").split(":")[1]),
        getPropString("host.2.default.biz").split(":")[2]);
    // 其他业务下的主机 1
    public static final HostDTO HOST_1_BIZ_2 = new HostDTO(
        Long.valueOf(getPropString("host.1.biz.2").split(":")[0]),
        getPropString("host.1.biz.2").split(":")[1]);
    // 白名单主机 1
    public static final HostDTO HOST_1_WHITE_LIST = new HostDTO(
        Long.valueOf(getPropString("host.1.white.list").split(":")[0]),
        getPropString("host.1.white.list").split(":")[1]);
    // 动态分组
    public static final String DYNAMIC_GROUP_ID = getPropString("dynamic.group.id");
    // 拓扑-模块 ID
    public static final Integer TOPO_MODULE_ID = getPropInteger("topo.module.id");
    // 拓扑-集群 ID
    public static final Integer TOPO_SET_ID = getPropInteger("topo.set.id");

    // 定义用于测试的业务
    public static final Long DEFAULT_BIZ = getPropLong("biz.default.id");
    public static final Long BIZ_2 = getPropLong("biz.2.id");
    public static final Long DEFAULT_BIZ_SET = getPropLong("biz_set.default.id");

    // 定义测试的用户
    public static final String DEFAULT_TEST_USER = getPropString("test.user.default");

    // 用于测试的默认系统账号别名
    public static final String DEFAULT_OS_ACCOUNT_ALIAS = getPropString("os.account.alias.default");

    public static Integer getPropInteger(String key) {

        String value = props.getProperty(key);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Integer.valueOf(value);
    }

    public static Long getPropLong(String key) {
        String value = props.getProperty(key);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Long.valueOf(value);
    }

    public static String getPropString(String key) {
        return props.getProperty(key);
    }

    public static boolean getPropBoolean(String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
