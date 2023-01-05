package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FilePathValidateUtilTest {
    @Test
    void testFileSystemAbsolutePath(){
        // DOS设备路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\.\\C:\\Test\\Foo.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\?\\C:\\Test\\Foo.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\Test\\Foo.txt")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\?\\C:\\Te st\\嘉Foo.txt")).isTrue();

        // UNC路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\system07\\C$\\")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\Server2\\Share\\Test\\Foo.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\Server201\\C$\\Test1\\Fo o.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("\\\\system07")).isFalse();

        // 传统DOS路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("c:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\嘉 abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath(":\\abc.txt")).isFalse();

        // linux路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/data/test_2022-04-12.apk")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/data/test_2022 04 12.apk")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("data/test_2022-04-12.apk")).isFalse();
    }

}
