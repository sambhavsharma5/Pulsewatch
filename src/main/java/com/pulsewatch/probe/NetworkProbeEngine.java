package com.pulsewatch.probe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.springframework.stereotype.Component;

@Component
public class NetworkProbeEngine {

    public ProbeResult execute(String targetUrl, int timeoutSeconds, int expectedStatusCode) {
        CertificateCapturingTrustManager capturingTrustManager = new CertificateCapturingTrustManager();
        
        HttpClient client;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{capturingTrustManager}, new SecureRandom());
            
            client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
        } catch (Exception e) {
            return new ProbeResult(null, 0, false, null, "SSL Engine Initialization Failure: " + e.getMessage());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("User-Agent", "PulseWatch-UptimeBot/1.0 (+https://pulsewatch.internal)")
                .GET()
                .build();

        Instant start = Instant.now();
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int latency = (int) Duration.between(start, Instant.now()).toMillis();

            Integer sslDaysRemaining = null;
            if (capturingTrustManager.getLastCertificate() != null) {
                Instant expiry = capturingTrustManager.getLastCertificate().getNotAfter().toInstant();
                sslDaysRemaining = (int) ChronoUnit.DAYS.between(Instant.now(), expiry);
            }

            boolean isSuccess = (response.statusCode() == expectedStatusCode);
            String error = isSuccess ? null : "Status mismatch: expected " + expectedStatusCode + ", got " + response.statusCode();

            return new ProbeResult(response.statusCode(), latency, isSuccess, sslDaysRemaining, error);

        } catch (Exception e) {
            int latency = (int) Duration.between(start, Instant.now()).toMillis();
            return new ProbeResult(null, latency, false, null, "Probe Failed: " + e.getMessage());
        }
    }
}