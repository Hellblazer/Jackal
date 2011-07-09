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
package org.smartfrog.services.anubis.partition.comms.nonblocking;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RxQueueWorker extends Thread {
    private static final Logger log     = Logger.getLogger(RxQueue.class.getCanonicalName());
    private RxQueue<RxJob>      rxQueue = null;

    /**
     * worker thread taking rx objects off the queue and delivering that object
     * to anubis code
     */
    public RxQueueWorker(RxQueue<RxJob> rxQueue) {
        this.rxQueue = rxQueue;
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.log(Level.WARNING, "Uncaught exception", e);
            }
        });
    }

    @Override
    public void run() {

        RxJob rxJob = null;

        while (rxQueue.isOpen()) {
            try {
                rxJob = rxQueue.next();

                if (rxJob != null) {
                    // got an object from the queue - deliver the object within to the appropriate mnh
                    MessageNioHandler mnh = rxJob.getHandler();
                    mnh.deliverObject(rxJob.getDeliverable());
                }
            } catch (Throwable e) {
                log.log(Level.WARNING, "Exception delivering", e);
            }
        }
    }

}
