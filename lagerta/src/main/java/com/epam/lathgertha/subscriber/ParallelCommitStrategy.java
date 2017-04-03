package com.epam.lathgertha.subscriber;

import com.epam.lathgertha.capturer.TransactionScope;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implemented parallel logic to commit batch of transactions.
 */
public class ParallelCommitStrategy implements CommitStrategy {
    private static final int POOL_COUNT = 5;

    private final CommitServitor commitServitor;
    private final ForkJoinPool pool;

    public ParallelCommitStrategy(CommitServitor commitServitor) {
        this.commitServitor = commitServitor;
        pool = new ForkJoinPool();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void commit(List<Long> txIdsToCommit, Map<Long, TransactionData> buffer) {
        new ParallelExecutor(buffer).commit(txIdsToCommit);
    }

    /**
     * Stores state of commit of single batch
     */
    private class ParallelExecutor {
        private final Map<Long, TransactionData> buffer;

        private final Map<Map.Entry<String, Object>, Long> lastOwner = new HashMap<>();
        private final Map<Long, TransactionRelation> relationMap = new HashMap<>();
        private final BlockingQueue<TransactionRelation> tasks = new LinkedBlockingQueue<>();
        private final AtomicInteger count = new AtomicInteger();

        ParallelExecutor(Map<Long, TransactionData> buffer) {
            this.buffer = buffer;
        }

        @SuppressWarnings("unchecked")
        public void commit(List<Long> txIdsToCommit) {
            txIdsToCommit
                    .stream()
                    .map(txId -> relation(txId, buffer))
                    .peek(relation -> relationMap.put(relation.id, relation))
                    .filter(TransactionRelation::isFree)
                    .forEach(tasks::add);

            relationMap.values().forEach(relation -> relation.fillRelations(relationMap));

            count.set(relationMap.size());
            IntStream
                    .range(0, Math.min(POOL_COUNT, relationMap.size()))
                    .boxed()
                    .map(i -> (Runnable) this::execute)
                    .map(pool::submit)
                    .collect(Collectors.toList())
                    .forEach(ForkJoinTask::join);
        }

        private TransactionRelation relation(Long txId, Map<Long, TransactionData> buffer) {
            return new TransactionRelation(txId, buffer
                    .get(txId)
                    .getTransactionScope()
                    .getScope()
                    .stream()
                    .flatMap(cacheKeys -> ((Stream<?>) cacheKeys.getValue()
                            .stream())
                            .map(key -> compositeKey(cacheKeys.getKey(), key)))
                    .map(compositeKey -> lastOwner.put(compositeKey, txId))
                    .distinct()
                    .filter(Objects::nonNull)
                    .map(relationMap::get)
                    .peek(blocker -> blocker.addDependent(txId))
                    .count());
        }

        private void execute() {
            try {
                while (count.getAndDecrement() > 0) {
                    TransactionRelation relation = tasks.take();
                    if (relation.isAlive() && commitServitor.commit(relation.getId(), buffer)) {
                        relation
                                .dependent()
                                .stream()
                                .filter(TransactionRelation::release)
                                .forEach(tasks::add);
                    } else {
                        relation
                                .dependent()
                                .stream()
                                .peek(TransactionRelation::kill)
                                .forEach(tasks::add);
                    }
                }
            } catch (InterruptedException e) {
                //do nothing
            }
        }

        private Map.Entry<String, Object> compositeKey(String cacheName, Object o) {
            return new AbstractMap.SimpleImmutableEntry<>(cacheName, o);
        }
    }

    /**
     * Stores single transaction relations
     */
    private static class TransactionRelation {
        private final long id;
        private AtomicInteger count;
        private final List<Long> dependentIds;
        private final List<TransactionRelation> dependent;
        private volatile boolean alive;

        TransactionRelation(long id, long count) {
            this.id = id;
            this.count = new AtomicInteger((int) count);
            dependentIds = new ArrayList<>();
            dependent = new ArrayList<>();
            alive = true;
        }

        long getId() {
            return id;
        }

        List<TransactionRelation> dependent() {
            return dependent;
        }

        void addDependent(long id) {
            dependentIds.add(id);
        }

        void fillRelations(Map<Long, TransactionRelation> relationMap) {
            dependentIds.stream().map(relationMap::get).forEach(dependent::add);
        }

        boolean release() {
            return count.decrementAndGet() == 0;
        }

        boolean isFree() {
            return count.get() == 0;
        }

        void kill() {
            alive = false;
        }

        public boolean isAlive() {
            return alive;
        }
    }
}
