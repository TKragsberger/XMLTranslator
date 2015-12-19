
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Thomas
 */
public class XmlTranslator {
    public static ConnectionFactory getConnection() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("datdb.cphbusiness.dk");
        factory.setPort(5672);
        factory.setUsername("student");
        factory.setPassword("cph");
        return factory;
    }

    public static String getXml(String ssn, String creditScore, String loanAmount, String loanDuration) throws ParserConfigurationException, SAXException, IOException {
        String xml = "<LoanRequest>\n"
                + "<ssn>" + ssn + "</ssn>"
                + "<creditScore>" + creditScore + "</creditScore>"
                + "<loanAmount>" + loanAmount + "</loanAmount>"
                + "<loanDuration>" + loanDuration + "</loanDuration>"
                + "</LoanRequest>";
        return xml;
    }

    private final static String QUEUE_NAME_BANK = "group10.bankXML";
    private final static String EXCHANGE_NAME = "cphbusiness.bankXML";
    private final static String REPLY_QUEUE_NAME = "group10.replyChannel.bankXML";

    public static void main(String[] args) throws IOException {

        Connection connection = getConnection().newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_NAME_BANK, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
                    throws IOException {
                String message = new String(body, "UTF-8");
                try {
                    String[]parts = message.split(">>>");
                    message = getXml(parts[0], parts[3], parts[1], parts[2]);
                } catch (ParserConfigurationException ex) {
                    Logger.getLogger(XmlTranslator.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SAXException ex) {
                    Logger.getLogger(XmlTranslator.class.getName()).log(Level.SEVERE, null, ex);
                }
                AMQP.BasicProperties.Builder replyChannel = new AMQP.BasicProperties.Builder();
                replyChannel.replyTo(REPLY_QUEUE_NAME);
                System.out.println(" [x] Received '" + message + "'");
                Connection connection2 = getConnection().newConnection();
                Channel channel2 = connection2.createChannel();
                channel2.exchangeDeclare(EXCHANGE_NAME, "fanout");
                channel2.basicPublish(EXCHANGE_NAME, "", replyChannel.build(), message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");
                channel2.close();
                connection2.close();
            }
        };

        channel.basicConsume(QUEUE_NAME_BANK, true, consumer);
    }
}
