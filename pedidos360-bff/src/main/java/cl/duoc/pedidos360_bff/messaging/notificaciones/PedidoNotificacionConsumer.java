package cl.duoc.pedidos360_bff.messaging.notificaciones;

import cl.duoc.pedidos360_bff.config.RabbitMQProperties;
import cl.duoc.pedidos360_bff.messaging.ConsumidorConAck;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor de la cola de notificaciones al cliente. */
@Component
public class PedidoNotificacionConsumer extends ConsumidorConAck {

    public PedidoNotificacionConsumer(RabbitMQProperties props) {
        super(props.notificaciones().queue());
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.notificaciones.queue}", ackMode = "MANUAL")
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesar(mensaje, channel, deliveryTag);
    }
}