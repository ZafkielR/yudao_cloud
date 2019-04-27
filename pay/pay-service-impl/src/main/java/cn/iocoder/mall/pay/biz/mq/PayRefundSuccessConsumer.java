package cn.iocoder.mall.pay.biz.mq;

import cn.iocoder.mall.pay.api.message.PayRefundSuccessMessage;
import cn.iocoder.mall.pay.biz.component.DubboReferencePool;
import cn.iocoder.mall.pay.biz.dao.PayRefundMapper;
import cn.iocoder.mall.pay.biz.dataobject.PayRefundDO;
import com.alibaba.dubbo.rpc.service.GenericService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Date;

@Service
@RocketMQMessageListener(
        topic = PayRefundSuccessMessage.TOPIC,
        consumerGroup = "pay-consumer-group-" + PayRefundSuccessMessage.TOPIC
)
public class PayRefundSuccessConsumer extends AbstractPayNotifySuccessConsumer<PayRefundSuccessMessage>
        implements RocketMQListener<PayRefundSuccessMessage> {

    @Autowired
    private PayRefundMapper payRefundMapper;

    @Override
    protected String invoke(PayRefundSuccessMessage message, DubboReferencePool.ReferenceMeta referenceMeta) {
        // 查询支付交易
        PayRefundDO refund = payRefundMapper.selectById(message.getRefundId());
        Assert.notNull(refund, String.format("回调消息(%s) 退款单不能为空", message.toString()));
        // 执行调用
        GenericService genericService = referenceMeta.getService();
        String methodName = referenceMeta.getMethodName();
        return (String) genericService.$invoke(methodName, new String[]{String.class.getName(), Integer.class.getName()},
                new Object[]{message.getOrderId(), refund.getPrice()});
    }

    @Override
    protected void afterInvokeSuccess(PayRefundSuccessMessage message) {
        PayRefundDO updateRefund = new PayRefundDO().setId(message.getRefundId()).setFinishTime(new Date());
        payRefundMapper.update(updateRefund, null);
    }

}