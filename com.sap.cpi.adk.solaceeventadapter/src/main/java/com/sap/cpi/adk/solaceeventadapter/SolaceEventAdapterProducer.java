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

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * The www.Sample.com producer.
 */
public class SolaceEventAdapterProducer extends DefaultProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(SolaceEventAdapterProducer.class);
    private SolaceEventAdapterEndpoint endpoint;
    private JCSMPSession session;
	public SolaceEventAdapterProducer(SolaceEventAdapterEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        initializeSession();
    }

    public void process(final Exchange exchange) throws Exception {
        String input = exchange.getIn().getBody(String.class);
		sendMessages(input);
		exchange.getOut().setBody(input);
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
		
    
 public void sendMessages(final String messageContent) {
    	
    	new Thread(new Runnable() {
    	    @Override public void run() {
    	        // do stuff in this thread
    	    	
    	    	try {
    	       	 Topic topic = JCSMPFactory.onlyInstance().createTopic(endpoint.getTopic());
    	       	 
    	         	 if((session==null) || session.isClosed()) {
    	         		
    	         		 LOG.warn("invalid solace session");
    	         		 
    	       		 initializeSession();
    	       	 }
    	              	     

    	            /** Anonymous inner-class for handling publishing events */
    	            XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
    	                @Override
    	                public void responseReceived(String messageID) {
    	               	 LOG.info("Producer received response for msg: " + messageID);
    	                }
    	                @Override
    	                public void handleError(String messageID, JCSMPException e, long timestamp) {
    	               	 LOG.error("Producer received error for msg:",
    	                            e);
    	                }
    	            });
    	            // Publish-only session is now hooked up and running!

    	            TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
    	         
    	            msg.setText(messageContent);
    	            LOG.info("Connected. About to send message '%s' to topic '%s'...%n",messageContent,topic.getName());
    	            prod.send(msg,topic);
    	            LOG.info("Message sent. Exiting.");
    	            
    	            session.closeSession();
    	       	}catch(Exception e) {
    	       		
    	       		LOG.error("failed to publish events:",e);
    	       	}
    	    	
    	    }
    	}).start();

    	

	}
    
}
