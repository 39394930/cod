package apachetomeejms;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.http.staticfiles.Location;
import jakarta.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Base64;
import java.util.Properties;

public class JavalinServer {

    private static Connection connection;
    private static Session session;
    private static MessageProducer producer;
    private static Topic topic;

    public static void main(String[] args) {
        // Configurăm serverul Javalin
    	 Javalin app = Javalin.create(javalinConfig -> {
             javalinConfig.staticFiles.add("./staticHTML", Location.EXTERNAL);
         }).start(7000);  
        // Endpoint pentru încărcarea imaginii
        app.post("/upload-image", ctx -> {
            // Verificăm dacă brokerul ActiveMQ este activ pe portul 61617
            if (isBrokerAvailable("172.17.0.2", 61617)) {
                // Obținem fișierul încărcat
                UploadedFile imageFile = ctx.uploadedFile("image");

                if (imageFile != null) {
                    // Trimitem imaginea către JMS Topic
                    sendImageToJmsTopic(imageFile);
                    ctx.result("Imaginea a fost încărcată și trimisă în Topic.");
                } else {
                    ctx.status(400).result("Fișierul nu a fost încărcat corect.");
                }
            } else {
                ctx.status(500).result("Eroare: Brokerul ActiveMQ nu este activ pe portul 61617.");
            }
        });

        // Încercăm să stabilim conexiunea JMS
        try {
            setupJmsConnection();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    // Verificăm dacă brokerul ActiveMQ este activ pe portul specificat
    private static boolean isBrokerAvailable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000); // Timeout de 2 secunde
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // Configurăm conexiunea JMS și Topic-ul
    private static void setupJmsConnection() throws JMSException {
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, "tcp://172.17.0.2:61617");
            InitialContext jndiContext = new InitialContext(props);

            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = session.createTopic("Topic_DAD");
            producer = session.createProducer(topic);
        } catch (Exception e) {
            e.printStackTrace();
            throw new JMSException("Eroare la inițializarea conexiunii JMS: " + e.getMessage());
        }
    }

    // Trimitem imaginea către JMS Topic
    private static void sendImageToJmsTopic(UploadedFile imageFile) {
        try {
            byte[] imageData = imageFile.content().readAllBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageData);
            String jsonMessage = "{ \"image\":\"" + imageBase64 + "\", \"fileName\":\"" + imageFile.filename() + "\" }";

            TextMessage message = session.createTextMessage(jsonMessage);
            producer.send(message);
            System.out.println("Imaginea a fost trimisă în topic!");
        } catch (JMSException | IOException e) {
            e.printStackTrace();
        }
    }
}
