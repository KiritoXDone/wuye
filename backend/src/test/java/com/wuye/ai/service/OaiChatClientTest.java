package com.wuye.ai.service;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OaiChatClientTest {

    @Test
    void selectsHttpsProxyForHttpsEndpoint() {
        InetSocketAddress address = OaiChatClient.proxyAddress(
                        URI.create("https://api.example.com/v1/chat/completions"),
                        Map.of(
                                "HTTPS_PROXY", "http://127.0.0.1:8888",
                                "HTTP_PROXY", "http://127.0.0.1:8080"
                        ))
                .orElseThrow();

        assertEquals("127.0.0.1", address.getHostString());
        assertEquals(8888, address.getPort());
    }

    @Test
    void bypassesProxyForExactHostAndSubdomain() {
        Map<String, String> environment = Map.of(
                "HTTPS_PROXY", "http://127.0.0.1:8888",
                "NO_PROXY", "localhost,.internal.example.com"
        );

        assertTrue(OaiChatClient.proxyAddress(URI.create("https://internal.example.com/v1"), environment).isEmpty());
        assertTrue(OaiChatClient.proxyAddress(URI.create("https://api.internal.example.com/v1"), environment).isEmpty());
    }

    @Test
    void acceptsLowercaseProxyWithoutScheme() {
        InetSocketAddress address = OaiChatClient.proxyAddress(
                        URI.create("https://api.example.com/v1"),
                        Map.of("https_proxy", "proxy.example.com:3128"))
                .orElseThrow();

        assertEquals("proxy.example.com", address.getHostString());
        assertEquals(3128, address.getPort());
    }
}
