package com.devoxx.genie.service.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BucketsTest {

    @Test
    void standardBucketsCoverEveryBoundary() {
        assertThat(Buckets.standard(-1)).isEqualTo("0");
        assertThat(Buckets.standard(0)).isEqualTo("0");
        assertThat(Buckets.standard(1)).isEqualTo("1");
        assertThat(Buckets.standard(2)).isEqualTo("2-5");
        assertThat(Buckets.standard(5)).isEqualTo("2-5");
        assertThat(Buckets.standard(6)).isEqualTo("6-10");
        assertThat(Buckets.standard(10)).isEqualTo("6-10");
        assertThat(Buckets.standard(11)).isEqualTo("11+");
        assertThat(Buckets.standard(999)).isEqualTo("11+");
    }

    @Test
    void chatMemoryBucketsCoverEveryBoundary() {
        assertThat(Buckets.chatMemory(-1)).isEqualTo("0");
        assertThat(Buckets.chatMemory(0)).isEqualTo("0");
        assertThat(Buckets.chatMemory(1)).isEqualTo("1-5");
        assertThat(Buckets.chatMemory(5)).isEqualTo("1-5");
        assertThat(Buckets.chatMemory(6)).isEqualTo("6-10");
        assertThat(Buckets.chatMemory(10)).isEqualTo("6-10");
        assertThat(Buckets.chatMemory(11)).isEqualTo("11-20");
        assertThat(Buckets.chatMemory(20)).isEqualTo("11-20");
        assertThat(Buckets.chatMemory(21)).isEqualTo("21+");
        assertThat(Buckets.chatMemory(500)).isEqualTo("21+");
    }
}
