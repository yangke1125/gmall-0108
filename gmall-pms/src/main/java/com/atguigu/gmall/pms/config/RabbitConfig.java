package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void init(){
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause)->{
            log.error("消息没有到达交换机，原因",cause);
        });
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey)->{
            log.error("消息没有到达队列，交换机：{}，路由器：{}，消息内容：{}",exchange,routingKey,new String(message.getBody()));
        });
    }
}
