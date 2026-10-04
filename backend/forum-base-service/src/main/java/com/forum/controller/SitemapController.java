package com.forum.controller;

import com.forum.service.SitemapService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    @GetMapping(value = {"/api/sitemap.xml", "/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap(HttpServletRequest request) {
        String sitemapXml = sitemapService.generateSitemapXml(request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/xml;charset=UTF-8"))
                .body(sitemapXml);
    }
}
