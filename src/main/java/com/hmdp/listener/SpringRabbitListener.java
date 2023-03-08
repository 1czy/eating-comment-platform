package com.hmdp.listener;

import com.hmdp.service.IVoucherOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.QUEUE_NAME;

@Component
public class SpringRabbitListener {
    @Resource
    private IVoucherOrderService voucherOrderService;
    @RabbitListener(queues = QUEUE_NAME)
    public void seckillOrderListener(Map<String,Long> map){
        voucherOrderService.createVocherOrder(map);
    }
}
