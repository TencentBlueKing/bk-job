/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.file.worker;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.model.resp.common.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

@DisplayName("模型集成测试")
public class ModelTest {

    @Test
    @DisplayName("测试JSON序列化")
    public void testJsonSerilize() {
        FileTreeNodeDef fileTreeNodeDef = new FileTreeNodeDef();
        fileTreeNodeDef.setNodeType("Node");
        fileTreeNodeDef.setDescription("制品库-节点");
        List<FileTreeNodeProperty> properties = new ArrayList<>();
        fileTreeNodeDef.setProperties(properties);

        FileTreeNodeTextProperty property = new FileTreeNodeTextProperty();
        property.setLabel("名称");
        property.setLabelEn("Name");
        property.setField("name");
        property.setType("text");
        FileLinkAction action = new FileLinkAction();
        action.setType("FILE_LINK");
        action.setTarget("${path}/${name}");
        List<PropertyAction> actions = new ArrayList<>();
        actions.add(action);
        property.setActions(actions);
        properties.add(property);

        property = new FileTreeNodeTextProperty();
        property.setLabel("大小");
        property.setLabelEn("Size");
        property.setField("size");
        property.setType("text");
        properties.add(property);

        property = new FileTreeNodeTextProperty();
        property.setLabel("更新人");
        property.setLabelEn("LastModifiedBy");
        property.setField("lastModifiedBy");
        property.setType("text");
        properties.add(property);

        property = new FileTreeNodeTextProperty();
        property.setLabel("最近更新时间");
        property.setLabelEn("LastModifiedBy");
        property.setField("lastModifiedBy");
        property.setType("text");
        properties.add(property);


        property = new FileTreeNodeTextProperty();
        actions = new ArrayList<>();
        // Button1
        FileLinkAction action1 = new FileLinkAction();
        action1.setType("FILE_LINK");
        action1.setTarget("${path}/${name}");
        CompositeExpression compositeExpression = new CompositeExpression();
        compositeExpression.setOperation(OperationEnum.AND);
        List<Expression> expressions = new ArrayList<>();
        expressions.add(new Expression("folder", true));
        compositeExpression.setExpressions(expressions);
        action1.setRely(compositeExpression);
        actions.add(action1);
        // Button2
        WorkerAction action2 = new WorkerAction();
        action2.setLabel("删除");
        action2.setLabelEn("Delete");
        action2.setType("WORKER_ACTION");
        action2.setActionCode("DELETE_REPO");
        action2.setParams("{\"projectId\":\"${projectId}\",\"repoName\":\"${repoName}\",\"fullPath\":\"${fullPath}\"}");
        actions.add(action2);
        property.setActions(actions);
        properties.add(property);

        System.out.println(JsonUtils.toJson(fileTreeNodeDef));
    }
}
