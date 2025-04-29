package com.tencent.bk.job.common.redis.aspect;

import com.tencent.bk.job.common.properties.JobSslProperties;
import com.tencent.bk.job.common.redis.config.JobRedisSslProperties;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SslVerifyMode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 由于Spring Data Redis 2.x版本不支持自定义SslVerifyMode为CA模式，但底层lettuce-core是支持的，
 * 因此在该类中借助Spring AOP能力修改LettuceConnectionFactory的方法，达到设置SslVerifyMode为CA模式的目的
 * 后续升级Spring Boot 3.x框架后，可使用Spring Data Redis 3.x版本的原生配置能力实现。
 */
@Slf4j
@Aspect
public class SetSslVerifyModeToCAAspect {

    private final JobSslProperties sslProperties;

    public SetSslVerifyModeToCAAspect(JobRedisSslProperties jobRedisSslProperties) {
        this.sslProperties = jobRedisSslProperties.getRedis();
    }

    @Pointcut("execution (* org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory"
        + ".createRedisURIAndApplySettings(..))")
    public void processRedisURI() {
    }

    @Around("processRedisURI()")
    public Object setSslVerifyModeToCAByProperties(ProceedingJoinPoint pjp) throws Exception {
        try {
            Object redisUriObj = pjp.proceed();
            if (!sslProperties.isEnabled()) {
                log.debug("Redis ssl is not enabled, setSslVerifyModeToCAByProperties ignore");
                return redisUriObj;
            }
            if (sslProperties.isVerifyHostname()) {
                log.info("Redis ssl has to verify hostname, setSslVerifyModeToCAByProperties ignore");
                return redisUriObj;
            }
            // Redis连接启用SSL并且不校验主机名称，将校验模式设置为仅校验CA模式
            if (!(redisUriObj instanceof RedisURI)) {
                log.warn(
                    "unexpected ProceedingJoinPoint: redisUri type is {}, which should be RedisURI, please check",
                    redisUriObj.getClass().getName()
                );
                return pjp.proceed();
            }
            RedisURI redisUri = (RedisURI) redisUriObj;
            redisUri.setVerifyPeer(SslVerifyMode.CA);
            log.info("Redis sslVerifyMode set: {}", redisUri.getVerifyMode());
            return redisUri;
        } catch (Throwable throwable) {
            throw new Exception("Fail to execute setSslVerifyModeToCAByProperties", throwable);
        }
    }
}
