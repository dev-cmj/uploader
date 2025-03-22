package com.project.common.config;

import com.project.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import java.util.HashMap;
import java.util.Map;

import static com.project.common.constants.RabbitMQConstants.*;

@Configuration
public class RabbitMQConfig {
    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(RabbitMQConstants.CONTENT_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(RabbitMQConstants.DLX_EXCHANGE);
    }

    @Bean
    public Queue validationRequestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put(RabbitMQConstants.X_DEAD_LETTER_EXCHANGE, RabbitMQConstants.DLX_EXCHANGE);
        args.put(RabbitMQConstants.X_DEAD_LETTER_ROUTING_KEY, RabbitMQConstants.DLQ_ROUTING_KEY);
        args.put(RabbitMQConstants.X_MESSAGE_TTL, RabbitMQConstants.MESSAGE_TTL);
        return new Queue(VALIDATION_REQUEST_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue validationResultQueue() {
        return new Queue(VALIDATION_RESULT_QUEUE, true);
    }

    @Bean
    public Queue statusUpdateQueue() {
        return new Queue(RabbitMQConstants.STATUS_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(RabbitMQConstants.DLQ_QUEUE, true);
    }

    @Bean
    public Binding validationRequestBinding(Queue validationRequestQueue, TopicExchange contentExchange) {
        return BindingBuilder
                .bind(validationRequestQueue)
                .to(contentExchange)
                .with(VALIDATION_REQUEST_ROUTING_KEY);
    }

    @Bean
    public Binding validationResultBinding(Queue validationResultQueue, TopicExchange contentExchange) {
        return BindingBuilder
                .bind(validationResultQueue)
                .to(contentExchange)
                .with(VALIDATION_RESULT_ROUTING_KEY);
    }

    @Bean
    public Binding statusUpdateBinding(Queue statusUpdateQueue, TopicExchange contentExchange) {
        return BindingBuilder
                .bind(statusUpdateQueue)
                .to(contentExchange)
                .with(RabbitMQConstants.STATUS_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder
                .bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(RabbitMQConstants.DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}