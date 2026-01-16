package com.tencent.bk.job.gateway.web.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
    value = "job.gateway.customAccessLog.enabled",
    havingValue = "true"
)
public @interface ConditionalOnCustomAccessLogEnabled {
}
