package io.github.alexswilliams.csv;

class CheckedLaterException extends RuntimeException {
    CheckedLaterException(final Exception cause) {
        super(cause);
    }
}
