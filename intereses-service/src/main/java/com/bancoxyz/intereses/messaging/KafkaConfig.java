package com.bancoxyz.intereses.messaging;

import com.bancoxyz.common.events.Topics;
import com.bancoxyz.common.messaging.KafkaErrorHandling;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic interesRecalculadoTopic() {
        return topic(Topics.INTERES_RECALCULADO);
    }

    @Bean
    public NewTopic movimientoAplicadoDlt() {
        return topic(Topics.MOVIMIENTO_APLICADO + Topics.DLT_SUFFIX);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        return KafkaErrorHandling.errorHandler(template, "intereses-service");
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(Topics.PARTICIONES).replicas(1).build();
    }
}
