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
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.Status;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.test.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.test.stats.StatsManager;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

public class TestMgr {
    private static final Logger       log           = Logger.getLogger(TestMgr.class.getCanonicalName());
    private static final long         STATSRATE     = 5;
    private final Set<TestConnection> connections   = new CopyOnWriteArraySet<TestConnection>();
    private TestServer                connectionServer;
    private ConnectionSet             connectionSet = null;
    private volatile long             lastStats     = 0;
    private PartitionManager          partitionManager;
    private final StatsManager        statistics    = new StatsManager();
    private volatile long             statsInterval = STATSRATE * 1000;                                  // adjusts with heartbeat
    // timing
    private boolean                   testable      = true;

    public TestMgr(InetSocketAddress endpoint,
                   PartitionManager partitionManager, int id,
                   ConnectionSet connectionSet, boolean testable)
                                                                 throws IOException,
                                                                 Exception {
        this.partitionManager = partitionManager;
        String threadName = "Anubis: Partition Manager Test Node (node " + id
                            + ") - connection server";
        connectionServer = new TestServer(this, endpoint, threadName);
        this.connectionSet = connectionSet;
        this.testable = testable;
    }

    public void closing(TestConnection connection) {
        partitionManager.deregister(connection);
        connections.remove(connection);
    }

    public InetSocketAddress getAddress() {
        return connectionServer.getAddress();
    }

    public boolean isTestable() {
        return testable;
    }

    public void newConnection(SocketChannel channel) {
        String threadName = "Anubis: Partition Manager Test Node (node "
                            + partitionManager.getId() + ") - connection";
        TestConnection connection = new TestConnection(channel, this,
                                                       threadName);
        if (connection.connected()) {
            connections.add(connection);
            partitionManager.register(connection);
            updateStatus(connection);
            updateTiming(connection);
            connection.start();
        }
    }

    public void schedulingInfo(long time, long delay) {
        statistics.schedulingInfo(time, delay);
        updateStats(time);
    }

    /**
     * set the nodes to ignore
     * 
     * @param ignoring
     */
    public void setIgnoring(View ignoring) {
        connectionSet.setIgnoring(ignoring);
        updateIgnoring(ignoring);
    }

    public void setTiming(long interval, long timeout) {
        connectionSet.setTiming(interval, timeout);
        updateTiming();
        statsInterval = STATSRATE * interval;
    }

    @PostConstruct
    public void start() throws IOException {

        if (!testable) {
            return;
        }

        if (!testable) {
            terminate();
            return;
        }
        connectionSet.registerTestManager(this);
        connectionServer.start();
    }

    @PreDestroy
    public void terminate() {
        if (testable) {
            connectionServer.shutdown();
            Iterator<TestConnection> iter = connections.iterator();
            while (iter.hasNext()) {
                iter.next().shutdown();
            }
        }
    }

    public void updateHeartbeat(Heartbeat hb) {
        HeartbeatMsg heartbeatMsg = HeartbeatMsg.toHeartbeatMsg(hb);
        byte[] wire;
        try {
            wire = heartbeatMsg.toWire();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unable to deserialize heartbeat", e);
            return;
        }
        for (TestConnection tc : connections) {
            tc.send(wire);
        }
    }

    public void updateIgnoring(View ignoring) {
        Iterator<TestConnection> iter = connections.iterator();
        while (iter.hasNext()) {
            updateIgnoring(ignoring, iter.next());
        }
    }

    public void updateIgnoring(View ignoring, TestConnection tc) {
        tc.sendObject(new IgnoringMsg(ignoring));
    }

    public void updateStats(long timenow) {
        if (lastStats < timenow - statsInterval) {
            Iterator<TestConnection> iter = connections.iterator();
            while (iter.hasNext()) {
                updateStats(iter.next());
            }
            lastStats = timenow;
        }
    }

    public void updateStats(TestConnection tc) {
        tc.sendObject(statistics.statsMsg());
    }

    public void updateStatus(TestConnection tc) {
        Status status = partitionManager.getStatus();
        tc.sendObject(new PartitionMsg(status.view, status.leader));
    }

    public void updateThreads(TestConnection tc) {
        String status = connectionSet.getThreadStatusString();
        tc.sendObject(new ThreadsMsg(status));
    }

    public void updateTiming() {
        Iterator<TestConnection> iter = connections.iterator();
        while (iter.hasNext()) {
            updateTiming(iter.next());
        }
    }

    public void updateTiming(TestConnection tc) {
        tc.sendObject(new TimingMsg(connectionSet.getInterval(),
                                    connectionSet.getTimeout()));
    }

}
