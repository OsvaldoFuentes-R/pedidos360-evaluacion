package cl.duoc.pedidos360_bff.messaging.tickets;

import cl.duoc.pedidos360_bff.config.RabbitMQProperties;
import cl.duoc.pedidos360_bff.messaging.ConsumidorConAck;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Consumidor de la cola de generación de tickets de despacho. */
@Component
public class PedidoTicketConsumer extends ConsumidorConAck {

    public PedidoTicketConsumer(RabbitMQProperties props) {
        super(props.tickets().queue());
    }

    @RabbitListener(queues = "${pedidos360.rabbitmq.tickets.queue}", ackMode = "MANUAL")
    public void recibir(String mensaje, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesar(mensaje, channel, deliveryTag);
    }
}