import jakarta.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.*;
import java.util.Base64;
import java.util.Properties;

public class ImagePublisherClient {

    protected static final String url = "tcp://localhost:61617"; // URL-ul brokerului

    public static void main(String[] args) {
        String topicName = null;
        Context jndiContext = null;
        TopicConnectionFactory topicConnectionFactory = null;
        TopicConnection topicConnection = null;
        TopicSession topicSession = null;
        Topic topic = null;
        TopicPublisher topicPublisher = null;

        if (args.length != 2) {
            System.out.println("Usage: java ImagePublisherClient <topic-name> <image-path>");
            System.exit(1);
        }

        topicName = args[0];
        String imagePath = args[1];
        System.out.println("Publishing image to topic: " + topicName);

        // JNDI context look-up + look-up factory & topic
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, url);

            jndiContext = new InitialContext(props);

            topicConnectionFactory = (TopicConnectionFactory) jndiContext.lookup("ConnectionFactory");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        try {
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = topicSession.createTopic(topicName);
            topicPublisher = topicSession.createPublisher(topic);

            // Citirea imaginii și trimiterea mesajului binar
            byte[] imageBytes = readImage(imagePath);
            sendImageMessage(topicSession, topicPublisher, imageBytes);

        } catch (JMSException e) {
            e.printStackTrace();
        } finally {
            if (topicConnection != null) {
                try {
                    topicConnection.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Citește imaginea într-un array de byte-uri
    private static byte[] readImage(String imagePath) throws IOException {
        File imageFile = new File(imagePath);
        byte[] imageBytes = new byte[(int) imageFile.length()];

        try (FileInputStream fis = new FileInputStream(imageFile)) {
            fis.read(imageBytes);
        }

        return imageBytes;
    }

    // Trimite imaginea ca un BytesMessage
    private static void sendImageMessage(TopicSession topicSession, TopicPublisher topicPublisher, byte[] imageBytes) throws JMSException {
        BytesMessage message = topicSession.createBytesMessage();
        message.writeBytes(imageBytes);
        topicPublisher.send(message);
        System.out.println("Image sent to topic");
    }
}
