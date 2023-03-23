package com.sap.cpi.adk.solaceeventadapter;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;

public class TestSession {
    private static Logger LOG = LoggerFactory.getLogger(TestSession.class);
    public static JCSMPSession session;
	public static void main(String[] args) {
		// TODO Auto-generated method stub

			try {
				initializeSession();
       	    	
       	    	receiveMessages();
			}
			catch (Exception e) {
				e.printStackTrace();
				LOG.error("Problem in creating solace connection", e);
			}
			

		
		
	}
	
	   private static void initializeSession() {
			try {
				System.out.println("Init Solace Session");
				String solaceHost=null;
				String vmr=null;
				
				solaceHost="tcps://mrrfgrw3e2g04.messaging.solace.cloud:55443";

				vmr="myservicemesh";

				
		        JCSMPProperties properties = new JCSMPProperties();
		        properties.setProperty(JCSMPProperties.HOST, solaceHost);     // host:port
		        properties.setProperty(JCSMPProperties.USERNAME, "xxxx"); // client-username
		       properties.setProperty(JCSMPProperties.PASSWORD, "xxxxxx"); // client-password
		       properties.setProperty(JCSMPProperties.VPN_NAME,  vmr); // message-vpn
		       session = JCSMPFactory.onlyInstance().createSession(properties);
		        session.connect();

		        System.out.println("Solace Session establishment was succesfull");
			}
			catch (Exception e) {
				LOG.error("Problem in creating solace connection", e);
			}
			

		}
		
		
	    
	    
	    public static void receiveMessages() {
	    	
	    	new Thread(new Runnable() {
	    	    @Override public void run() {
	    	        // do stuff in this thread
	    	      	  XMLMessageConsumer cons=null;
	    	    	try {
	    	    	 Topic topic = JCSMPFactory.onlyInstance().createTopic("testcpi");
	    	    	 
	    	    	 System.out.println("receiveMessages");
	    	    	 
	    	    	 if((session==null) || session.isClosed()) {
	    	    		 initializeSession();
	    	    	 }
	    	    	 final CountDownLatch latch = new CountDownLatch(1);       
	    	                                                            // synchronizing b/w threads
	    	        /** Anonymous inner-class for MessageListener
	    	         *  This demonstrates the async threaded message callback */
	    	            cons = session.getMessageConsumer(new XMLMessageListener() {
	    	            @Override
	    	            public void onReceive(BytesXMLMessage msg) {
	    	                if (msg instanceof TextMessage) {
	    	                	String recvMessage= ((TextMessage)msg).getText();
	    	                	
	    	                	System.out.println("Event received:"+ recvMessage);
	    	                	
	    	                    	                	
	    	                } 
	    	                   latch.countDown();  // unblock main thread
	    	            }

	    	            @Override
	    	            public void onException(JCSMPException e) {
	    	            	System.out.println("Consumer received exception:"+ e);   	
	    	               	latch.countDown(); 
	    	            }
	    	        });
	    	        session.addSubscription(topic);
	    	        LOG.info("Solace Client Connected. Awaiting message...");
	    	        cons.start();
	    	        // Consume-only session is now hooked up and running!
	    	        latch.await(); 
	    	        
	    	    	}catch(Exception e) {
	    	    		LOG.error("failed to receive events: %s%n",e);
	    	    	}
	    	    	
	    	     if((cons!=null) && (session!=null)) {

	    	       // Close consumer
	    	       cons.close();
	     	       session.closeSession();
	    	    	}
	     	       //LOG.info("Closed Solace session");
	    	    }
	    	}).start();
		}

	

}
