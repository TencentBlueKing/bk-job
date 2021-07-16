package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomCollectionUtilsTest {

    @Test
    void testMergeList() {
        List<Long> list1 = Arrays.asList(1L, 2L);
        List<Long> list2 = Arrays.asList(1L, 3L, 4L);
        assertThat(CustomCollectionUtils.mergeList(null, null)).isNull();
        assertThat(CustomCollectionUtils.mergeList(list1, null)).hasSize(2);
        assertThat(CustomCollectionUtils.mergeList(null, list2)).hasSize(3);
        List<Long> mergedList = CustomCollectionUtils.mergeList(list1, list2);
        assertThat(mergedList).hasSize(4);
        mergedList.forEach(System.out::println);
    }
}
