package com.bancoxyz.cuentas.messaging;

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
    public NewTopic movimientoAplicadoTopic() {
        return topic(Topics.MOVIMIENTO_APLICADO);
    }

    @Bean
    public NewTopic transaccionRegistradaDlt() {
        return topic(Topics.TRANSACCION_REGISTRADA + Topics.DLT_SUFFIX);
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> template) {
        return KafkaErrorHandling.errorHandler(template, "cuentas-service");
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(Topics.PARTICIONES).replicas(1).build();
    }
}
