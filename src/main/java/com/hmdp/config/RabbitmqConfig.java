package com.hmdp.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hmdp.utils.RedisConstants.QUEUE_NAME;

/**
 * @author CZY
 */
@Configuration
public class RabbitmqConfig {
    /**
     * 创建队列
     * @return
     */
    @Bean
    public Queue creatOrderQueue(){
        return new Queue(QUEUE_NAME);
    }
}
