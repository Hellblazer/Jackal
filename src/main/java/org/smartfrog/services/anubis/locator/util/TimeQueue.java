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
package org.smartfrog.services.anubis.locator.util;

import java.util.Comparator;

public class TimeQueue extends SortedSetMap<Long, TimeQueueElement> {

    /**
     * Construct the super class using the given comparator. The keys of the
     * SortedSetMap will be time stamps recorded as a Long. The comparator
     * orders these keys by value.
     */
    public TimeQueue() {
        super(new Comparator<Long>() {
            @Override
            public int compare(Long obj1, Long obj2) {
                return obj1.compareTo(obj2);
            }
        });
    }

    /**
     * Add an element to the queue. The element is inserted into the queue in
     * the order defined by the time paramter. The element is also stamped with
     * that time.
     * 
     * @param element
     *            the element
     * @param time
     *            the time
     * @return true
     */
    public boolean add(TimeQueueElement element, long time) {
        Long queuedTime = Long.valueOf(time);
        put(queuedTime, element);
        element.setQueuedTime(queuedTime);
        return true;
    }

    /**
     * Removes an element from the queue. If the element is not queued at the
     * time recorded in its time-stamp the operation will fail.
     * 
     * @param element
     * @return true if the element is removed successfully, false if not.
     */
    public boolean remove(TimeQueueElement element) {
        return remove(element.getQueuedTime(), element);
    }

}