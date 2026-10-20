package com.tencent.bk.job.file.worker.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.IncorrectConfigException;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class EnvironmentService implements ApplicationContextAware {

    private static final String ENV_POD_IP = "BK_JOB_POD_IP";

    private ApplicationContext applicationContext;
    private final WorkerConfig workerConfig;

    public EnvironmentService(WorkerConfig workerConfig) {
        this.workerConfig = workerConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public boolean isInK8s() {
        String[] profileArr = applicationContext.getEnvironment().getActiveProfiles();
        for (String profile : profileArr) {
            if (JobConstants.PROFILE_KUBERNETES.equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private String getAccessHostInK8s() {
        String podName = System.getenv("BK_JOB_POD_NAME");
        if (StringUtils.isBlank(podName)) {
            String message = "ENV BK_JOB_POD_NAME cannot be blank!";
            throw new IncorrectConfigException(
                message,
                ErrorCode.INVALID_CONFIG,
                new String[]{"ENV:BK_JOB_POD_NAME"}
            );
        }
        String fileWorkerServiceName = System.getenv("BK_JOB_FILE_WORKER_SERVICE_NAME");
        if (StringUtils.isBlank(fileWorkerServiceName)) {
            String message = "ENV BK_JOB_FILE_WORKER_SERVICE_NAME cannot be blank!";
            throw new IncorrectConfigException(
                message,
                ErrorCode.INVALID_CONFIG,
                new String[]{"ENV:BK_JOB_FILE_WORKER_SERVICE_NAME"}
            );
        }
        String namespace = System.getenv("KUBERNETES_NAMESPACE");
        if (StringUtils.isBlank(namespace)) {
            String message = "ENV KUBERNETES_NAMESPACE cannot be blank!";
            throw new IncorrectConfigException(
                message,
                ErrorCode.INVALID_CONFIG,
                new String[]{"ENV:KUBERNETES_NAMESPACE"}
            );
        }
        String accessHost = podName + "." + fileWorkerServiceName + "." + namespace;
        log.debug("accessHost={}", accessHost);
        return accessHost;
    }

    private String getInnerIpInK8s() {
        String nodeIP = System.getenv("BK_JOB_NODE_IP");
        String podIp = IpUtils.getFirstMachineIP();
        log.info("nodeIP={}", nodeIP);
        if (!StringUtils.isBlank(nodeIP)) {
            return nodeIP;
        } else {
            return podIp;
        }
    }

    public String getAccessHost() {
        String accessHost;
        if (isInK8s()) {
            accessHost = getAccessHostInK8s();
        } else {
            accessHost = workerConfig.getAccessHost();
        }
        if (StringUtils.isBlank(accessHost)) {
            accessHost = IpUtils.getFirstMachineIP();
            log.debug("accessHost is blank, use first machine ip");
        }
        return accessHost;
    }

    /**
     * 获取 Worker 当前实际 IP 列表，供 Gateway 校验其 DNS 解析结果是否已指向最新地址。
     * <ul>
     *     <li>K8s 环境：优先取 Helm Chart 注入的 Pod IP（{@code BK_JOB_POD_IP}，fieldRef: status.podIP），
     *     未注入时回退为本机网卡地址。注意不能使用 {@code BK_JOB_NODE_IP}（节点 IP）</li>
     *     <li>二进制部署：本机网卡地址，accessHost 本身为 IP 字面量时一并加入</li>
     * </ul>
     *
     * @return IP 列表；获取不到任何 IP 时返回 null，Gateway 退化为仅校验可解析
     */
    public List<String> getExpectedIps() {
        Set<String> ips = new LinkedHashSet<>();
        if (isInK8s()) {
            String podIpEnv = getEnv(ENV_POD_IP);
            String podIp = StringUtils.isBlank(podIpEnv) ? null : podIpEnv.trim();
            if (IpUtils.isValidIpAddress(podIp)) {
                ips.add(podIp);
            } else {
                log.info("ENV {} is not a valid ip: {}, use machine ips", ENV_POD_IP, podIp);
                ips.addAll(listMachineIps());
            }
        } else {
            ips.addAll(listMachineIps());
            String accessHost = StringUtils.isBlank(workerConfig.getAccessHost())
                ? null : workerConfig.getAccessHost().trim();
            if (IpUtils.isValidIpAddress(accessHost)) {
                ips.add(accessHost);
            }
        }
        if (ips.isEmpty()) {
            log.warn("Cannot get any ip of current worker, gateway will only check whether access host is resolvable");
            return null;
        }
        return new ArrayList<>(ips);
    }

    String getEnv(String name) {
        return System.getenv(name);
    }

    List<String> listMachineIps() {
        return IpUtils.listMachineIps();
    }

    public Pair<String, String> getInnerProtocolAndIp() {
        String innerIpProtocol;
        String innerIp;
        if (isInK8s()) {
            innerIp = getInnerIpInK8s();
        } else {
            innerIp = workerConfig.getInnerIp().trim();
        }
        if (StringUtils.isBlank(innerIp)) {
            innerIp = IpUtils.getFirstMachineIP();
            log.debug("innerIp is blank, use first machine ip");
        }
        String protocolInferredByIp = IpUtils.inferProtocolByIp(innerIp);
        if (StringUtils.isNotBlank(workerConfig.getInnerIpProtocol())) {
            innerIpProtocol = workerConfig.getInnerIpProtocol().trim();
            if (!protocolInferredByIp.equalsIgnoreCase(innerIpProtocol)) {
                log.warn(
                    "innerIp({}) protocol specified by job.file-worker.inner-ip-protocol={} " +
                        "not match protocolInferredByIp({}), use {}",
                    innerIp,
                    innerIpProtocol,
                    protocolInferredByIp,
                    innerIpProtocol
                );
            }
        } else {
            innerIpProtocol = protocolInferredByIp;
        }
        // 将IPv6地址转为完整无压缩格式
        if (IpUtils.PROTOCOL_IP_V6.equalsIgnoreCase(innerIpProtocol)) {
            innerIp = IpUtils.getFullIpv6ByCompressedOne(innerIp);
        }
        return Pair.of(innerIpProtocol, innerIp);
    }
}
