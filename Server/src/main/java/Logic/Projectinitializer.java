package Logic;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import Mqtt.MQTTBroker;
import Mqtt.MQTTPublisher;
import Mqtt.MQTTSuscriber;

/**
 * ES: Clase encargada de inicializar el sistema y de lanzar el hilo de
 * previsión meteorológica EN: Class in charge of initializing the thread of
 * weather forecast
 */
@WebListener
public class Projectinitializer implements ServletContextListener {
    private MQTTSuscriber suscriber;

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // shutdown background tasks
        try {
            Database.RangeDAO.shutdownScheduler();
        } catch (Exception e) {
            Log.log.error("Error during contextDestroyed: {}", e);
        }
    }

    @Override
    /**
     * ES: Metodo empleado para detectar la inicializacion del servidor	<br>
     * EN: Method used to detect server initialization
     *
     * @param sce <br>
     * ES: Evento de contexto creado durante el arranque del servidor	<br>
     * EN: Context event created during server launch
     */
    public void contextInitialized(ServletContextEvent sce) {
        Log.log.info("-->Suscribe Topics<--");
        MQTTBroker broker = new MQTTBroker();
        suscriber = new MQTTSuscriber(broker);
        // Subscriptions loaded from DB by the subscriber during construction
        // Make subscriber available to servlets via ServletContext
        sce.getServletContext().setAttribute("mqttSubscriber", suscriber);
        MQTTPublisher.publish(broker, "test", "Hello from Tomcat :)");

        // Ensure parameter ranges cache is loaded at startup (blocking one-time load)
        try {
            new Database.RangeDAO().reloadAllRanges();
            Log.log.info("Parameter ranges loaded at startup");
        } catch (Exception e) {
            Log.log.error("Error loading parameter ranges at startup: {}", e);
        }
    }
}
