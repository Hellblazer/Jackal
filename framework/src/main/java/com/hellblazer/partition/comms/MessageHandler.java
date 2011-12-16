/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.partition.comms;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.WireSizes;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;

import com.hellblazer.jackal.util.HexDump;
import com.hellblazer.pinkie.CommunicationsHandler;
import com.hellblazer.pinkie.SocketChannelHandler;

/**
 * 
 * @author hhildebrand
 * 
 */
public class MessageHandler implements IOConnection, CommunicationsHandler,
                WireSizes {
    private static enum State {
        BODY, ERROR, HEADER, INITIAL;
    };

    private static final Logger log = Logger.getLogger(MessageHandler.class.getCanonicalName());

    private static String toHex(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 4);
        PrintStream stream = new PrintStream(baos);
        HexDump.hexdump(stream, data, 0, data.length);
        stream.close();
        return baos.toString();
    }

    private boolean                       announceTerm = true;
    private ConnectionInitiator           ci;
    private final Semaphore               completion   = new Semaphore(1);
    private final ConnectionSet           connectionSet;
    private volatile SocketChannelHandler handler;
    private boolean                       ignoring     = false;
    private final Identity                me;
    private volatile MessageConnection    messageConnection;
    private final AtomicBoolean           open         = new AtomicBoolean();
    private ByteBuffer                    readBuffer;
    private volatile State                readState    = State.HEADER;
    private final AtomicLong              receiveCount = new AtomicLong(
                                                                        INITIAL_MSG_ORDER);
    private final ByteBuffer              rxHeader     = ByteBuffer.allocate(8);
    private final AtomicLong              sendCount    = new AtomicLong(
                                                                        INITIAL_MSG_ORDER - 1);
    private final WireSecurity            wireSecurity;
    private ByteBuffer                    writeBuffer;
    private volatile State                writeState   = State.INITIAL;
    private final ByteBuffer              wxHeader     = ByteBuffer.allocate(8);
    private final Executor                dispatcher;

    public MessageHandler(Executor dispatcher, WireSecurity wireSecurity,
                          Identity id, ConnectionSet cs) {
        this.wireSecurity = wireSecurity;
        me = id;
        connectionSet = cs;
        this.dispatcher = dispatcher;
    }

    public MessageHandler(Executor dispatcher, WireSecurity wireSecurity,
                          Identity id, ConnectionSet cs, MessageConnection con,
                          ConnectionInitiator ci) {
        this(dispatcher, wireSecurity, id, cs);
        messageConnection = con;
        this.ci = ci;
    }

    @Override
    public void accept(SocketChannelHandler handler) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Socket accepted");
        }
        this.handler = handler;
        open.set(true);
        handler.selectForRead();
    }

    @Override
    public void closing() { 
        if (log.isLoggable(Level.FINER)) {
            log.finer("closing is being called");
        }
        completion.release(100);
        if (announceTerm && messageConnection != null) {
            dispatcher.execute(new Runnable() {
                @Override
                public void run() {
                    messageConnection.closing();
                }
            });
        }
    }

    @Override
    public void connect(SocketChannelHandler handler) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Socket connected");
        }
        this.handler = handler;
        open.set(true);
        dispatcher.execute(new Runnable() {
            @Override
            public void run() {
                ci.handshake(MessageHandler.this);
            }
        });
        handler.selectForRead();
    }

    @Override
    public boolean connected() {
        return open.get();
    }

    public void deliverObject(ByteBuffer fullRxBuffer) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("deliverObject is being called");
        }

        if (ignoring) {
            return;
        }

        WireMsg msg = null;
        try {

            byte[] bytes = fullRxBuffer.array();
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("Delivering bytes: \n%s", toHex(bytes)));
            }
            msg = wireSecurity.fromWireForm(bytes);

        } catch (WireSecurityException ex) {
            log.log(Level.SEVERE,
                    format("%s non blocking connection transport encountered security violation unmarshalling message - ignoring the message ",
                           me), ex);
            return;

        } catch (Exception ex) {
            log.log(Level.SEVERE,
                    format("%s connection transport unable to unmarshall message ",
                           me), ex);
            shutdown();
            return;
        }

        if (!(msg instanceof TimedMsg)) {
            log.severe(format("%s connection transport received non timed message ",
                              me));
            shutdown();
            return;
        }

        final TimedMsg tm = (TimedMsg) msg;

        if (tm.getOrder() != receiveCount.get()) {
            log.severe(format("%s connection transport has delivered a message out of order - shutting down.  Expected: %s, received: %s",
                              me, receiveCount, tm.getOrder()));
            shutdown();
            return;
        }

        /**
         * handle the message. We do not increment the order for the initial
         * heartbeat message opening a new connection.
         */
        if (messageConnection == null) {
            dispatcher.execute(new Runnable() {
                @Override
                public void run() {
                    initialMsg(tm);
                }
            });
        } else {
            receiveCount.incrementAndGet();
            messageConnection.deliver(tm);
        }
    }

    @Override
    public void readReady() {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Socket read ready " + me);
        }
        switch (readState) {
            case HEADER: {
                if (!read(rxHeader)) {
                    return;
                }
                if (rxHeader.hasRemaining()) {
                    break;
                }
                rxHeader.clear();
                int readMagic = rxHeader.getInt();
                if (log.isLoggable(Level.FINER)) {
                    log.finer("Read magic number: " + readMagic);
                }
                if (readMagic == MAGIC_NUMBER) {
                    if (log.isLoggable(Level.FINER)) {
                        log.finer("RxHeader magic-number fits");
                    }
                    // get the object size and create a new buffer for it
                    int objectSize = rxHeader.getInt();
                    if (log.isLoggable(Level.FINER)) {
                        log.finer("read objectSize: " + objectSize);
                    }
                    readBuffer = ByteBuffer.wrap(new byte[objectSize]);
                    readState = State.BODY;
                } else {
                    log.severe(me + ": %  CANNOT FIND MAGIC_NUMBER:  "
                               + readMagic + " instead");
                    readState = State.ERROR;
                    shutdown();
                    return;
                }
                // Fall through to BODY state intended.
            }
            case BODY: {
                if (!read(readBuffer)) {
                    return;
                }
                if (!readBuffer.hasRemaining()) {
                    rxHeader.clear();
                    deliverObject(readBuffer);
                    readBuffer = null;
                    readState = State.HEADER;
                }
                break;
            }
            default: {
                throw new IllegalStateException("Illegal read state "
                                                + readState);
            }
        }

        handler.selectForRead();
    }

    @Override
    public void send(Heartbeat heartbeat) {
        send((TimedMsg) HeartbeatMsg.toHeartbeatMsg(heartbeat));
    }

    @Override
    public void send(TimedMsg tm) {
        /*
        if (!(tm instanceof Heartbeat)) {
            System.out.println("Sending " + tm + " on " + messageConnection);
        }
        */
        byte[] bytesToSend = null;
        try {
            tm.setOrder(sendCount.incrementAndGet());
            bytesToSend = wireSecurity.toWireForm(tm);
        } catch (Exception ex) {
            log.log(Level.SEVERE,
                    format("%s failed to marshall timed message: %s - not sent",
                           me, tm), ex);
            return;
        }

        sendObject(bytesToSend);
    }

    @Override
    public void setIgnoring(boolean ignoring) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("setIgnoring is being called");
        }
        this.ignoring = ignoring;
    }

    public void shutdown() {
        handler.close();
    }

    @Override
    public void silent() {
        if (log.isLoggable(Level.FINER)) {
            log.finer("silent is being called");
        }
        announceTerm = false;
    }

    @Override
    public void terminate() {
        if (log.isLoggable(Level.FINER)) {
            log.finer("terminate is being called");
        }
        announceTerm = false;
        shutdown();
    }

    @Override
    public void writeReady() {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Socket write ready " + messageConnection);
        }
        switch (writeState) {
            case HEADER: {
                if (!write(wxHeader)) {
                    return;
                }
                if (wxHeader.hasRemaining()) {
                    break;
                }
                writeState = State.BODY;
                // fallthrough to body intentional
            }
            case BODY: {
                if (!write(writeBuffer)) {
                    return;
                }
                if (!writeBuffer.hasRemaining()) {
                    writeBuffer = null;
                    completion.release();
                    return;
                }
                break;
            }
            default: {
                throw new IllegalStateException("Illegal write state: "
                                                + writeState);
            }
        }
        handler.selectForWrite();
    }

    private void initialMsg(TimedMsg tm) {

        if (log.isLoggable(Level.FINER)) {
            log.finer("initialMsg is being called");
        }

        Object obj = tm;
        TimedMsg bytes = tm;

        /**
         * must be a heartbeat message
         */
        if (!(obj instanceof HeartbeatMsg)) {
            log.severe(format("%s did not receive a heartbeat message first - shutdown",
                              me));
            shutdown();
            return;
        }

        HeartbeatMsg hbmsg = (HeartbeatMsg) obj;

        /**
         * There must be a valid connection (heartbeat connection)
         */
        if (!connectionSet.getView().contains(hbmsg.getSender())) {
            if (log.isLoggable(Level.INFO)) {
                log.info(format("%s did not have incoming connection for %s in the connection set",
                                me, hbmsg.getSender()));
            }
            shutdown();
            return;
        }

        Connection con = connectionSet.getConnection(hbmsg.getSender());

        /**
         * If it is a message connection then attempt to assign this impl to
         * that connection. If successful then record the message connection so
         * all further messages go directly to it. If not successful then
         * shutdown the this implementation object and abort.
         */
        if (con instanceof MessageConnection) {
            if (((MessageConnection) con).assignImpl(this)) {
                messageConnection = (MessageConnection) con;
                messageConnection.deliver(bytes);
            } else {
                log.severe(format("Failed to assign existing msg connection impl: %s",
                                  con));
                silent();
                shutdown();
            }
            return;
        }

        /**
         * By now we should be left with a heartbeat connection - sanity check
         */
        if (!(con instanceof HeartbeatConnection)) {
            log.severe(format("%s ?!? incoming connection is in connection set, but not heartbeat or message type",
                              this));
            shutdown();
            return;
        }
        HeartbeatConnection hbcon = (HeartbeatConnection) con;

        /**
         * If the connection is a heartbeat connection then the other end must
         * be setting up the connection without this end having requested it.
         * That means the other end must want it, so check the msgLink field for
         * this end is set - this is a sanity check.
         * 
         * *********************************************************************
         * 
         * The case can happen, so the above comment is incorrect. If the user
         * does a connect and then disconnect without sending a message, then
         * the other end could initiate a connection neither end needs in
         * response to the initial connect. Do not count this as an error, but
         * do log its occurance.
         */
        if (!hbmsg.getMsgLinks().contains(me.id)) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(format("%s incoming connection from %s when neither end wants the connection",
                                 me, con.getSender()));
            }
        }

        /**
         * Now we are left with a valid heartbeat connection and the other end
         * is initiating a message connection, so create this end.
         * 
         * Note that the connection set only finds out about the newly created
         * message connection when it is informed by the call to
         * connectionSet.useNewMessageConnection(), so it can not terminate the
         * connection before the call to messageConnection.assignImpl(). Also,
         * we created the message connection, so we know it does not yet have an
         * impl. Hence we can assume it will succeed in assigning the impl.
         */
        messageConnection = new MessageConnection(me, connectionSet,
                                                  hbcon.getProtocol(),
                                                  hbcon.getCandidate());
        if (!messageConnection.assignImpl(this)) {
            log.severe(format("Failed to assign incoming connection on heartbeat: %s",
                              messageConnection));
            silent();
            shutdown();
        }
        messageConnection.deliver(bytes);

        /**
         * if the call to connectionSet.useNewMessageConnection() then a
         * connection has been created since we checked for it above with
         * connectionSet.getConnection(). The other end will not make two
         * connection attempts at the same time, but if this thread is delayed
         * during the last 20 lines of code for long enough for the following to
         * happen: 1. other end time out connection + 2. quiesence period + 3.
         * this end rediscover other end in multicast heartbeats + 4. other end
         * initiates new connection attempt + 5. new connection attempt gets
         * accepted (new thread created for it) + 6. read first heartbeat and
         * get through this code in the new thread. Then it could beat this
         * thread to it. If all this happens (and based on the premise
         * "if it can happen it will happen") then this thread should rightly
         * comit suicide in disgust!!!!
         */
        if (!connectionSet.useNewMessageConnection(messageConnection)) {
            if (log.isLoggable(Level.INFO)) {
                log.info(format("%s Concurrent creation of message connections from %s",
                                me, messageConnection.getSender()));
            }
            shutdown();
            return;
        }
    }

    private boolean isClose(IOException ioe) {
        return "Broken pipe".equals(ioe.getMessage())
               || "Connection reset by peer".equals(ioe.getMessage());
    }

    private boolean read(ByteBuffer buffer) {
        try {
            handler.getChannel().read(buffer);
        } catch (IOException ioe) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Failed to read socket channel", ioe);
            }
            readState = State.ERROR;
            shutdown();
            return false;
        } catch (NotYetConnectedException nycex) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING,
                        "Attempt to read a socket channel before it is connected",
                        nycex);
            }
            readState = State.ERROR;
            return false;
        }
        return true;
    }

    private boolean write(ByteBuffer buffer) {
        try {
            handler.getChannel().write(buffer);
        } catch (AsynchronousCloseException e) {
            if (log.isLoggable(Level.FINER)) {
                log.log(Level.FINER,
                        "shutting down handler due to other side closing", e);
            }
            writeState = State.ERROR;
            shutdown();
            return false;
        } catch (IOException ioe) {
            if (log.isLoggable(Level.WARNING) && !isClose(ioe)) {
                log.log(Level.WARNING, "shutting down handler", ioe);
            }
            writeState = State.ERROR;
            shutdown();
            return false;
        }
        return true;
    }

    protected synchronized void sendObject(byte[] bytes) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("sendObject being called");
        }
        try {
            completion.acquire();
        } catch (InterruptedException e) {
            return;
        }
        wxHeader.clear();
        wxHeader.putInt(0, MAGIC_NUMBER);
        wxHeader.putInt(4, bytes.length);
        wxHeader.clear();
        writeBuffer = ByteBuffer.wrap(bytes);
        writeState = State.HEADER;
        handler.selectForWrite();
    }
}