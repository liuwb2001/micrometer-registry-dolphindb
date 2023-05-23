package io.micrometer.dolphindb;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
//import io.micrometer.opentsdb.OpenTSDBMeterRegistry;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Main app = new Main();
//        DolphinDBConfig config;
        MeterRegistry registry = new DolphinDBMeterRegistry(k->null, Clock.SYSTEM);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> app.flushMetric(registry), 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void flushMetric(MeterRegistry registry) {
        Counter counter = Counter.builder("demo.testing2")
                .tag("app", "java")
                .description("123")
                .register(registry);
        counter.increment();
        Timer timer = Timer.builder("demo.timer")
                        .tag("app1", "python")
                        .register(registry);
        timer.record(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException ignored) {
            }
        });
        System.out.println(timer.count());
    }
}
