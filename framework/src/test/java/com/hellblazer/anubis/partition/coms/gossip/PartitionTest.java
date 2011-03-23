/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
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
package com.hellblazer.anubis.partition.coms.gossip;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.annotations.DeployedPostProcessor;
import com.hellblazer.anubis.partition.coms.gossip.configuration.ControllerGossipConfiguration;
import com.hellblazer.anubis.partition.coms.gossip.configuration.GossipConfiguration;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PartitionTest extends TestCase {
    static class MyController extends Controller {

        public MyController(Timer timer, long checkPeriod, long expirePeriod,
                            Identity partitionIdentity, long heartbeatTimeout,
                            long heartbeatInterval) {
            super(timer, checkPeriod, expirePeriod, partitionIdentity,
                  heartbeatTimeout, heartbeatInterval);
        }

        @Override
        protected NodeData createNode(Heartbeat hb) {
            return new Node(hb, this);
        }

    }

    @Configuration
    static class MyControllerConfig extends ControllerGossipConfiguration {

        @Override
        @Bean
        public DeployedPostProcessor deployedPostProcessor() {
            return new DeployedPostProcessor();
        }

        @Override
        public int magic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Controller constructController() throws UnknownHostException {
            return new MyController(timer(), 1000, 300000, partitionIdentity(),
                                    heartbeatTimeout(), heartbeatInterval());
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact1(), seedContact2());
        }

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort2);
        }

    }

    static class Node extends NodeData {
        CyclicBarrier barrier       = INITIAL_BARRIER;
        boolean       barrierBroken = false;
        int           cardinality   = CONFIGS.length;
        boolean       interrupted   = false;

        public Node(Heartbeat hb, Controller controller) {
            super(hb, controller);
        }

        @Override
        protected void partitionNotification(View partition, int leader) {
            log.finer("Partition notification: " + partition);
            super.partitionNotification(partition, leader);
            if (partition.isStable() && partition.cardinality() == cardinality) {
                interrupted = false;
                barrierBroken = false;
                Thread testThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            interrupted = true;
                            return;
                        } catch (BrokenBarrierException e) {
                            barrierBroken = true;
                        }
                    }
                }, "Stability test thread for: " + getIdentity());
                testThread.setDaemon(true);
                testThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        e.printStackTrace();
                    }
                });
                testThread.start();
            }
        }
    }

    @Configuration
    static class node0 extends nodeCfg {
        @Override
        public int node() {
            return 0;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact1();
        }
    }

    @Configuration
    static class node1 extends nodeCfg {
        @Override
        public int node() {
            return 1;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact2();
        }
    }

    @Configuration
    static class node10 extends nodeCfg {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends nodeCfg {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends nodeCfg {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends nodeCfg {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends nodeCfg {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends nodeCfg {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends nodeCfg {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends nodeCfg {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends nodeCfg {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends nodeCfg {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends nodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends nodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends nodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends nodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends nodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends nodeCfg {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends nodeCfg {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends nodeCfg {
        @Override
        public int node() {
            return 9;
        }
    }

    static class nodeCfg extends GossipConfiguration {

        @Override
        public int getMagic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact1(), seedContact2());
        }

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort2);
        }
    }

    @SuppressWarnings("rawtypes")
    final static Class[]                     CONFIGS = { node0.class,
            node1.class, node2.class, node3.class, node4.class, node5.class,
            node6.class, node7.class, node8.class, node9.class, node10.class,
            node11.class, node12.class, node13.class, node14.class,
            node15.class, node16.class, node17.class, node18.class,
            node19.class                            };
    static CyclicBarrier                     INITIAL_BARRIER;
    static int                               testPort1;
    static int                               testPort2;
    private static final Logger              log     = Logger.getLogger(PartitionTest.class.getCanonicalName());
    static {
        String port = System.getProperty("com.hellblazer.anubis.partition.coms.gossip.test.port.1",
                                         "24010");
        testPort1 = Integer.parseInt(port);
        port = System.getProperty("com.hellblazer.anubis.partition.coms.gossip.test.port.2",
                                  "24020");
        testPort2 = Integer.parseInt(port);
    }

    MyController                             controller;
    AnnotationConfigApplicationContext       controllerContext;
    List<AnnotationConfigApplicationContext> memberContexts;
    List<Node>                               partition;

    /**
     * Test that a partition can form two asymmetric partitions, with one
     * stabilizing, and then reform the original partition.
     */
    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        CyclicBarrier barrierA = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CyclicBarrier barrierB = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionB.add(member);
                member.barrier = barrierA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionA.add(member);
                member.barrier = barrierB;
                member.cardinality = minorPartitionSize;
            }
        }
        log.info("asymmetric partitioning: " + A);
        controller.asymPartition(A);
        log.info("Awaiting stability of minor partition A");
        barrierA.await(60, TimeUnit.SECONDS);
        // The other partition should still be unstable.
        assertEquals(0, barrierB.getNumberWaiting());

        View viewA = partitionA.get(0).getView();
        for (Node member : partitionA) {
            assertFalse(member.barrierBroken);
            assertFalse(member.interrupted);
            assertEquals(viewA, member.getView());
        }

        // reform
        CyclicBarrier barrier = new CyclicBarrier(CONFIGS.length + 1);
        for (Node node : partition) {
            assertFalse(node.barrierBroken);
            assertFalse(node.interrupted);
            node.barrier = barrier;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        barrier.await(60, TimeUnit.SECONDS);
    }

    /**
     * Test that a partition can form two stable sub partions and then reform
     * the original partition.
     */
    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        CyclicBarrier barrierA = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CyclicBarrier barrierB = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionB.add(member);
                member.barrier = barrierA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionA.add(member);
                member.barrier = barrierB;
                member.cardinality = minorPartitionSize;
            }
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stability of minor partition A");
        barrierA.await(60, TimeUnit.SECONDS);
        log.info("Awaiting stability of minor partition B");
        barrierB.await(60, TimeUnit.SECONDS);

        View viewA = partitionA.get(0).getView();
        for (Node member : partitionA) {
            assertFalse(member.barrierBroken);
            assertFalse(member.interrupted);
            assertEquals(viewA, member.getView());
        }

        View viewB = partitionB.get(0).getView();
        for (Node member : partitionB) {
            assertFalse(member.barrierBroken);
            assertFalse(member.interrupted);
            assertEquals(viewB, member.getView());
        }

        // reform
        CyclicBarrier barrier = new CyclicBarrier(CONFIGS.length + 1);
        for (Node node : partition) {
            node.barrier = barrier;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        barrier.await(60, TimeUnit.SECONDS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log.info("Setting up initial partition");
        INITIAL_BARRIER = new CyclicBarrier(CONFIGS.length + 1);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   MyControllerConfig.class);
        memberContexts = createMembers();
        controller = (MyController) controllerContext.getBean(Controller.class);
        log.info("Awaiting initial partition stability");
        boolean success = false;
        try {
            INITIAL_BARRIER.await(120, TimeUnit.SECONDS);
            success = true;
            log.info("Initial partition stable");
            partition = new ArrayList<PartitionTest.Node>();
            for (AnnotationConfigApplicationContext context : memberContexts) {
                partition.add((Node) controller.getNode(context.getBean(Identity.class)));
            }
        } finally {
            if (!success) {
                tearDown();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        testPort1++;
        testPort2++;
        if (controllerContext != null) {
            try {
                controllerContext.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        controllerContext = null;
        if (memberContexts != null) {
            for (AnnotationConfigApplicationContext context : memberContexts) {
                try {
                    context.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        memberContexts = null;
        controller = null;
        partition = null;
        INITIAL_BARRIER = null;
        Thread.sleep(10000);
    }

    private List<AnnotationConfigApplicationContext> createMembers() {
        ArrayList<AnnotationConfigApplicationContext> contexts = new ArrayList<AnnotationConfigApplicationContext>();
        for (Class<?> config : CONFIGS) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }
}