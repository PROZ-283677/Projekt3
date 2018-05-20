package game;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.Queue;

public class PTPProducer {
	private String id;
	
	public PTPProducer(String id){
		this.id = id;
	}
	
	public void sendQueueMessage(String msg) {
		ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
		try {
			((com.sun.messaging.ConnectionFactory) connectionFactory)
					.setProperty(com.sun.messaging.ConnectionConfiguration.imqAddressList, "localhost:7676/jms");
			
			JMSContext jmsContext = connectionFactory.createContext();
			JMSProducer jmsProducer = jmsContext.createProducer();
			Queue queue = new com.sun.messaging.Queue("ATJQueue");
			
			Message message = jmsContext.createMessage();
			message.setStringProperty("MSG", msg);
			message.setStringProperty("ID", id);

			jmsProducer.send(queue, message);
			System.out.printf("Wiadomość '%s' została wysłana.\n", message.getStringProperty("MSG"));
			
			jmsContext.close();
		}
		catch(JMSException e) { e.printStackTrace(); }
	}
	
}