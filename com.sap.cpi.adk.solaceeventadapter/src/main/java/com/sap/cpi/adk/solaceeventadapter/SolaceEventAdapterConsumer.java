/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sap.cpi.adk.solaceeventadapter;

import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
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


/**
 * The Sample.com consumer.
 */
public class SolaceEventAdapterConsumer extends ScheduledPollConsumer {
    private Logger LOG = LoggerFactory.getLogger(SolaceEventAdapterConsumer.class);

    private final SolaceEventAdapterEndpoint endpoint;
    private JCSMPSession session;

    public SolaceEventAdapterConsumer(final SolaceEventAdapterEndpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        
    	Runnable worker = new MyRunnable();
	    Thread workerTask = new Thread(worker);
        // We can set the name of the thread
	    workerTask.setName("SolaceEventListnerThread");
        // Start the thread, never call method run() direct
	    workerTask.start();
     LOG.info("Finished all threads");
    }

      
    
    class MyRunnable implements Runnable {

        @Override
        public void run() {

       	 try {
       	    	
       	    	initializeSession();
       	    	
       	    	receiveMessages();
       	    	
       			} catch (Exception e) {
       				LOG.error("ems service error: ", e);
       			}
       		
        }
    }
    
    
    private void initializeSession() {
		try {
			LOG.info("Init Solace Session");
			String solaceHost=null;
			String vmr=null;
			
			solaceHost=endpoint.getSolaceHost();
			
			if(solaceHost==null) {
				solaceHost="35.188.9.216";
				//solaceHost="localhost";
			}
	
			vmr=endpoint.getVmr();
			
			if(vmr==null) {
				vmr="default";
				
			}
	
			
	        JCSMPProperties properties = new JCSMPProperties();
	        properties.setProperty(JCSMPProperties.HOST, solaceHost);     // host:port
	        properties.setProperty(JCSMPProperties.USERNAME, "admin"); // client-username
	       properties.setProperty(JCSMPProperties.PASSWORD, "admin"); // client-password
	       properties.setProperty(JCSMPProperties.VPN_NAME,  vmr); // message-vpn
	       session = JCSMPFactory.onlyInstance().createSession(properties);
	        session.connect();

			LOG.info("Solace Session establishment was succesfull");
		}
		catch (Exception e) {
			LOG.error("Problem in creating solace connection", e);
		}
		

	}
	
	
    
    
    public void receiveMessages() {
    	
    	new Thread(new Runnable() {
    	    @Override public void run() {
    	        // do stuff in this thread
    	      	  XMLMessageConsumer cons=null;
    	    	try {
    	    	 Topic topic = JCSMPFactory.onlyInstance().createTopic(endpoint.getTopic());
    	    	     
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
    	                	
    	                	LOG.info("Event received:"+ recvMessage);
    	                	
    	                	Exchange exchange = null;;
    	    			      	                 	
    	                           // send message to next processor in the route
    	    			     try {
    	    			    	 exchange = endpoint.createExchange();
	    			      	      
    	    			    	 	 exchange.getIn().setBody(recvMessage);
    	    	    			     exchange.getOut().setBody(recvMessage);
    	    	    			     getProcessor().process(exchange);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								log.error("Failed to process exchange",e);
							}
    	    			     finally {
    	    			            // log exception if an exception occurred and was not handled
    	    			            if (exchange.getException() != null) {
    	    			                getExceptionHandler().handleException("Error processing exchange", exchange, exchange.getException());
    	    			            }
    	    			        }
    	                	
    	                } 
    	                   latch.countDown();  // unblock main thread
    	            }

    	            @Override
    	            public void onException(JCSMPException e) {
    	               	LOG.error("Consumer received exception:",e);   	
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

	@Override
	protected int poll() throws Exception {
		// TODO Auto-generated method stub
		
	 if((session==null) || session.isClosed()) {
    		
		Runnable worker = new MyRunnable();
		
	    Thread workerTask = new Thread(worker);
	    // We can set the name of the thread
	    workerTask.setName("SolaceEventListnerThread");
        // Start the thread, never call method run() direct
	    workerTask.start();
       LOG.info("Creating new session");
		 }
		return 0;
	}
    
    
    
    
    
    
}
