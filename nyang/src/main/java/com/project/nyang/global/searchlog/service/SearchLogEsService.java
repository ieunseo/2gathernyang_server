package com.project.nyang.global.searchlog.service;

import com.project.nyang.global.searchlog.domain.SearchLogDocument;
import com.project.nyang.global.searchlog.repository.SearchLogEsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * 엘라스틱서치 저장. 통계 집계 비즈니스 로직 서비스
 * @fileName        : SearchLogEsRepository
 * @author          : 박세정
 * @since           : 2025-07-16
 *
 */
@RequiredArgsConstructor
@Service
public class SearchLogEsService {
    private final SearchLogEsRepository searchLogEsRepository;

    //Redis Stream에서 전달 받은 검색데이터 저장 메서드 (프로젝트 종료 후 Kafka에서 교체)
    public void save(SearchLogDocument searchLogDocument) {
        searchLogEsRepository.save(searchLogDocument);
    }

}
