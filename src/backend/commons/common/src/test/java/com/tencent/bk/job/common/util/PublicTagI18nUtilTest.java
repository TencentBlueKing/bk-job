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

package com.tencent.bk.job.common.util;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublicTagI18nUtilTest {

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(I18nUtil.class, "i18nService", new TestMessageI18nService());
    }

    @AfterEach
    public void tearDown() {
        ReflectionTestUtils.setField(I18nUtil.class, "i18nService", null);
    }

    @Test
    public void getI18nName() {
        assertEquals("Continuous Integration", PublicTagI18nUtil.getI18nName("持续集成"));
        assertEquals("业务自定义", PublicTagI18nUtil.getI18nName("业务自定义"));
        assertEquals("", PublicTagI18nUtil.getI18nName(""));
    }

    @Test
    public void matchesName() {
        assertTrue(PublicTagI18nUtil.matchesName("持续集成", "持续"));
        assertTrue(PublicTagI18nUtil.matchesName("持续集成", "integration"));
        assertTrue(PublicTagI18nUtil.matchesName("持续集成", "Integration"));
        assertFalse(PublicTagI18nUtil.matchesName("持续集成", "deploy"));
        assertFalse(PublicTagI18nUtil.matchesName(null, "integration"));
        assertTrue(PublicTagI18nUtil.matchesName("持续集成", null));
    }

    private static class TestMessageI18nService implements MessageI18nService {
        private static final Map<String, String> MESSAGE_MAP = Map.of(
            "common.public.tag.continuousIntegration", "Continuous Integration",
            "common.public.tag.testing", "Testing",
            "common.public.tag.commonTools", "Common Tools",
            "common.public.tag.releaseDeployment", "Release & Deployment",
            "common.public.tag.troubleshooting", "Troubleshooting",
            "common.public.tag.opsRelease", "Operations Release"
        );

        @Override
        public String getI18n(String msgKey) {
            return MESSAGE_MAP.getOrDefault(msgKey, "");
        }

        @Override
        public String getI18nWithArgs(String msgKey, Object... args) {
            return getI18n(msgKey);
        }

        @Override
        public String getI18n(Locale locale, String msgKey) {
            return getI18n(msgKey);
        }

        @Override
        public String getI18nWithArgs(Locale locale, String msgKey, Object... args) {
            return getI18n(msgKey);
        }
    }
}
