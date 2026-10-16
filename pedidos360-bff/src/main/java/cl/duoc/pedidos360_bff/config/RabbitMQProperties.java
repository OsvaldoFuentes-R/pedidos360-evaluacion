package cl.duoc.pedidos360_bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nombres de la topología RabbitMQ, leídos desde application.yml
 * (prefijo "pedidos360.rabbitmq"). Así los nombres viven en un solo lugar.
 */
@ConfigurationProperties(prefix = "pedidos360.rabbitmq")
public record RabbitMQProperties(
        String exchange,
        String dlx,
        String unroutableExchange,
        String unroutableQueue,
        Route registro,
        Route notificaciones,
        Route tickets) {

    /**
     * Un caso de uso de mensajería: cola principal, su routing key
     * y la cola de mensajes fallidos (DLQ) con la routing key del DLX.
     */
    public record Route(
            String queue,
            String routingKey,
            String dlq,
            String dlqRoutingKey) {
    }
}