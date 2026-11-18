package com.project.nyang.global.searchlog.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

/**
 * 검색 로그 Redis Stream 컨슈머 설정입니다.
 * 컨슈머 그룹을 만들고(이미 있으면 그대로 사용), 새 메시지를 받으면 SearchLogStreamConsumer로 넘깁니다.
 *
 * @fileName : SearchLogStreamConfig
 * @since : 2026-11-18
 */
@Slf4j
@Configuration
public class SearchLogStreamConfig {

    @Bean(destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> searchLogListenerContainer(
            RedisConnectionFactory connectionFactory,
            StringRedisTemplate redisTemplate,
            SearchLogStreamConsumer consumer) {

        createGroupIfAbsent(redisTemplate);

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .errorHandler(e -> log.warn("검색 로그 스트림 읽기 오류: {}", e.getMessage()))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        // receive(): 자동 ACK 하지 않음. ES 저장 성공 후 컨슈머가 직접 ACK
        container.receive(
                Consumer.from(SearchLogStreamConsumer.GROUP, SearchLogStreamConsumer.CONSUMER),
                StreamOffset.create(SearchLogPublisher.STREAM_KEY, ReadOffset.lastConsumed()),
                consumer);
        container.start();
        return container;
    }

    private void createGroupIfAbsent(StringRedisTemplate redisTemplate) {
        try {
            // 스트림이 없으면 같이 만든다 (MKSTREAM)
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection ->
                    connection.execute("XGROUP",
                            "CREATE".getBytes(),
                            SearchLogPublisher.STREAM_KEY.getBytes(),
                            SearchLogStreamConsumer.GROUP.getBytes(),
                            "0".getBytes(),
                            "MKSTREAM".getBytes()));
            log.info("검색 로그 컨슈머 그룹 생성: {}", SearchLogStreamConsumer.GROUP);
        } catch (Exception e) {
            // 이미 그룹이 있으면 BUSYGROUP 오류가 나므로 무시
            if (e.getMessage() == null || !e.getMessage().contains("BUSYGROUP")) {
                log.warn("검색 로그 컨슈머 그룹 생성 실패: {}", e.getMessage());
            }
        }
    }
}
