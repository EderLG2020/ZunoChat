package com.example.backend.module.messagemanagement.realtime.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Configuración de RabbitMQ — solo activa cuando rabbitmq.enabled=true.
 * Crea su propio ConnectionFactory para evitar que el
 * RabbitAutoConfiguration de Spring Boot intente conectar en startup.
 */
@Configuration
@ConditionalOnProperty(name = "rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfig {

    public static final String EXCHANGE_DIRECT  = "zunochat.direct";
    public static final String EXCHANGE_FANOUT  = "zunochat.fanout";
    public static final String EXCHANGE_DLX     = "zunochat.dlx";

    public static final String QUEUE_MESSAGES      = "q.messages";
    public static final String QUEUE_READ_RECEIPTS = "q.read.receipts";
    public static final String QUEUE_PRESENCE      = "q.presence";
    public static final String QUEUE_DEAD_LETTER   = "q.dead.letter";

    public static final String RK_MESSAGE  = "chat.message";
    public static final String RK_READ     = "chat.read";
    public static final String RK_PRESENCE = "chat.presence";

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.port}")
    private int port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    /**
     * ConnectionFactory propia — desacoplada del autoconfigure de Spring Boot.
     * Solo se crea si rabbitmq.enabled=true.
     */
    @Bean
    public ConnectionFactory rabbitConnectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost(virtualHost);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        return factory;
    }

    @Bean public DirectExchange directExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DIRECT).durable(true).build();
    }
    @Bean public FanoutExchange fanoutExchange() {
        return ExchangeBuilder.fanoutExchange(EXCHANGE_FANOUT).durable(true).build();
    }
    @Bean public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLX).durable(true).build();
    }
    @Bean public Queue messagesQueue() {
        return QueueBuilder.durable(QUEUE_MESSAGES)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "dead")
                .withArgument("x-message-ttl", 86_400_000)
                .build();
    }
    @Bean public Queue readReceiptsQueue() {
        return QueueBuilder.durable(QUEUE_READ_RECEIPTS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX).build();
    }
    @Bean public Queue presenceQueue() {
        return QueueBuilder.durable(QUEUE_PRESENCE)
                .withArgument("x-message-ttl", 30_000).build();
    }
    @Bean public Queue deadLetterQueue() {
        return QueueBuilder.durable(QUEUE_DEAD_LETTER).build();
    }
    @Bean public Binding bindMessages(Queue messagesQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(messagesQueue).to(directExchange).with(RK_MESSAGE);
    }
    @Bean public Binding bindReadReceipts(Queue readReceiptsQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(readReceiptsQueue).to(directExchange).with(RK_READ);
    }
    @Bean public Binding bindPresence(Queue presenceQueue, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(presenceQueue).to(fanoutExchange);
    }
    @Bean public Binding bindDeadLetter(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dead");
    }
    @Bean public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory rabbitConnectionFactory) {
        RabbitTemplate template = new RabbitTemplate(rabbitConnectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) System.err.println("RabbitMQ: mensaje no confirmado — " + cause);
        });
        return template;
    }
    @Bean public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10_000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
    @Bean public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory rabbitConnectionFactory) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(rabbitConnectionFactory);
        f.setMessageConverter(jsonMessageConverter());
        f.setAdviceChain(retryInterceptor());
        f.setConcurrentConsumers(3);
        f.setMaxConcurrentConsumers(10);
        f.setPrefetchCount(5);
        f.setDefaultRequeueRejected(false);
        return f;
    }
}