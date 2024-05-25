package io.github.alexswilliams.csv;


interface Result<T> {
    default OK<T> ok() {
        if (this instanceof OK) return (OK<T>) this;
        throw new ClassCastException("Result was an Error");
    }

    default Error<T> error() {
        if (this instanceof Error) return (Error<T>) this;
        throw new ClassCastException("Result was an Error");
    }

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
