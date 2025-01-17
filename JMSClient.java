package apachetomeejms;

import jakarta.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;

public class JMSClient {

    protected static final String url = "tcp://localhost:61617";  // Broker URL

    public static void main(String[] args) {
        String topicName = null;
        Context jndiContext = null;
        TopicConnectionFactory topicConnectionFactory = null;
        TopicConnection topicConnection = null;
        TopicSession topicSession = null;
        Topic topic = null;
        TopicSubscriber topicSubscriber = null;
        TextListener topicListener = null;
        InputStreamReader inputStreamReader = null;
        char answer = '\0';

        // Dacă nu există argumente, folosim valoarea implicită
        if (args.length != 2) {
            System.out.println("Imaginea se afla la adresa http://172.17.0.4 TOPIC:Topic_DAD");
            System.exit(1);
        }

        topicName = args[1];  // Numele topicului
        System.out.println("Topic name = " + topicName);

        // Configurarea JNDI context + look-up factory & topic
        try {
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, args[0]);  // URL-ul brokerului

            jndiContext = new InitialContext(props);
            topicConnectionFactory = (TopicConnectionFactory) jndiContext.lookup("ConnectionFactory");
            topic = (Topic) jndiContext.lookup(topicName);
        } catch (NamingException ne) {
            ne.printStackTrace();
            System.exit(2);
        }

        // Conectarea la broker și abonarea la topic
        try {
            topicConnection = topicConnectionFactory.createTopicConnection();
            topicSession = topicConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
            topicSubscriber = topicSession.createSubscriber(topic);

            // Setarea listener-ului pentru mesaje
            topicListener = new TextListener();
            topicSubscriber.setMessageListener(topicListener);

            // Pornirea conexiunii
            topicConnection.start();

            System.out.println("Se asteapta primirea imaginii... Pentru a inchide programul, apasa q + CR/LF");
            inputStreamReader = new InputStreamReader(System.in);
            while (!(answer == 'q')) {
                try {
                    answer = (char) inputStreamReader.read();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        } catch (JMSException jmse) {
            jmse.printStackTrace();
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

    // Listener care procesează mesajele primite
    static class TextListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            TextMessage msg = null;

            try {
                if (message instanceof TextMessage) {
                    msg = (TextMessage) message;
                    String jsonMessage = msg.getText();

                    // Parsarea mesajului JSON fără org.json
                    // Extragem manual valorile din JSON
                    String imageBase64 = extractJsonValue(jsonMessage, "image");
                    String fileName = extractJsonValue(jsonMessage, "fileName");

                    // Decodarea imaginii din Base64
                    byte[] imageBytes = Base64.getDecoder().decode(imageBase64);

                    // Salvarea imaginii într-un fișier
                    try (FileOutputStream fos = new FileOutputStream(new File(fileName))) {
                        fos.write(imageBytes);
                        System.out.println("Imaginea a fost salvată în fișier");
                    }

                } else {
                    System.out.println("Mesajul nu este de tip TextMessage!");
                }
            } catch (JMSException jmse) {
                jmse.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // Funcție pentru a extrage valoarea unui câmp JSON manual
        private String extractJsonValue(String jsonMessage, String key) {
            int keyIndex = jsonMessage.indexOf("\"" + key + "\":");
            if (keyIndex == -1) {
                return null;
            }

            int startIndex = jsonMessage.indexOf("\"", keyIndex + key.length() + 3) + 1;
            int endIndex = jsonMessage.indexOf("\"", startIndex);

            if (startIndex == -1 || endIndex == -1) {
                return null;
            }

            return jsonMessage.substring(startIndex, endIndex);
        }
    }
}
