package com.project.nyang.global.searchlog.service;

import com.project.nyang.global.searchlog.domain.SearchLogDocument;
import com.project.nyang.global.searchlog.repository.SearchLogEsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * 엘라스틱서치 저장. 통계 집계 비즈니스 로직 서비스
 * @fileName        : SearchLogEsRepository
 * @author          : 박세정
 * @since           : 2025-07-16
 *
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SearchLogEsService {
    private final SearchLogEsRepository searchLogEsRepository;

    //검색데이터 저장 메서드
    public void save(SearchLogDocument searchLogDocument) {
        searchLogEsRepository.save(searchLogDocument);
    }

    // 프로젝트 종료 후 리팩터링: Kafka를 거치지 않고 검색 로그를 ES에 바로 저장
    // 검색 응답이 로그 저장을 기다리지 않도록 비동기로 처리하고, 실패해도 검색에는 영향 없음
    @Async
    public void saveSearchLog(String keyword) {
        try {
            save(SearchLogDocument.builder()
                    .keyword(keyword)
                    .searchedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build());
        } catch (Exception e) {
            log.warn("검색 로그 저장 실패: {}", e.getMessage());
        }
    }

}
