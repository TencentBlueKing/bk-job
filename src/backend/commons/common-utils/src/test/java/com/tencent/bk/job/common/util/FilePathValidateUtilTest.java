package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FilePathValidateUtilTest {
    @Test
    void testFileSystemAbsolutePath(){
        // 传统DOS路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("c:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\嘉 abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath(":\\abc.txt")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\\\")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\logs\\..\\access.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\.config\\conf")).isTrue();

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

        // linux路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/data/test_2022-04-12.apk")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/data/test_2022 04 12.apk")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/.conf/abc")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/test/../test.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("data/test_2022-04-12.apk")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("///")).isFalse();
    }

}
