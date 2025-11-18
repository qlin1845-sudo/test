package net.mooctest;

import java.util.concurrent.*;

public class ThreadPoolManager {
    private static volatile ThreadPoolManager instance;
    private final ExecutorService executorService;

    public ThreadPoolManager() {
        // 创建一个固定大小的线程池，大小可以根据系统要求进行调整
        int poolSize = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(poolSize);
    }

    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    public void submitTask(Runnable task) {
        executorService.submit(task);
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
