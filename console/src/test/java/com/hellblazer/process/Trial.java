/** 
 * (C) Copyright 2011 Hal Hildebrand, all rights reserved.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.process;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.smartfrog.services.anubis.partition.test.controller.gui.GraphicController;
import org.smartfrog.services.anubis.partition.test.controller.gui.TestControllerConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import sun.management.ConnectorAddressLink;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;

import com.sun.jmx.remote.internal.RMIExporter;

/**
 * @author Hal Hildebrand
 * 
 */
public class Trial  {
	private static AnnotationConfigApplicationContext controllerContext;
	private static GraphicController controller;
    public Trial()
    {
    	System.out.println("crap");
    }
    public void calling()
    {
    	System.out.println("alesso");
    }
    public static void hello()
    {

    	System.out.println("calling another method");
    }
	protected static Class<?> getControllerConfig() {
		return TestControllerConfig.class;
	}
	
    public static void main(String[] argv) throws Exception {
    	System.out.println("crapshit");
		controllerContext = new AnnotationConfigApplicationContext(
				getControllerConfig());
		
		controller = controllerContext.getBean(GraphicController.class);

		
    	for(int i = 0; i < argv.length; i++)
    	{
    		Class config = Class.forName(argv[i]);
        	AnnotationConfigApplicationContext controllerContext = new AnnotationConfigApplicationContext(config);

    	}
//    	hello();
//    	CallingThis call = new CallingThis();
//    	call.printThis();
//    	System.out.println("hello");
//    	Class config = Class.forName(argv[0]);
//    	new AnnotationConfigApplicationContext(config);

//    	while(checker)
//    	{
    		Thread.sleep(500000000);
//    	}

    }

   

}
