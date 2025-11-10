package com.example.websiteanalyzer.service;

import com.example.websiteanalyzer.model.AnalysisResult;
import com.example.websiteanalyzer.repository.AnalysisRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class AnalyzerService {

    private final AnalysisRepository repository;
    private final ExecutorService executor;

    private static final int HTML_TIMEOUT_MS = 10000;
    private static final int HEAD_TIMEOUT_MS = 4000;
    private static final int MAX_LINKS_TO_CHECK = 10;
    private static final int THREAD_POOL_SIZE = 6;

    public AnalyzerService(AnalysisRepository repository) {
        this.repository = repository;
        this.executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void analyzeAndUpdate(Long id, String url) {
        executor.submit(() -> {
            try {
                AnalysisResult result = repository.findById(id).orElse(new AnalysisResult());
                result.setUrl(url);
                result.setAnalyzedAt(LocalDateTime.now());
                repository.save(result);

                // Fetch HTML
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(HTML_TIMEOUT_MS)
                        .ignoreHttpErrors(true)
                        .followRedirects(true)
                        .get();

                result.setTitle(Optional.ofNullable(doc.title()).orElse(""));
                result.setMetaDescription(doc.select("meta[name=description]").attr("content"));
                result.setWordCount(doc.text().split("\\s+").length);
                repository.save(result);

                // Links
                List<String> links = doc.select("a[href]").eachAttr("abs:href").stream()
                        .filter(Objects::nonNull).distinct().collect(Collectors.toList());
                result.setTotalLinks(links.size());

                List<String> toCheck = links.stream().limit(MAX_LINKS_TO_CHECK).collect(Collectors.toList());
                AtomicInteger broken = new AtomicInteger(0);
                CountDownLatch latch = new CountDownLatch(toCheck.size());

                for (String link : toCheck) {
                    executor.submit(() -> {
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(link).openConnection();
                            conn.setRequestMethod("HEAD");
                            conn.setConnectTimeout(HEAD_TIMEOUT_MS);
                            conn.setReadTimeout(HEAD_TIMEOUT_MS);
                            conn.connect();
                            if (conn.getResponseCode() >= 400) broken.incrementAndGet();
                            conn.disconnect();
                        } catch (Exception e) {
                            broken.incrementAndGet();
                        } finally { latch.countDown(); }
                    });
                }
                latch.await((HEAD_TIMEOUT_MS / 1000L) * toCheck.size(), TimeUnit.SECONDS);
                result.setBrokenLinks(broken.get());

                // SSL
                if (url.startsWith("https")) {
                    try {
                        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
                        conn.setConnectTimeout(HEAD_TIMEOUT_MS);
                        conn.setReadTimeout(HEAD_TIMEOUT_MS);
                        conn.connect();
                        X509Certificate cert = (X509Certificate) conn.getServerCertificates()[0];
                        result.setSslValid(true);
                        result.setSslIssuer(cert.getIssuerX500Principal().getName());
                        result.setSslExpiryDate(cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                        conn.disconnect();
                    } catch (Exception e) {
                        result.setSslValid(false);
                        result.setSslIssuer("SSL error: " + e.getMessage());
                    }
                } else {
                    result.setSslValid(false);
                    result.setSslIssuer("No SSL");
                }

                // Domain
                try {
                    String host = new URL(url).getHost();
                    String ip = InetAddress.getByName(host).getHostAddress();
                    result.setDomainInfo("Host: " + host + " | IP: " + ip);
                } catch (Exception e) {
                    result.setDomainInfo("Domain lookup failed");
                }

                // Security headers
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setRequestMethod("HEAD");
                    conn.setConnectTimeout(HEAD_TIMEOUT_MS);
                    conn.setReadTimeout(HEAD_TIMEOUT_MS);
                    conn.connect();
                    Map<String, String> headers = new HashMap<>();
                    Arrays.asList("Content-Security-Policy","Strict-Transport-Security",
                                    "X-Frame-Options","X-Content-Type-Options","Referrer-Policy")
                            .forEach(h -> { if(conn.getHeaderField(h)!=null) headers.put(h, conn.getHeaderField(h)); });
                    result.setSecurityHeaders(headers);
                    conn.disconnect();
                } catch (Exception e) { result.setSecurityHeaders(new HashMap<>()); }

                // Score
                int score = 0;
                if(result.isSslValid()) score+=30;
                if(result.getBrokenLinks()==0) score+=20;
                if(result.getSecurityHeaders()!=null && !result.getSecurityHeaders().isEmpty()) score+=30;
                if(result.getDomainInfo()!=null && !result.getDomainInfo().isBlank()) score+=20;
                result.setScore(score);

                result.setAnalyzedAt(LocalDateTime.now());
                repository.save(result);

            } catch (Exception e) {
                // Catch-all
                AnalysisResult failResult = repository.findById(id).orElse(new AnalysisResult());
                failResult.setTitle("Error fetching website");
                failResult.setMetaDescription(e.getMessage());
                failResult.setWordCount(0);
                failResult.setTotalLinks(0);
                failResult.setBrokenLinks(0);
                failResult.setSslValid(false);
                failResult.setSslIssuer("Error");
                failResult.setDomainInfo("Error");
                failResult.setSecurityHeaders(new HashMap<>());
                failResult.setScore(0);
                failResult.setAnalyzedAt(LocalDateTime.now());
                repository.save(failResult);
            }
        });
    }

    public void shutdown() { executor.shutdown(); }
}
