package game;

import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;

public class PTPConsumer {
	private JMSConsumer jmsConsumer;
	private JMSContext jmsContext;
	
	public PTPConsumer(Game game, String id) {
		ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
		jmsContext = connectionFactory.createContext();
		try {
			Queue queue = new com.sun.messaging.Queue("ATJQueue");
			jmsConsumer = jmsContext.createConsumer(queue, "ID <> '" + id + "'");
			jmsConsumer.setMessageListener(game);
		}
		catch(JMSException e) { e.printStackTrace(); }
	}
	
	public void close() {
		jmsConsumer.close();
		jmsContext.close();
	}
	
	public void emptyQueue() {
		ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
		JMSContext jmsContext = connectionFactory.createContext();
		try {
			((com.sun.messaging.ConnectionFactory) connectionFactory)
					.setProperty(com.sun.messaging.ConnectionConfiguration.imqAddressList,"localhost:7676/jms");
			Queue queue = new com.sun.messaging.Queue("ATJQueue");
			JMSConsumer jmsConsumer = jmsContext.createConsumer(queue);
			Message msg;
			while ((msg = jmsConsumer.receive(10)) != null) {
				System.out.printf("Pobrano wiadomosc '%s'\n", msg.getStringProperty("TEXT"));
			}
			jmsConsumer.close();
			System.out.println("Zakonczono odbior");
		}
		catch (JMSException e) { e.printStackTrace(); }
		jmsContext.close();
	}
}
