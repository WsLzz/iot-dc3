/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.manager.service.rabbit;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.rabbitmq.client.Channel;
import io.github.pnoker.center.manager.service.BatchService;
import io.github.pnoker.center.manager.service.DriverSdkService;
import io.github.pnoker.center.manager.service.EventService;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.driver.EventConstant;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.DriverEvent;
import io.github.pnoker.common.entity.driver.DriverConfiguration;
import io.github.pnoker.common.entity.driver.DriverRegister;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接收驱动发送过来的驱动事件数据
 * 其中包括：驱动心跳事件、在线、离线、故障等其他事件
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DriverEventReceiver {

    @Resource
    private RedisUtil redisUtil;
    @Resource
    private EventService eventService;
    @Resource
    private BatchService batchService;
    @Resource
    private DriverSdkService driverSdkService;

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @RabbitHandler
    @RabbitListener(queues = "#{driverEventQueue.name}")
    public void driverEventReceive(Channel channel, Message message, DriverEvent driverEvent) {
        try {
            MessageProperties properties = message.getMessageProperties();
            channel.basicAck(properties.getDeliveryTag(), true);
            if (ObjectUtil.isNull(driverEvent) || CharSequenceUtil.isEmpty(driverEvent.getServiceName())) {
                log.error("Invalid driver event {}", driverEvent);
                return;
            }

            log.debug("Driver {} event, From: {}, Received: {}", driverEvent.getType(), message.getMessageProperties().getReceivedRoutingKey(), driverEvent);
            String routingKey = RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + driverEvent.getServiceName();

            switch (driverEvent.getType()) {
                case EventConstant.Driver.HANDSHAKE:
                    handshakeEvent(routingKey);
                    break;
                case EventConstant.Driver.REGISTER:
                    registerEvent(driverEvent, routingKey);
                    break;
                case EventConstant.Driver.METADATA_SYNC:
                    metadataEvent(driverEvent, routingKey);
                    break;
                case EventConstant.Driver.STATUS:
                    statusEvent(driverEvent);
                    break;
                case EventConstant.Driver.ERROR:
                    //TODO 去重
                    threadPoolExecutor.execute(() -> eventService.addDriverEvent(driverEvent));
                    break;
                default:
                    log.error("Invalid event type, {}", driverEvent.getType());
                    break;
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 处理握手事件
     *
     * @param routingKey Routing Key
     */
    private void handshakeEvent(String routingKey) {
        DriverConfiguration driverConfiguration = new DriverConfiguration(
                PrefixConstant.DRIVER,
                EventConstant.Driver.HANDSHAKE_BACK,
                null,
                ResponseEnum.OK
        );
        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_METADATA,
                routingKey,
                driverConfiguration
        );
    }

    /**
     * 处理注册事件
     *
     * @param driverEvent DriverEvent
     * @param routingKey  Routing Key
     */
    private void registerEvent(DriverEvent driverEvent, String routingKey) {
        DriverConfiguration driverConfiguration = new DriverConfiguration(
                PrefixConstant.DRIVER,
                EventConstant.Driver.REGISTER_BACK,
                null,
                ResponseEnum.OK
        );
        try {
            driverSdkService.driverRegister(Convert.convert(DriverRegister.class, driverEvent.getContent()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            driverConfiguration.setResponse(ResponseEnum.FAILURE);
        }
        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_METADATA,
                routingKey,
                driverConfiguration
        );
    }

    /**
     * 处理元数据同步事件
     *
     * @param driverEvent DriverEvent
     * @param routingKey  Routing Key
     */
    private void metadataEvent(DriverEvent driverEvent, String routingKey) {
        DriverConfiguration driverConfiguration = new DriverConfiguration(
                PrefixConstant.DRIVER,
                EventConstant.Driver.METADATA_SYNC_BACK,
                null,
                ResponseEnum.OK
        );
        try {
            driverConfiguration.setContent(batchService.batchDriverMetadata(driverEvent.getServiceName()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            driverConfiguration.setResponse(ResponseEnum.FAILURE);
        }
        rabbitTemplate.convertAndSend(
                RabbitConstant.TOPIC_EXCHANGE_METADATA,
                routingKey,
                driverConfiguration
        );
    }

    /**
     * 处理状态事件
     *
     * @param driverEvent DriverEvent
     */
    private void statusEvent(DriverEvent driverEvent) {
        redisUtil.setKey(
                PrefixConstant.DRIVER_STATUS_KEY_PREFIX + driverEvent.getServiceName(),
                driverEvent.getContent(),
                driverEvent.getTimeOut(),
                driverEvent.getTimeUnit()
        );
    }
}
