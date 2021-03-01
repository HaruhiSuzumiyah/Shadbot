package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.SystemUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClientGroup;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final Logger LOGGER = LogUtil.getLogger(TaskManager.class);

    private final List<Disposable> tasks;

    public TaskManager() {
        this.tasks = new ArrayList<>(4);
    }

    public void schedulePresenceUpdates(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling presence updates");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofMinutes(15), DEFAULT_SCHEDULER)
                .map(__ -> ShadbotUtil.getRandomStatus())
                .flatMap(gateway::updatePresence)
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    /*public void scheduleLottery(GatewayDiscordClient gateway) {
        LOGGER.info("Starting lottery (next draw in {})", FormatUtil.formatDurationWords(LotteryCmd.getDelay()));
        final Disposable task = Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), DEFAULT_SCHEDULER)
                .flatMap(__ -> LotteryCmd.draw(gateway)
                        .onErrorResume(err -> Mono.fromCallable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }*/

    public void schedulePeriodicStats(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling periodic stats log");

        final GatewayClientGroup group = gateway.getGatewayClientGroup();
        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofSeconds(15), DEFAULT_SCHEDULER)
                .then(gateway.getGuilds().count())
                .doOnNext(guildCount -> {
                    Telemetry.UPTIME_GAUGE.set(SystemUtil.getUptime());
                    Telemetry.PROCESS_CPU_USAGE_GAUGE.set(SystemUtil.getProcessCpuUsage());
                    Telemetry.SYSTEM_CPU_USAGE_GAUGE.set(SystemUtil.getSystemCpuUsage());
                    Telemetry.MAX_HEAP_MEMORY_GAUGE.set(SystemUtil.getMaxHeapMemory());
                    Telemetry.TOTAL_HEAP_MEMORY_GAUGE.set(SystemUtil.getTotalHeapMemory());
                    Telemetry.USED_HEAP_MEMORY_GAUGE.set(SystemUtil.getUsedHeapMemory());
                    Telemetry.TOTAL_MEMORY_GAUGE.set(SystemUtil.getTotalMemory());
                    Telemetry.FREE_MEMORY_GAUGE.set(SystemUtil.getFreeMemory());
                    Telemetry.GC_COUNT_GAUGE.set(SystemUtil.getGCCount());
                    Telemetry.GC_TIME_GAUGE.set(SystemUtil.getGCTime());
                    Telemetry.THREAD_COUNT_GAUGE.set(SystemUtil.getThreadCount());
                    Telemetry.DAEMON_THREAD_COUNT_GAUGE.set(SystemUtil.getDaemonThreadCount());

                    Telemetry.GUILD_COUNT_GAUGE.set(guildCount);

                    for (int i = 0; i < group.getShardCount(); ++i) {
                        final long responseTime = group.find(i).orElseThrow().getResponseTime().toMillis();
                        Telemetry.RESPONSE_TIME_GAUGE.labels(Integer.toString(i)).set(responseTime);
                    }
                })
                .subscribe(null, ExceptionHandler::handleUnknownError);

        this.tasks.add(task);
    }

    public void schedulePostStats(BotListStats botListStats) {
        LOGGER.info("Starting bot list stats scheduler");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofHours(3), DEFAULT_SCHEDULER)
                .flatMap(__ -> botListStats.postStats())
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}
