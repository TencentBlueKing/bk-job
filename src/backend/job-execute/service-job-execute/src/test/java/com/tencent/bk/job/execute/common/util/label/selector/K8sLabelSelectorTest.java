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

package com.tencent.bk.job.execute.common.util.label.selector;

import com.tencent.bk.job.execute.util.label.selector.LabelSelectorParse;
import com.tencent.bk.job.execute.util.label.selector.LabelSelectorParseException;
import com.tencent.bk.job.execute.util.label.selector.Operator;
import com.tencent.bk.job.execute.util.label.selector.Requirement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class K8sLabelSelectorTest {

    @Test
    void testSelectorParse() {
        String[] testGoodStrings = {
            "x=a,y=b,z=c",
            "",
            "x!=a,y=b",
            "x=",
            "x= ",
            "x=,z= ",
            "x= ,z= ",
            "!x",
            "x>1",
            "x>1,z<5",
        };
        String[] testBadStrings = {
            "x=a||y=b",
            "x==a==b",
            "!x=a",
            "x<a",
        };
        for (String test : testGoodStrings) {
            assertDoesNotThrow(() -> LabelSelectorParse.parse(test));
        }
        for (String test : testBadStrings) {
            assertThrows(LabelSelectorParseException.class, () -> LabelSelectorParse.parse(test));
        }
    }

    @Test
    void testParseToRequirements() {
        expectMatch(LabelSelectorParse.parse("app=nginx"),
            Requirements.build(newRequirement("app", Operator.Equals, "nginx")));
        expectMatch(LabelSelectorParse.parse("app==nginx"),
            Requirements.build(newRequirement("app", Operator.DoubleEquals, "nginx")));
        expectMatch(LabelSelectorParse.parse("version==1.0"),
            Requirements.build(newRequirement("version", Operator.DoubleEquals, "1.0")));
        expectMatch(LabelSelectorParse.parse("version=1.0"),
            Requirements.build(newRequirement("version", Operator.Equals, "1.0")));


        expectMatch(LabelSelectorParse.parse("app!=nginx"),
            Requirements.build(newRequirement("app", Operator.NotEquals, "nginx")));

        expectMatch(LabelSelectorParse.parse("app in (nginx, redis)"),
            Requirements.build(newRequirement("app", Operator.In, new String[]{"nginx", "redis"})));
        expectMatch(LabelSelectorParse.parse("version in (1.0, 2.0)"),
            Requirements.build(newRequirement("version", Operator.In, new String[]{"1.0", "2.0"})));

        expectMatch(LabelSelectorParse.parse("app notin (nginx, redis)"),
            Requirements.build(newRequirement("app", Operator.NotIn, new String[]{"nginx", "redis"})));

        expectMatch(LabelSelectorParse.parse("app"),
            Requirements.build(newRequirement("app", Operator.Exists)));
        expectMatch(LabelSelectorParse.parse("release-version"),
            Requirements.build(newRequirement("release-version", Operator.Exists)));

        expectMatch(LabelSelectorParse.parse("!app"),
            Requirements.build(newRequirement("app", Operator.DoesNotExist)));

        expectMatch(LabelSelectorParse.parse("version > 10"),
            Requirements.build(newRequirement("version", Operator.GreaterThan, "10")));
        expectMatch(LabelSelectorParse.parse("version < 10"),
            Requirements.build(newRequirement("version", Operator.LessThan, "10")));

        expectMatch(LabelSelectorParse.parse("app=nginx, tier=frontend"),
            Requirements.build(
                newRequirement("app", Operator.Equals, "nginx"),
                newRequirement("tier", Operator.Equals, "frontend"))
        );
        expectMatch(LabelSelectorParse.parse("env=production,version!=1.0"),
            Requirements.build(
                newRequirement("env", Operator.Equals, "production"),
                newRequirement("version", Operator.NotEquals, "1.0"))
        );
        expectMatch(LabelSelectorParse.parse("app in (nginx, redis),tier=backend"),
            Requirements.build(
                newRequirement("app", Operator.In, new String[]{"nginx", "redis"}),
                newRequirement("tier", Operator.Equals, "backend"))
        );
        expectMatch(LabelSelectorParse.parse("!beta-version,app!=mysql"),
            Requirements.build(
                newRequirement("beta-version", Operator.DoesNotExist),
                newRequirement("app", Operator.NotEquals, "mysql"))
        );

        expectMatch(LabelSelectorParse.parse("val in (in, notin)"),
            Requirements.build(
                newRequirement("val", Operator.In, new String[]{"in", "notin"}))
        );
    }

    private Requirement newRequirement(String key, Operator op, String val) {
        return new Requirement(key, op, Collections.singletonList(val));
    }

    private Requirement newRequirement(String key, Operator op) {
        return new Requirement(key, op, null);
    }

    private Requirement newRequirement(String key, Operator op, String[] vals) {
        return new Requirement(key, op, Arrays.asList(vals));
    }

    private void expectMatch(List<Requirement> actualRequirementList,
                             Requirements expectRequirements) {
        actualRequirementList.sort(new ByKey());
        expectRequirements.requirements.sort(new ByKey());
        assertEquals(actualRequirementList.size(), expectRequirements.requirements.size());
        for (int i = 0; i < actualRequirementList.size(); i++) {
            Requirement actual = actualRequirementList.get(i);
            Requirement expect = expectRequirements.requirements.get(i);
            assertEquals(actual, expect);
        }
    }

    private static class Requirements {
        private final List<Requirement> requirements = new ArrayList<>();

        public static Requirements build(Requirement... requirementArgs) {
            Requirements requirements = new Requirements();
            for (Requirement requirement : requirementArgs) {
                requirements.add(requirement);
            }
            return requirements;
        }

        public void add(Requirement requirement) {
            requirements.add(requirement);
        }
    }

    private static class ByKey implements Comparator<Requirement> {
        @Override
        public int compare(Requirement o1, Requirement o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    }
}
