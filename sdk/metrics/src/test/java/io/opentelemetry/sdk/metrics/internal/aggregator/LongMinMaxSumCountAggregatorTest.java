/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.aggregator;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.GuardedBy;
import io.opentelemetry.sdk.common.InstrumentationLibraryInfo;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.exemplar.ExemplarReservoir;
import io.opentelemetry.sdk.metrics.internal.descriptor.MetricDescriptor;
import io.opentelemetry.sdk.resources.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

class LongMinMaxSumCountAggregatorTest {
  private static final Resource RESOURCE = Resource.getDefault();
  private static final InstrumentationLibraryInfo INSTRUMENTATION_LIBRARY_INFO =
      InstrumentationLibraryInfo.empty();
  private static final MetricDescriptor METRIC_DESCRIPTOR =
      MetricDescriptor.create("name", "description", "unit");
  private static final LongMinMaxSumCountAggregator aggregator =
      new LongMinMaxSumCountAggregator(ExemplarReservoir::noSamples);

  @Test
  void createHandle() {
    assertThat(aggregator.createHandle()).isInstanceOf(LongMinMaxSumCountAggregator.Handle.class);
  }

  @Test
  void testRecordings() {
    AggregatorHandle<MinMaxSumCountAccumulation> aggregatorHandle = aggregator.createHandle();
    aggregatorHandle.recordLong(100);
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty()))
        .isEqualTo(MinMaxSumCountAccumulation.create(1, 100, 100, 100));
    aggregatorHandle.recordLong(200);
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty()))
        .isEqualTo(MinMaxSumCountAccumulation.create(1, 200, 200, 200));
    aggregatorHandle.recordLong(-75);
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty()))
        .isEqualTo(MinMaxSumCountAccumulation.create(1, -75, -75, -75));
  }

  @Test
  void toAccumulationAndReset() {
    AggregatorHandle<MinMaxSumCountAccumulation> aggregatorHandle = aggregator.createHandle();
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty())).isNull();

    aggregatorHandle.recordLong(100);
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty()))
        .isEqualTo(MinMaxSumCountAccumulation.create(1, 100, 100, 100));
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty())).isNull();

    aggregatorHandle.recordLong(100);
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty()))
        .isEqualTo(MinMaxSumCountAccumulation.create(1, 100, 100, 100));
    assertThat(aggregatorHandle.accumulateThenReset(Attributes.empty())).isNull();
  }

  @Test
  void toMetricData() {
    AggregatorHandle<MinMaxSumCountAccumulation> aggregatorHandle = aggregator.createHandle();
    aggregatorHandle.recordLong(10);

    MetricData metricData =
        aggregator.toMetricData(
            RESOURCE,
            INSTRUMENTATION_LIBRARY_INFO,
            METRIC_DESCRIPTOR,
            Collections.singletonMap(
                Attributes.empty(), aggregatorHandle.accumulateThenReset(Attributes.empty())),
            AggregationTemporality.CUMULATIVE,
            0,
            10,
            100);
    assertThat(metricData).isNotNull();
    assertThat(metricData.getType()).isEqualTo(MetricDataType.SUMMARY);
  }

  @Test
  void testMultithreadedUpdates() throws Exception {
    final AggregatorHandle<MinMaxSumCountAccumulation> aggregatorHandle = aggregator.createHandle();
    final Summary summarizer = new Summary();
    int numberOfThreads = 10;
    final long[] updates = new long[] {1, 2, 3, 5, 7, 11, 13, 17, 19, 23};
    final int numberOfUpdates = 1000;
    final CountDownLatch starter = new CountDownLatch(numberOfThreads);
    List<Thread> workers = new ArrayList<>();
    for (int i = 0; i < numberOfThreads; i++) {
      final int index = i;
      Thread t =
          new Thread(
              () -> {
                long update = updates[index];
                try {
                  starter.await();
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                for (int j = 0; j < numberOfUpdates; j++) {
                  aggregatorHandle.recordLong(update);
                  if (ThreadLocalRandom.current().nextInt(10) == 0) {
                    summarizer.process(aggregatorHandle.accumulateThenReset(Attributes.empty()));
                  }
                }
              });
      workers.add(t);
      t.start();
    }
    for (int i = 0; i <= numberOfThreads; i++) {
      starter.countDown();
    }

    for (Thread worker : workers) {
      worker.join();
    }
    // make sure everything gets merged when all the aggregation is done.
    summarizer.process(aggregatorHandle.accumulateThenReset(Attributes.empty()));

    assertThat(summarizer.accumulation)
        .isEqualTo(
            MinMaxSumCountAccumulation.create(numberOfThreads * numberOfUpdates, 101000, 1, 23));
  }

  private static final class Summary {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @GuardedBy("lock")
    @Nullable
    private MinMaxSumCountAccumulation accumulation;

    void process(@Nullable MinMaxSumCountAccumulation other) {
      if (other == null) {
        return;
      }
      lock.writeLock().lock();
      try {
        if (accumulation == null) {
          accumulation = other;
          return;
        }
        accumulation = aggregator.merge(accumulation, other);
      } finally {
        lock.writeLock().unlock();
      }
    }
  }
}
