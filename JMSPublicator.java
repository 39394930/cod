package apachetomeejms;

import jakarta.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.*;
import java.util.Base64;
import java.util.Properties;

public class JMSPublicator extends JavalinServer {
	 public static Properties getProp(String ip, String port) {
		 Properties props = new Properties();
		 props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
		 "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		 //props.setProperty(Context.PROVIDER_URL, "tcp://localhost:61617");
	         props.setProperty(Context.PROVIDER_URL, "tcp://"+ip+":"+port);
		 return props;
	 }
    public static void main(String[] args) {
        // Configurăm și trimitem imaginea la brokerul ActiveMQ
        try {
            // Creăm contextul JNDI și obținem conexiunea
            InitialContext jndiContext = new InitialContext(getProp(args[0],args[1]));
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");
            Connection connection = connectionFactory.createConnection();
            connection.start();  // Pornim conexiunea

            // Creăm sesiunea și topic-ul pentru publicare
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createTopic("jms/topic/test"); // Topicul unde trimitem mesajele

            // Creăm producer-ul pentru a trimite mesaje pe topic
            MessageProducer producer = session.createProducer(destination);

            // Citim fișierul imagine și îl codificăm în Base64
            File imageFile = new File("C:\\Users\\Stefania\\Documents\\Imagine.bmp");  // Înlocuiește cu calea fișierului tău
            if (imageFile.exists()) {
                byte[] imageData = new byte[(int) imageFile.length()];
                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    fis.read(imageData);
                }

                // Codificăm imaginea în Base64
                String imageBase64 = Base64.getEncoder().encodeToString(imageData);

                // Construim mesajul JSON cu imaginea codificată și numele fișierului
                String jsonMessage = "{"
                        + "\"image\":\"" + imageBase64 + "\","
                        + "\"fileName\":\"" + imageFile.getName() + "\""
                        + "}";

                // Creăm mesajul JMS de tip TextMessage
                TextMessage message = session.createTextMessage(jsonMessage);

                // Trimiterea mesajului către topic
                producer.send(message);
                System.out.println("Imaginea a fost trimisă pe topic!");

            } else {
                System.out.println("Fișierul imagine nu a fost găsit.");
            }

            // Închidem conexiunea
            connection.close();

        } catch (JMSException | NamingException | IOException e) {
            e.printStackTrace();
        }
    }
}