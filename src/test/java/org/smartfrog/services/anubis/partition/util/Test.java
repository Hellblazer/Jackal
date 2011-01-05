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
package org.smartfrog.services.anubis.partition.util;

import junit.framework.TestCase;

public class Test extends TestCase{
    public Test() {
    }

    public static void main(String[] args) {
        Test t = new Test();
        t.testIt();
    }

    public void testIt() {

        NodeIdSet bs = new NodeIdSet();

        System.out.println("new bit set=" + bs);

        bs.add(5);
        bs.add(10);

        System.out.println("modified bit set=" + bs);

        bs.remove(5);

        System.out.println("last bit set=" + bs);

    }

}
