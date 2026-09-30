package com.pulsewatch.probe;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class CertificateCapturingTrustManager implements X509TrustManager {

    private final X509TrustManager defaultTrustManager;
    private X509Certificate lastCertificate;

    public CertificateCapturingTrustManager() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            X509TrustManager tm = null;
            for (TrustManager m : tmf.getTrustManagers()) {
                if (m instanceof X509TrustManager) {
                    tm = (X509TrustManager) m;
                    break;
                }
            }
            this.defaultTrustManager = tm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize system TrustManager", e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        defaultTrustManager.checkClientTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain != null && chain.length > 0) {
            this.lastCertificate = chain[0]; // Intercept peer leaf certificate
        }
        defaultTrustManager.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    public X509Certificate getLastCertificate() {
        return lastCertificate;
    }
}