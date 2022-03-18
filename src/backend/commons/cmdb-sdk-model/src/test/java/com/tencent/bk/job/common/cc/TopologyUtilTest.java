package com.tencent.bk.job.common.cc;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopologyUtilTest {

    @Test
    void testPrint() {
        InstanceTopologyDTO appTopology = new InstanceTopologyDTO();
        appTopology.setObjectId("biz");
        appTopology.setInstanceId(2L);
        List<InstanceTopologyDTO> childList = new ArrayList<>();
        InstanceTopologyDTO child1 = new InstanceTopologyDTO();
        child1.setObjectId("set");
        child1.setInstanceId(3L);
        childList.add(child1);
        InstanceTopologyDTO child2 = new InstanceTopologyDTO();
        child2.setObjectId("set");
        child2.setInstanceId(4L);
        InstanceTopologyDTO child3 = new InstanceTopologyDTO();
        child3.setObjectId("module");
        child3.setInstanceId(5L);
        child2.setChild(Collections.singletonList(child3));
        childList.add(child2);
        appTopology.setChild(childList);
        InstanceTopologyDTO child4 = new InstanceTopologyDTO();
        child4.setObjectId("set");
        child4.setInstanceId(6L);
        InstanceTopologyDTO child5 = new InstanceTopologyDTO();
        child5.setObjectId("module");
        child5.setInstanceId(7L);
        child4.setChild(Collections.singletonList(child5));
        childList.add(child4);
        appTopology.setChild(childList);
        TopologyUtil.printTopo(appTopology);
    }
}
