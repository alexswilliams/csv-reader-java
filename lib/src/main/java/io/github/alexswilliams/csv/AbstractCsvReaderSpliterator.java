package io.github.alexswilliams.csv;

import java.util.List;
import java.util.Spliterator;

abstract class AbstractCsvReaderSpliterator implements Spliterator<Result<List<String>>> {
    @Override
    public Spliterator<Result<List<String>>> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return ORDERED | IMMUTABLE | NONNULL;
    }
}
