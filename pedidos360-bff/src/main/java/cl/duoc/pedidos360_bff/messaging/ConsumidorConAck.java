package cl.duoc.pedidos360_bff.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Lógica común de los consumidores con ACK manual.
 *
 * <ul>
 *   <li>Mensaje válido: se procesa y se confirma (ACK).</li>
 *   <li>Mensaje inválido: se rechaza sin reencolar (NACK). Como las colas tienen
 *       configurado el DLX, RabbitMQ lo envía a su DLQ y aquí queda en el log.</li>
 * </ul>
 *
 * <p>Cada dominio (registro, notificaciones, tickets) tiene su propio consumidor
 * en su paquete y solo declara el {@code @RabbitListener}.</p>
 */
public abstract class ConsumidorConAck {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String nombreCola;

    protected ConsumidorConAck(String nombreCola) {
        this.nombreCola = nombreCola;
    }

    protected void procesar(String mensaje, Channel channel, long deliveryTag) throws IOException {
        try {
            validar(mensaje);
            log.info("[{}] Mensaje procesado (ACK): {}", nombreCola, mensaje);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.warn("[{}] Mensaje FALLIDO -> se envía a la DLQ (NACK): '{}' | motivo: {}",
                    nombreCola, mensaje, e.getMessage());
            // requeue = false: no vuelve a la cola, pasa al DLX y llega a la DLQ.
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Regla de validación de ejemplo para poder probar el flujo de errores.
     * Se puede cambiar aquí (o sobrescribir en un consumidor) sin tocar el resto.
     */
    protected void validar(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("el mensaje está vacío");
        }
        if (mensaje.toLowerCase().contains("error")) {
            throw new IllegalArgumentException("el mensaje contiene datos inválidos");
        }
    }
}