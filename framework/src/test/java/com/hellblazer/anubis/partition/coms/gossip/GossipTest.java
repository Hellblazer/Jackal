package com.hellblazer.anubis.partition.coms.gossip;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;

public class GossipTest extends TestCase {

    public void testExamineAllNew() throws Exception {
        GossipCommunications communications = mock(GossipCommunications.class);
        GossipHandler gossipHandler = mock(GossipHandler.class);
        SystemView view = mock(SystemView.class);
        Random random = mock(Random.class);
        Identity localIdentity = new Identity(666, 0, 1);
        InetSocketAddress localAddress = new InetSocketAddress(0);
        when(view.getLocalAddress()).thenReturn(localAddress);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 1), 0,
                                    3);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 2), 0,
                                    1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 3), 2,
                                    1);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 4), 0,
                                    3);
        Digest digest1a = new Digest(new InetSocketAddress("google.com", 1), 0,
                                     -1);
        Digest digest2a = new Digest(new InetSocketAddress("google.com", 2), 0,
                                     -1);
        Digest digest3a = new Digest(new InetSocketAddress("google.com", 3), 2,
                                     -1);
        Digest digest4a = new Digest(new InetSocketAddress("google.com", 4), 0,
                                     -1);

        Gossip gossip = new Gossip(view, random, 4, localIdentity,
                                   communications, 4, TimeUnit.DAYS);

        gossip.examine(asList(digest1, digest2, digest3, digest4),
                       gossipHandler);

        verify(gossipHandler).reply(asList(digest1a, digest2a, digest3a,
                                           digest4a),
                                    new ArrayList<HeartbeatState>());
        verifyNoMoreInteractions(gossipHandler);
    }

    public void testExamineMixed() throws Exception {
        GossipCommunications communications = mock(GossipCommunications.class);
        GossipHandler gossipHandler = mock(GossipHandler.class);
        SystemView view = mock(SystemView.class);
        Random random = mock(Random.class);
        Identity localIdentity = new Identity(666, 0, 1);
        InetSocketAddress localAddress = new InetSocketAddress(0);
        when(view.getLocalAddress()).thenReturn(localAddress);

        InetSocketAddress address1 = new InetSocketAddress(1);
        InetSocketAddress address2 = new InetSocketAddress(2);
        InetSocketAddress address3 = new InetSocketAddress(3);
        InetSocketAddress address4 = new InetSocketAddress(4);

        Digest digest1 = new Digest(address1, 1, 3);
        Digest digest2 = new Digest(address2, 1, 1);
        Digest digest3 = new Digest(address3, 3, 4);
        Digest digest4 = new Digest(address4, 4, 3);

        Digest digest1a = new Digest(address1, 1, 0);
        Digest digest3a = new Digest(address3, 3, 3);

        HeartbeatState state1 = new HeartbeatState(null,
                                                   new Identity(666, 1, 0),
                                                   address1);
        state1.setViewNumber(1);

        HeartbeatState state2 = new HeartbeatState(null,
                                                   new Identity(666, 2, 2),
                                                   address2);
        state2.setViewNumber(2);

        HeartbeatState state3 = new HeartbeatState(null,
                                                   new Identity(666, 3, 3),
                                                   address3);
        state3.setViewNumber(3);

        HeartbeatState state4 = new HeartbeatState(null,
                                                   new Identity(666, 4, 4),
                                                   address4);
        state4.setViewNumber(4);

        Gossip gossip = new Gossip(view, random, 4, localIdentity,
                                   communications, 4, TimeUnit.DAYS);

        Field ep = Gossip.class.getDeclaredField("endpoints");
        ep.setAccessible(true);

        @SuppressWarnings("unchecked")
        ConcurrentMap<InetSocketAddress, Endpoint> endpoints = (ConcurrentMap<InetSocketAddress, Endpoint>) ep.get(gossip);

        endpoints.put(address1, new Endpoint(state1));
        endpoints.put(address2, new Endpoint(state2));
        endpoints.put(address3, new Endpoint(state3));
        endpoints.put(address4, new Endpoint(state4));

        gossip.examine(asList(digest1, digest2, digest3, digest4),
                       gossipHandler);

        verify(gossipHandler).reply(asList(digest1a, digest3a),
                                    asList(state2, state4));
        verifyNoMoreInteractions(gossipHandler);
    }

    public void testApplyDiscover() throws Exception {
        GossipCommunications communications = mock(GossipCommunications.class);
        HeartbeatReceiver receiver = mock(HeartbeatReceiver.class);
        SystemView view = mock(SystemView.class);
        Random random = mock(Random.class);
        Identity localIdentity = new Identity(666, 0, 1);
        InetSocketAddress localAddress = new InetSocketAddress(0);
        when(view.getLocalAddress()).thenReturn(localAddress);
        when(communications.getLocalAddress()).thenReturn(localAddress);

        InetSocketAddress address1 = new InetSocketAddress(1);
        InetSocketAddress address2 = new InetSocketAddress(2);
        InetSocketAddress address3 = new InetSocketAddress(3);
        InetSocketAddress address4 = new InetSocketAddress(4);

        HeartbeatState state1 = new HeartbeatState(null,
                                                   new Identity(666, 1, 0),
                                                   address1);
        HeartbeatState state2 = new HeartbeatState(null,
                                                   new Identity(666, 2, 1),
                                                   address2);
        HeartbeatState state3 = new HeartbeatState(null,
                                                   new Identity(666, 3, 3),
                                                   address3);
        HeartbeatState state4 = new HeartbeatState(null,
                                                   new Identity(666, 4, 4),
                                                   address4);

        Gossip gossip = new Gossip(view, random, 4, localIdentity,
                                   communications, 4, TimeUnit.DAYS);
        gossip.create(receiver);

        gossip.apply(asList(state1, state2, state3, state4));

        verify(communications).connect(eq(address1), isA(Endpoint.class),
                                       isA(Runnable.class));
        verify(communications).connect(eq(address2), isA(Endpoint.class),
                                       isA(Runnable.class));
        verify(communications).connect(eq(address3), isA(Endpoint.class),
                                       isA(Runnable.class));
        verify(communications).connect(eq(address4), isA(Endpoint.class),
                                       isA(Runnable.class));

        verify(communications).setGossip(gossip);

        verifyNoMoreInteractions(communications);
    }

    public void testApplyUpdate() throws Exception {
        GossipCommunications communications = mock(GossipCommunications.class);
        HeartbeatReceiver receiver = mock(HeartbeatReceiver.class);
        SystemView view = mock(SystemView.class);
        Random random = mock(Random.class);
        Identity localIdentity = new Identity(666, 0, 1);
        InetSocketAddress localAddress = new InetSocketAddress(0);
        when(view.getLocalAddress()).thenReturn(localAddress);
        when(communications.getLocalAddress()).thenReturn(localAddress);

        Endpoint ep1 = mock(Endpoint.class);
        Endpoint ep2 = mock(Endpoint.class);
        Endpoint ep3 = mock(Endpoint.class);
        Endpoint ep4 = mock(Endpoint.class);

        InetSocketAddress address1 = new InetSocketAddress(1);
        InetSocketAddress address2 = new InetSocketAddress(2);
        InetSocketAddress address3 = new InetSocketAddress(3);
        InetSocketAddress address4 = new InetSocketAddress(4);

        HeartbeatState state1 = new HeartbeatState(null,
                                                   new Identity(666, 1, 0),
                                                   address1);
        state1.setViewNumber(1);

        HeartbeatState state2 = new HeartbeatState(null,
                                                   new Identity(666, 2, 2),
                                                   address2);
        state2.setViewNumber(2);

        HeartbeatState state3 = new HeartbeatState(null,
                                                   new Identity(666, 3, 3),
                                                   address3);
        state3.setViewNumber(3);

        HeartbeatState state4 = new HeartbeatState(null,
                                                   new Identity(666, 4, 4),
                                                   address4);
        state4.setViewNumber(4);

        when(ep1.getEpoch()).thenReturn(0L);
        when(ep1.getVersion()).thenReturn(0L);
        when(ep1.getState()).thenReturn(state1);

        when(ep2.getEpoch()).thenReturn(3L);
        when(ep2.getVersion()).thenReturn(1L);

        when(ep3.getEpoch()).thenReturn(3L);
        when(ep3.getVersion()).thenReturn(0L);
        when(ep3.getState()).thenReturn(state3);

        when(ep4.getEpoch()).thenReturn(4L);
        when(ep4.getVersion()).thenReturn(5L);

        Gossip gossip = new Gossip(view, random, 4, localIdentity,
                                   communications, 4, TimeUnit.DAYS);
        gossip.create(receiver);

        Field ep = Gossip.class.getDeclaredField("endpoints");
        ep.setAccessible(true);

        @SuppressWarnings("unchecked")
        ConcurrentMap<InetSocketAddress, Endpoint> endpoints = (ConcurrentMap<InetSocketAddress, Endpoint>) ep.get(gossip);

        endpoints.put(address1, ep1);
        endpoints.put(address2, ep2);
        endpoints.put(address3, ep3);
        endpoints.put(address4, ep4);

        long now = System.currentTimeMillis();
        gossip.apply(asList(state1, state2, state3, state4));

        verify(ep1).getEpoch();
        verify(ep1, new Times(2)).getVersion();
        verify(ep1).updateState(now, state1);
        verify(ep1).getState();
        verifyNoMoreInteractions(ep1);

        verify(ep2).getEpoch();
        verifyNoMoreInteractions(ep2);

        verify(ep3).getEpoch();
        verify(ep3, new Times(2)).getVersion();
        verify(ep3).updateState(now, state3);
        verify(ep3).getState();
        verifyNoMoreInteractions(ep3);

        verify(ep4).getEpoch();
        verify(ep4).getVersion();
        verifyNoMoreInteractions(ep4);

        verify(communications).setGossip(gossip);

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(communications, new Times(2)).dispatch(captor.capture());

        for (Runnable action : captor.getAllValues()) {
            action.run();
        }

        verify(receiver).receiveHeartbeat(state1);
        verify(receiver).receiveHeartbeat(state3);
        verifyNoMoreInteractions(communications);
    }
}
