package com.tencent.bk.job.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;

@Getter
public class BaseEsbReq {
    public String toUrlParams() {
        StringBuilder urlString = new StringBuilder(512);
        char c = '&';
        Class<?> aClass = getClass();
        while (aClass != null) {
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field field : declaredFields) {
                String key;
                JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                if (annotation != null) {
                    key = annotation.value();
                } else {
                    key = field.getName();
                }
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                    try {
                        Object b = field.get(this);
                        if (b != null) {
                            urlString.append(c).append(key).append('=').append(urlEncode(b.toString()));
                        }
                    } catch (IllegalAccessException ignored) {
                    } finally {
                        field.setAccessible(false);
                    }
                }
            }
            aClass = aClass.getSuperclass();
        }
        if (urlString.toString().length() > 0) {
            return "?" + urlString.toString().substring(1);
        } else {
            return "?";
        }
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encode failed");
        }
    }
}
