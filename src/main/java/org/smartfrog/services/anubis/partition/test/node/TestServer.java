/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.test.node;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionFactory;
import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionServer;

public class TestServer extends ConnectionServer implements ConnectionFactory {

    private TestMgr testManager;

    public TestServer(TestMgr testManager, String host, int port,
                      String threadName) throws IOException {
        super(threadName, host, port);
        this.testManager = testManager;
        setConnectionFactory(this);
    }

    @Override
    public void createConnection(SocketChannel channel) {
        testManager.newConnection(channel);
    }
}