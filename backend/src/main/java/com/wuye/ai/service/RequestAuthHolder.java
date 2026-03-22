package com.wuye.ai.service;

public final class RequestAuthHolder {

    private static final ThreadLocal<String> AUTHORIZATION = new ThreadLocal<>();

    private RequestAuthHolder() {
    }

    public static void setAuthorization(String authorization) {
        AUTHORIZATION.set(authorization);
    }

    public static String getAuthorization() {
        return AUTHORIZATION.get();
    }

    public static void clear() {
        AUTHORIZATION.remove();
    }
}