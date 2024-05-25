package io.github.alexswilliams.csv;

public interface Result<T> {
    final class OK<T> implements Result<T> {
        T data;

        OK(T data) {
            this.data = data;
        }
    }

    final class Error<T> implements Result<T> {
        Exception exception;
        int line;

        Error(Exception exception, int line) {
            this.exception = exception;
            this.line = line;
        }
    }
}
