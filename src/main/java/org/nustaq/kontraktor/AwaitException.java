package org.nustaq.kontraktor;

/**
 * if await is used on a Promise to wait for its result (non-blocking!) any error object
 * returned by the Promise will trigger a this Exception. In case the error
 * object is instance of Throwable, this will be thrown directly.
 */
public class AwaitException extends RuntimeException {
    Object o;

    public AwaitException(Object o) {
        super(""+o);
        this.o = o;
    }

}
