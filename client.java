import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Base64;

public class ImageReceiverClient {

    private static final String BROKER_URL = "tcp://localhost:61616";  // URL-ul brokerului
    private static final String TOPIC_NAME = "ImageTopic";  // Numele topicului de unde ascultă mesajele

    public static void receiveImage() throws JMSException {
        // Setăm configurările pentru broker
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, BROKER_URL);

            // Creăm conexiunea și sesiunea JMS
            Context context = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) context.lookup("ConnectionFactory");
            TopicConnection connection = (TopicConnection) connectionFactory.createConnection();
            TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            // Creăm topicul de la care vom primi mesajele
            Topic topic = (Topic) context.lookup(TOPIC_NAME);

            // Creăm un subscriber care va asculta mesajele pe topic
            TopicSubscriber subscriber = session.createSubscriber(topic);

            // Creăm un mesaj listener care va procesa mesajele primite
            subscriber.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    try {
                        if (message instanceof TextMessage) {
                            TextMessage textMessage = (TextMessage) message;
                            String base64Image = textMessage.getText();
                            System.out.println("Mesaj primit: " + base64Image);

                            // Decodificăm imaginea și o salvăm pe disc
                            saveImage(base64Image, "received_image.jpg");
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            connection.start();
            System.out.println("Clientul a început să asculte pentru mesaje...");
        } catch (NamingException | JMSException e) {
            e.printStackTrace();
        }
    }

    // Metoda pentru decodificarea unui mesaj Base64 și salvarea imaginii
    private static void saveImage(String base64Data, String filePath) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(decodedBytes);
                System.out.println("Imaginea a fost salvată la: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Începem să ascultăm pentru mesaje
            receiveImage();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
