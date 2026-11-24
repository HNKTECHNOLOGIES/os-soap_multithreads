package fr.evaloc;

import fr.evaloc.config.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Endpoint;

/**
 * Point d'entrée du POC autonome : publie l'endpoint JAX-WS via {@link Endpoint#publish}.
 * <p>
 * WSDL : {@code http://localhost:8080/ws/declarations?wsdl}
 */
public final class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) {
        final ApplicationContext context = ApplicationContext.bootstrap();
        String url = context.soapEndpointUrl();

        Endpoint endpoint = Endpoint.publish(url, context.getDeclarationWebService());
        LOG.info("OS SOAP EVALOC démarré sur {} (WSDL: {}?wsdl)", url, url);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    endpoint.stop();
                } catch (Exception ignored) {
                    // ignore
                }
                context.close();
                LOG.info("OS SOAP EVALOC arrêté");
            }
        }, "evaloc-shutdown"));

        // Garde le process vivant
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            endpoint.stop();
            context.close();
        }
    }
}
