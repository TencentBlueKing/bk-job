package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ListUtilTest {

    @Test
    void testIsAllTrue() {
        List<Boolean> allTrueList = new ArrayList<>();
        assertThat(ListUtil.isAllTrue(allTrueList)).isTrue();
        allTrueList.add(true);
        allTrueList.add(true);
        allTrueList.add(true);
        assertThat(ListUtil.isAllTrue(allTrueList)).isTrue();
        List<Boolean> notAllTrueList = new ArrayList<>();
        notAllTrueList.add(true);
        notAllTrueList.add(false);
        notAllTrueList.add(false);
        assertThat(ListUtil.isAllTrue(notAllTrueList)).isFalse();
    }
}
