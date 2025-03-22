package com.project.common.constants;


/**
 * RabbitMQ 관련 상수 정의
 */
public class RabbitMQConstants {

    // Exchange 이름
    public static final String CONTENT_EXCHANGE = "content.exchange";
    public static final String DLX_EXCHANGE = "content.dlx.exchange";

    // Queue 이름
    public static final String UPLOAD_QUEUE = "content.upload.queue";
    public static final String VALIDATION_QUEUE = "content.validation.queue";
    public static final String PROCESSING_QUEUE = "content.processing.queue";
    public static final String STORAGE_QUEUE = "content.storage.queue";
    public static final String NOTIFICATION_QUEUE = "content.notification.queue";
    public static final String DLQ_QUEUE = "content.dlq.queue";
    public static final String STATUS_UPDATE_QUEUE = "content.status.update.queue";

    // Routing Key
    public static final String UPLOAD_ROUTING_KEY = "content.upload";
    public static final String VALIDATION_ROUTING_KEY = "content.validation";
    public static final String PROCESSING_ROUTING_KEY = "content.processing";
    public static final String STORAGE_ROUTING_KEY = "content.storage";
    public static final String NOTIFICATION_ROUTING_KEY = "content.notification";
    public static final String DLQ_ROUTING_KEY = "content.dlq";
    public static final String STATUS_UPDATE_ROUTING_KEY = "content.status";

    public static final String VALIDATION_REQUEST_QUEUE = "content.validation.request.queue";
    public static final String VALIDATION_RESULT_QUEUE = "content.validation.result.queue";

    public static final String VALIDATION_REQUEST_ROUTING_KEY = "content.validation.request";
    public static final String VALIDATION_RESULT_ROUTING_KEY = "content.validation.result";

    // Content Type Routing Keys
    public static final String IMAGE_ROUTING_KEY = "content.type.image";
    public static final String VIDEO_ROUTING_KEY = "content.type.video";
    public static final String AUDIO_ROUTING_KEY = "content.type.audio";
    public static final String PDF_ROUTING_KEY = "content.type.pdf";
    public static final String TEXT_ROUTING_KEY = "content.type.text";
    public static final String OTHER_ROUTING_KEY = "content.type.other";

    // Header Keys
    public static final String X_RETRY_COUNT = "x-retry-count";
    public static final String X_PRIORITY = "x-priority";
    public static final String X_ORIGINAL_EXCHANGE = "x-original-exchange";
    public static final String X_ORIGINAL_ROUTING_KEY = "x-original-routing-key";

    // 처리 우선순위
    public static final int PRIORITY_HIGH = 10;
    public static final int PRIORITY_NORMAL = 5;
    public static final int PRIORITY_LOW = 1;

    // Queue 속성
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String X_MESSAGE_TTL = "x-message-ttl";

    // 재시도 제한
    public static final int MAX_RETRY_COUNT = 3;

    // 메시지 TTL (밀리초)
    public static final int MESSAGE_TTL = 60000; // 1분
}