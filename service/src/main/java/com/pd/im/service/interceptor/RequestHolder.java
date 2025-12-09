package com.pd.im.service.interceptor;

/**
 * @author Parker
 * @date 12/9/25
 */
public class RequestHolder {
    private final static ThreadLocal<Boolean> requestHolder = new ThreadLocal<>();

    public static void set(Boolean isAdmin) {
        requestHolder.set(isAdmin);
    }

    public static Boolean get() {
        return requestHolder.get();
    }

    public static void remove() {
        requestHolder.remove();
    }
}
