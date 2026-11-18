package com.project.nyang.global.searchlog.stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 검색 로그를 Redis Stream에 발행합니다.
 * 프로젝트 종료 후 리팩터링: Kafka 대신 이미 사용 중인 Redis의 Stream으로 교체했습니다.
 *
 * @fileName : SearchLogPublisher
 * @since : 2026-11-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchLogPublisher {

    public static final String STREAM_KEY = "search-log";
    // 스트림이 무한히 커지지 않도록 최근 약 10만 건만 유지 (~ 근사 trim이라 빠름)
    private static final long MAX_LENGTH = 100_000;

    private final StringRedisTemplate redisTemplate;

    // 검색 응답이 발행을 기다리지 않도록 비동기, 실패해도 검색에는 영향 없음
    @Async
    public void publish(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        try {
            Map<byte[], byte[]> fields = Map.of(
                    bytes("keyword"), bytes(keyword),
                    bytes("searchedAt"), bytes(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)));
            MapRecord<byte[], byte[], byte[]> record = StreamRecords.newRecord().in(bytes(STREAM_KEY)).ofMap(fields);
            // Spring Data Redis 3.2의 opsForStream()은 MAXLEN 옵션이 없어 커넥션의 XADD를 직접 호출
            redisTemplate.execute((RedisCallback<Object>) connection ->
                    connection.streamCommands().xAdd(record, XAddOptions.maxlen(MAX_LENGTH).approximateTrimming(true)));
        } catch (Exception e) {
            log.warn("검색 로그 발행 실패: {}", e.getMessage());
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
