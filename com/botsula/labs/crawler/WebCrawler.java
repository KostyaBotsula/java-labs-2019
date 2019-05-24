package com.botsula.labs.crawler;

import info.kgeorgiy.java.advanced.crawler.Crawler;
import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;
import info.kgeorgiy.java.advanced.crawler.Result;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static info.kgeorgiy.java.advanced.crawler.URLUtils.getHost;

public class WebCrawler implements Crawler {

    private static final int EXTRACTORS_LIMIT = 140;
    private static final int DOWNLOADERS_LIMIT = 140;
    private static final Result EMPTY_RESULT = new Result(Collections.emptyList(), Collections.emptyMap());

    private final int MAX_CONCURRENT_DOWNLOADS_PER_HOST;
    private final Downloader downloader;
    private final ExecutorService executor;
    private final Map<String, Semaphore> hostCounters = new ConcurrentHashMap<>();
    private final Map<String, Lock> downloadLocks = new ConcurrentHashMap<>();
    private final Map<String, Lock> extractLocks = new ConcurrentHashMap<>();
    private final Map<String, Result> takenPages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> children = new ConcurrentHashMap<>();
    private final Semaphore extractorsLock;
    private final Semaphore downloadersLock;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) throws IOException {
        this.downloader = downloader;
        MAX_CONCURRENT_DOWNLOADS_PER_HOST = perHost;

        extractorsLock = new Semaphore(extractors);
        downloadersLock = new Semaphore(downloaders);

        downloaders = downloaders > DOWNLOADERS_LIMIT ? DOWNLOADERS_LIMIT : downloaders;
        extractors = extractors > EXTRACTORS_LIMIT ? EXTRACTORS_LIMIT : extractors;

        executor = Executors.newWorkStealingPool(extractors + downloaders);
    }

    @Override
    public Result download(String pageUrl, int i) {
        if (i <= 0 || pageUrl == null) {
            return EMPTY_RESULT;
        }

        String host = getHostFromURL(pageUrl);

        Document document = downloadPage(pageUrl, host);

        extractDocument(document, pageUrl);

        ArrayList<String> downloaded = new ArrayList<>(takenPages.get(pageUrl).getDownloaded());
        HashMap<String, IOException> errors = new HashMap<>(takenPages.get(pageUrl).getErrors());

        children.getOrDefault(pageUrl, new HashSet<>()).stream()
                .map(url -> (Callable<Result>) () -> download(url, i - 1))
                .map(executor::submit)
                .collect(Collectors.toList())
                .forEach(subResult -> {
                    try {
                        Result result = subResult.get();
                        downloaded.addAll(result.getDownloaded());
                        errors.putAll(result.getErrors());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });

        return new Result(downloaded, errors);
    }

    private Document downloadPage(String pageUrl, String host) {
        Document document = null;
        if (!takenPages.containsKey(pageUrl)) {
            downloadLocks.putIfAbsent(pageUrl, new ReentrantLock());
            downloadLocks.get(pageUrl).lock();
            try {
                if (!takenPages.containsKey(pageUrl)) {
                    hostCounters.putIfAbsent(host, new Semaphore(MAX_CONCURRENT_DOWNLOADS_PER_HOST));
                    try {
                        downloadersLock.acquire();
                        hostCounters.get(host).acquire();
                        try {
                            document = downloader.download(pageUrl);
                            takenPages.put(pageUrl, new Result(Collections.singletonList(pageUrl), Collections.emptyMap()));
                        } catch (IOException e) {
                            Map<String, IOException> errors = Collections.singletonMap(pageUrl, e);
                            takenPages.put(pageUrl, new Result(Collections.emptyList(), errors));
                        }
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Can't acquire the semaphore");
                    } finally {
                        hostCounters.get(host).release();
                        downloadersLock.release();
                    }
                }
            } finally {
                downloadLocks.get(pageUrl).unlock();
            }
        }

        return document;
    }

    private void extractDocument(Document document, String pageUrl) {
        if (!children.containsKey(pageUrl)) {
            extractLocks.putIfAbsent(pageUrl, new ReentrantLock());
            extractLocks.get(pageUrl).lock();
            try {
                if (document != null && !children.containsKey(pageUrl)) {
                    List<String> childUrls = new ArrayList<>();
                    try {
                        extractorsLock.acquire();
                        childUrls = document.extractLinks();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Cannot acquire the semaphore");
                    } finally {
                        extractorsLock.release();
                    }
                    children.put(pageUrl, new HashSet<>(childUrls));
                }
            } finally {
                extractLocks.get(pageUrl).unlock();
            }
        }
    }

    private String getHostFromURL(String pageUrl) {
        String host;
        try {
            host = getHost(pageUrl);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Can't get the host from url :" + pageUrl);
        }
        return host;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}