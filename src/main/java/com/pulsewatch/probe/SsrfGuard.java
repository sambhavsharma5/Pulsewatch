package com.pulsewatch.probe;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import org.springframework.stereotype.Component;

@Component
public class SsrfGuard {

    public void assertSafeUrl(String targetUrl) {
        URI uri;
        try {
            uri = URI.create(targetUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + targetUrl);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only http:// or https:// URLs are allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must contain a valid hostname");
        }
        String lower = host.toLowerCase();
        if (lower.equals("localhost") || lower.endsWith(".localhost") || lower.endsWith(".local")) {
            throw new IllegalArgumentException("Protected hostname not allowed: " + host);
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isBlocked(address)) {
                    throw new IllegalArgumentException(
                            "Target resolves to a protected/private address: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve hostname: " + host);
        }
    }

    private boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
            return true;
        }
        if (address instanceof Inet4Address) {
            return isCgnat(address.getHostAddress());
        }
        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();
            return isIpv4Mapped(bytes) && isCgnat(ipv4FromMapped(bytes));
        }
        return false;
    }

    private boolean isCgnat(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        try {
            int first = Integer.parseInt(parts[0]);
            int second = Integer.parseInt(parts[1]);
            return first == 100 && second >= 64 && second <= 127; // 100.64.0.0/10 CGNAT
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isIpv4Mapped(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }

    private String ipv4FromMapped(byte[] bytes) {
        return (bytes[12] & 0xFF) + "." + (bytes[13] & 0xFF) + "." + (bytes[14] & 0xFF) + "." + (bytes[15] & 0xFF);
    }
}