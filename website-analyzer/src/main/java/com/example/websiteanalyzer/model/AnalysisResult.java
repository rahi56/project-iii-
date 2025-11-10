package com.example.websiteanalyzer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    private String title;

    @Column(length = 2000)
    private String metaDescription;

    private int wordCount;

    // SSL info
    private boolean sslValid;
    private String sslIssuer;
    private LocalDateTime sslExpiryDate;

    // Domain info
    @Column(length = 3000)
    private String domainInfo;

    // Links
    private int totalLinks;
    private int brokenLinks;

    // Security headers
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "security_headers", joinColumns = @JoinColumn(name = "analysis_id"))
    @MapKeyColumn(name = "header_name")
    @Column(name = "header_value", length = 2000)
    private Map<String, String> securityHeaders = new HashMap<>();

    // Score
    private int score;

    private LocalDateTime analyzedAt;

    // ====== Getters & Setters ======

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public boolean isSslValid() { return sslValid; }
    public void setSslValid(boolean sslValid) { this.sslValid = sslValid; }

    public String getSslIssuer() { return sslIssuer; }
    public void setSslIssuer(String sslIssuer) { this.sslIssuer = sslIssuer; }

    public LocalDateTime getSslExpiryDate() { return sslExpiryDate; }
    public void setSslExpiryDate(LocalDateTime sslExpiryDate) { this.sslExpiryDate = sslExpiryDate; }

    public String getDomainInfo() { return domainInfo; }
    public void setDomainInfo(String domainInfo) { this.domainInfo = domainInfo; }

    public int getTotalLinks() { return totalLinks; }
    public void setTotalLinks(int totalLinks) { this.totalLinks = totalLinks; }

    public int getBrokenLinks() { return brokenLinks; }
    public void setBrokenLinks(int brokenLinks) { this.brokenLinks = brokenLinks; }

    public Map<String, String> getSecurityHeaders() { return securityHeaders; }
    public void setSecurityHeaders(Map<String, String> securityHeaders) { this.securityHeaders = securityHeaders; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
