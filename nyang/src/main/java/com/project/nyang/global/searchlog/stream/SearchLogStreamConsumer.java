package com.project.nyang.global.searchlog.stream;

import com.project.nyang.global.searchlog.domain.SearchLogDocument;
import com.project.nyang.global.searchlog.service.SearchLogEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis Stream의 검색 로그를 받아 Elasticsearch에 저장합니다.
 * ES 저장에 성공한 메시지만 ACK 하므로, 실패한 메시지는 pending 목록에 남았다가 재처리됩니다.
 *
 * @fileName : SearchLogStreamConsumer
 * @since : 2026-11-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    public static final String GROUP = "search-log-group";
    // shortcut: 서버가 한 대라 컨슈머 이름 고정, 여러 대로 늘리면 인스턴스마다 다른 이름 필요
    public static final String CONSUMER = "nyang-1";

    private final SearchLogEsService searchLogEsService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        handle(record);
    }

    // ES가 잠깐 죽었다 살아난 경우를 위해, ACK 안 된(pending) 메시지를 1분마다 다시 처리
    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public void retryPending() {
        try {
            List<MapRecord<String, Object, Object>> pending = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP, CONSUMER),
                    StreamReadOptions.empty().count(100),
                    StreamOffset.create(SearchLogPublisher.STREAM_KEY, ReadOffset.from("0")));
            if (pending == null || pending.isEmpty()) {
                return;
            }
            log.info("검색 로그 재처리: {}건", pending.size());
            for (MapRecord<String, Object, Object> r : pending) {
                handle(r.getStream(), r.getId().getValue(),
                        String.valueOf(r.getValue().get("keyword")),
                        String.valueOf(r.getValue().get("searchedAt")));
            }
        } catch (Exception e) {
            log.warn("검색 로그 재처리 실패: {}", e.getMessage());
        }
    }

    private void handle(MapRecord<String, String, String> record) {
        handle(record.getStream(), record.getId().getValue(),
                record.getValue().get("keyword"), record.getValue().get("searchedAt"));
    }

    private void handle(String stream, String recordId, String keyword, String searchedAt) {
        try {
            searchLogEsService.save(SearchLogDocument.builder()
                    .keyword(keyword)
                    .searchedAt(searchedAt)
                    .build());
            redisTemplate.opsForStream().acknowledge(stream, GROUP, recordId);
        } catch (Exception e) {
            // ACK 하지 않으면 pending 목록에 남아 retryPending()에서 다시 시도
            log.warn("검색 로그 ES 저장 실패, 재처리 대기: {} ({})", recordId, e.getMessage());
        }
    }
}
