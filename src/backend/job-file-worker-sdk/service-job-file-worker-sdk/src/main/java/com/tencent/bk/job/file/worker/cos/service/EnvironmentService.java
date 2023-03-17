package com.tencent.bk.job.file.worker.cos.service;

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

@Slf4j
@Service
public class EnvironmentService implements ApplicationContextAware {

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
        String podName = System.getenv("BK_JOB_FILE_WORKER_POD_NAME");
        if (StringUtils.isBlank(podName)) {
            String message = "ENV BK_JOB_FILE_WORKER_POD_NAME cannot be blank!";
            throw new IncorrectConfigException(
                message,
                ErrorCode.INVALID_CONFIG,
                new String[]{"ENV:BK_JOB_FILE_WORKER_POD_NAME"}
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
        String accessHost = podName + "." + fileWorkerServiceName;
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
