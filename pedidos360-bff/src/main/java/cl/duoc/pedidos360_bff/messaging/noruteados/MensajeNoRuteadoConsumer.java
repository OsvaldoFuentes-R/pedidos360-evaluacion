package cl.duoc.pedidos360_bff.messaging.noruteados;

import cl.duoc.pedidos360_bff.config.RabbitMQProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Registra en el log los mensajes que llegaron al exchange principal con una
 * routing key que no coincide con ningún binding (por ejemplo, mal escrita).
 */
@Component
public class MensajeNoRuteadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(MensajeNoRuteadoConsumer.class);

    private final RabbitMQProperties props;

    public MensajeNoRuteadoConsumer(RabbitMQProperties props) {
        this.props = props;
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.unroutable-queue}")
    public void recibir(Message message) {
        String cuerpo = new String(message.getBody(), StandardCharsets.UTF_8);
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        log.error("[NO RUTEADO] La routing key '{}' no coincide con ningún binding de '{}'. Mensaje descartado: '{}'",
                routingKey, props.exchange(), cuerpo);
    }
}