package com.tencent.bk.job.common.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        // 支持 java.time.LocalDateTime 序列化与反序列化
        return builder -> builder.serializers(LocalDateTimeSerializer.INSTANCE).build();
    }}
