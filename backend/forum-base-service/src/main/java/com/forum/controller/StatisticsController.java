package com.forum.controller;

import com.forum.dto.ResponseDTO;
import com.forum.dto.StatisticsDTO;
import com.forum.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<ResponseDTO<StatisticsDTO>> getStatistics() {
        return ResponseEntity.ok(ResponseDTO.success(statisticsService.getStatistics()));
    }
}
