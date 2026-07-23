package com.elasticpom.external.integration;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaperListener {

    @KafkaListener(
            topics = "paper",
            groupId = "paper-service-consumer"
    )

    public void consume(String paper)  {

        System.out.println("Consuming paper " + paper);
    }

}
