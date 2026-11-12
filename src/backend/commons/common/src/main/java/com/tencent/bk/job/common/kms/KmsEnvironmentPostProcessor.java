/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.kms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ENABLE_KMS 为 true 时，按凭证短名解密信封并写入 Environment。未开启则不做任何事。
 */
public class KmsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered,
    ApplicationListener<ApplicationContextInitializedEvent> {

    static final String PROPERTY_SOURCE_NAME = "bkKmsDecryptedPropertySource";

    private static final DeferredLog LOG = new DeferredLog();
    private static final String CREDENTIAL_NAME_PATTERN = "[a-z0-9][a-z0-9-]*";

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!"true".equalsIgnoreCase(environment.getProperty("ENABLE_KMS"))) {
            return;
        }
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }
        String credentials = environment.getProperty("BK_JOB_KMS_CREDENTIALS");
        if (credentials == null || credentials.isBlank()) {
            throw new IllegalStateException("BK_JOB_KMS_CREDENTIALS is required when ENABLE_KMS=true");
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        for (String rawName : credentials.split(",")) {
            String shortName = rawName.trim();
            if (shortName.isEmpty()) {
                continue;
            }
            if (!shortName.matches(CREDENTIAL_NAME_PATTERN)) {
                throw new IllegalStateException("invalid KMS credential short name: " + shortName);
            }
            decryptOne(environment, shortName, merged);
        }
        if (merged.isEmpty()) {
            throw new IllegalStateException("ENABLE_KMS=true but no credential was decrypted");
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, merged));
        LOG.info("BK-KMS decrypted " + merged.size() + " properties from " + credentials);
    }

    private void decryptOne(ConfigurableEnvironment environment, String shortName, Map<String, Object> merged) {
        String privateKeyName = "BK_JOB_KMS_PRIVATE_KEY_"
            + shortName.toUpperCase(Locale.ROOT).replace('-', '_');
        String privateKey = environment.getProperty(privateKeyName);
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException(privateKeyName + " is required to decrypt credential " + shortName);
        }
        Path envelopePath = Path.of("/etc/kms", shortName, "envelope");
        if (!Files.isRegularFile(envelopePath)) {
            throw new IllegalStateException("KMS envelope file not found: " + envelopePath);
        }
        String envelope;
        try {
            envelope = Files.readString(envelopePath, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("failed to read KMS envelope: " + envelopePath, e);
        }
        if (envelope.isEmpty()) {
            throw new IllegalStateException("KMS envelope is empty: " + envelopePath);
        }
        Map<String, String> properties = BkKmsDecryptor.decrypt(privateKey.trim(), envelope);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (merged.containsKey(entry.getKey())) {
                throw new IllegalStateException(
                    "duplicate KMS property from credential " + shortName + ": " + entry.getKey());
            }
            merged.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void onApplicationEvent(ApplicationContextInitializedEvent event) {
        LOG.replayTo(KmsEnvironmentPostProcessor.class);
    }
}
