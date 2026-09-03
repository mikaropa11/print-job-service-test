package com.adobe.printservice.web;

import com.adobe.printservice.exception.DatabaseUnavailableException;
import com.adobe.printservice.repository.RenderTemplateRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthResource {

    private final RenderTemplateRepository renderTemplateRepository;

    public HealthResource(RenderTemplateRepository renderTemplateRepository) {
        this.renderTemplateRepository = renderTemplateRepository;
    }

    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "UP");
    }

    @GetMapping("/ready")
    public Map<String, String> ready() {
        try {
            renderTemplateRepository.count();
            return Map.of("status", "READY", "database", "UP");
        } catch (RuntimeException exception) {
            throw new DatabaseUnavailableException(exception);
        }
    }
}
