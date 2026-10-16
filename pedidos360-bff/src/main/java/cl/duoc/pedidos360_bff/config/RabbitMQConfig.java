package cl.duoc.pedidos360_bff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Topología de RabbitMQ de Pedidos360: exchanges, colas, DLQs y bindings.
 *
 * <p>Esta clase solo declara infraestructura; no contiene lógica de negocio.
 * Spring Boot declara estos beans en el broker al conectarse (RabbitAdmin).</p>
 *
 * <pre>
 *  pedidos360.exchange (direct)
 *     ├─ pedido.registro     ──► pedidos.registro
 *     ├─ pedido.notificacion ──► pedidos.notificaciones
 *     └─ pedido.ticket       ──► pedidos.tickets
 *
 *  (mensaje rechazado o expirado en una cola principal)
 *  pedidos360.dlx (topic)
 *     ├─ dlq.pedido.registro     ──► pedidos.registro.dlq
 *     ├─ dlq.pedido.notificacion ──► pedidos.notificaciones.dlq
 *     └─ dlq.pedido.ticket       ──► pedidos.tickets.dlq
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(RabbitMQProperties.class)
public class RabbitMQConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);

    private static final String ARG_ALTERNATE_EXCHANGE = "alternate-exchange";
    private static final String ARG_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String ARG_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    private final RabbitMQProperties props;

    public RabbitMQConfig(RabbitMQProperties props) {
        this.props = props;
    }

    // ------------------------------------------------------------------
    // Exchanges
    // ------------------------------------------------------------------

    /**
     * Exchange principal: enruta por routing key exacta a cada cola.
     * Los mensajes cuya routing key no coincide con ningún binding se envían
     * al exchange alternativo (alternate-exchange) en vez de descartarse.
     */
    @Bean
    public DirectExchange pedidosExchange() {
        return new DirectExchange(props.exchange(), true, false,
                Map.<String, Object>of(ARG_ALTERNATE_EXCHANGE, props.unroutableExchange()));
    }

    /** Dead Letter Exchange: recibe los mensajes que no pudieron procesarse. */
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(props.dlx(), true, false);
    }

    // ------------------------------------------------------------------
    // Colas principales (cada una apunta al DLX con la routing key de su DLQ)
    // ------------------------------------------------------------------

    @Bean
    public Queue registroQueue() {
        return mainQueue(props.registro());
    }

    @Bean
    public Queue notificacionesQueue() {
        return mainQueue(props.notificaciones());
    }

    @Bean
    public Queue ticketsQueue() {
        return mainQueue(props.tickets());
    }

    // ------------------------------------------------------------------
    // Dead Letter Queues
    // ------------------------------------------------------------------

    @Bean
    public Queue registroDlq() {
        return QueueBuilder.durable(props.registro().dlq()).build();
    }

    @Bean
    public Queue notificacionesDlq() {
        return QueueBuilder.durable(props.notificaciones().dlq()).build();
    }

    @Bean
    public Queue ticketsDlq() {
        return QueueBuilder.durable(props.tickets().dlq()).build();
    }

    // ------------------------------------------------------------------
    // Bindings: cola principal <- exchange principal (routing key)
    // ------------------------------------------------------------------

    @Bean
    public Binding registroBinding() {
        return BindingBuilder.bind(registroQueue())
                .to(pedidosExchange())
                .with(props.registro().routingKey());
    }

    @Bean
    public Binding notificacionesBinding() {
        return BindingBuilder.bind(notificacionesQueue())
                .to(pedidosExchange())
                .with(props.notificaciones().routingKey());
    }

    @Bean
    public Binding ticketsBinding() {
        return BindingBuilder.bind(ticketsQueue())
                .to(pedidosExchange())
                .with(props.tickets().routingKey());
    }

    // ------------------------------------------------------------------
    // Bindings: DLQ <- DLX (routing key de dead letter)
    // ------------------------------------------------------------------

    @Bean
    public Binding registroDlqBinding() {
        return BindingBuilder.bind(registroDlq())
                .to(deadLetterExchange())
                .with(props.registro().dlqRoutingKey());
    }

    @Bean
    public Binding notificacionesDlqBinding() {
        return BindingBuilder.bind(notificacionesDlq())
                .to(deadLetterExchange())
                .with(props.notificaciones().dlqRoutingKey());
    }

    @Bean
    public Binding ticketsDlqBinding() {
        return BindingBuilder.bind(ticketsDlq())
                .to(deadLetterExchange())
                .with(props.tickets().dlqRoutingKey());
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Mensajes sin ruta (routing key que no coincide con ningún binding)
    // ------------------------------------------------------------------

    /** Exchange alternativo: recibe lo que el exchange principal no pudo enrutar. */
    @Bean
    public FanoutExchange unroutableExchange() {
        return new FanoutExchange(props.unroutableExchange(), true, false);
    }

    /** Cola donde caen los mensajes sin ruta; un consumidor los deja en el log. */
    @Bean
    public Queue unroutableQueue() {
        return QueueBuilder.durable(props.unroutableQueue()).build();
    }

    @Bean
    public Binding unroutableBinding() {
        return BindingBuilder.bind(unroutableQueue()).to(unroutableExchange());
    }

    /**
     * Spring solo declara colas y exchanges cuando abre su primera conexión.
     * Sin consumidores ni productores aún, abrimos una al arrancar para que la
     * topología se cree en RabbitMQ. Si el broker no está, solo avisa en el log.
     */
    @Bean
    public ApplicationRunner declararTopologia(ConnectionFactory connectionFactory) {
        return args -> {
            try {
                connectionFactory.createConnection();
            } catch (AmqpException e) {
                log.warn("No se pudo conectar a RabbitMQ; la topología no se declaró: {}", e.getMessage());
            }
        };
    }

    /** Cola durable con dead-lettering hacia el DLX de la topología. */
    private Queue mainQueue(RabbitMQProperties.Route route) {
        return QueueBuilder.durable(route.queue())
                .withArgument(ARG_DEAD_LETTER_EXCHANGE, props.dlx())
                .withArgument(ARG_DEAD_LETTER_ROUTING_KEY, route.dlqRoutingKey())
                .build();
    }
}