package com.example.websiteanalyzer.Controller;

import com.example.websiteanalyzer.model.AnalysisResult;
import com.example.websiteanalyzer.repository.AnalysisRepository;
import com.example.websiteanalyzer.service.AnalyzerService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class AnalysisController {

    private final AnalyzerService analyzerService;
    private final AnalysisRepository repository;

    public AnalysisController(AnalyzerService analyzerService, AnalysisRepository repository) {
        this.analyzerService = analyzerService;
        this.repository = repository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("url") String url, Model model) {
        if (url == null || url.isBlank()) {
            model.addAttribute("error", "Please enter a valid URL");
            return "index";
        }

        if (!url.startsWith("https://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            AnalysisResult temp = new AnalysisResult();
            temp.setUrl(url);
            temp.setTitle("Analyzing...");
            temp.setMetaDescription("Please wait, analysis is in progress...");
            temp.setScore(0);
            temp = repository.save(temp);

            Long id = temp.getId();
            analyzerService.analyzeAndUpdate(id, url);

            model.addAttribute("result", temp);
            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to start analysis: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/api/result/{id}")
    @ResponseBody
    public ResponseEntity<AnalysisResult> getResult(@PathVariable Long id) {
        Optional<AnalysisResult> result = repository.findById(id);
        return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public String history(Model model) {
        List<AnalysisResult> results = repository.findAll();
        model.addAttribute("results", results);
        return "history";
    }
}
