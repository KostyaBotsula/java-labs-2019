package com.botsula.labs.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IterativeParallelism implements ListIP {

    public <T> T maximum(int threadLimit, List<? extends T> list, Comparator<? super T> comparator)
            throws InterruptedException {
        Function<List<? extends T>, T> f = l -> Collections.max(l, comparator);

        return Collections.max(invokeForThreadsResult(list, threadLimit, f), comparator);
    }

    public <T> T minimum(int threadLimit, List<? extends T> list, Comparator<? super T> comparator)
            throws InterruptedException {
        Function<List<? extends T>, T> f = l -> Collections.min(l, comparator);

        return Collections.min(invokeForThreadsResult(list, threadLimit, f), comparator);
    }

    public <T> boolean all(int threadLimit, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        Function<List<? extends T>, Boolean> f = l -> l.stream().allMatch(predicate);

        return invokeForThreadsResult(list, threadLimit, f).stream().allMatch(value -> value);

    }

    public <T> boolean any(int threadLimit, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        Function<List<? extends T>, Boolean> f = l -> l.stream().anyMatch(predicate);

        return invokeForThreadsResult(list, threadLimit, f).stream().anyMatch(value -> value);
    }

    @Override
    public String join(int threadLimit, List<?> list) throws InterruptedException {
        Function<List<?>, String> f = l -> l.stream().map(Object::toString).collect(Collectors.joining());

        return invokeForThreadsResult(list, threadLimit, f).stream().collect(Collectors.joining());
    }

    @Override
    public <T> List<T> filter(int threadLimit, List<? extends T> list, Predicate<? super T> predicate)
            throws InterruptedException {
        Function<List<? extends T>, List<T>> f = l -> l.stream().filter(predicate).collect(Collectors.toList());

        return invokeForThreadsResult(list, threadLimit, f).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public <T, U> List<U> map(int threadLimit, List<? extends T> list, Function<? super T, ? extends U> function)
            throws InterruptedException {
        Function<List<? extends T>, List<U>> f = l -> l.stream().map(function).collect(Collectors.toList());

        return invokeForThreadsResult(list, threadLimit, f).stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private <T> int getNumberOfTasks(int i, int numOfElements) {
        return numOfElements < i ? numOfElements : i;
    }

    private <T, R> List<R> invokeForThreadsResult(List<? extends T> elements, int threadLimit,
                                                  Function<List<? extends T>, R> function) throws InterruptedException {

        int taskNum = getNumberOfTasks(threadLimit, elements.size());

        Thread[] threads = new Thread[taskNum];
        List<R> threadResults = new ArrayList<>(Collections.nCopies(taskNum, null));

        int subListLength = elements.size() / taskNum;
        for (int i = 0; i < taskNum; i++) {
            int task = i;
            Thread thread = new Thread(() -> {
                int fromIndex = subListLength * task;
                int toIndex = task == taskNum - 1 ? elements.size() : fromIndex + subListLength;
                threadResults.set(task, function.apply(elements.subList(fromIndex, toIndex)));
            });
            thread.start();
            threads[i] = thread;
        }

        for (Thread thread : threads) {
            thread.join();
        }

        return threadResults;
    }
}
