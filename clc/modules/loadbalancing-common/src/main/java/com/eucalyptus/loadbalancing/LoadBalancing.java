/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.loadbalancing;

import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.annotation.FaultLogPrefix;
import com.eucalyptus.component.annotation.Partition;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.component.annotation.PublicService;
import com.eucalyptus.component.id.Eucalyptus;


/**
 * @author Chris Grzegorczyk <grze@eucalyptus.com>
 */
@Partition( Eucalyptus.class )
@PublicService
@PolicyVendor( PolicySpec.VENDOR_LOADBALANCING )
@FaultLogPrefix( "cloud" )

 public class LoadBalancing extends ComponentId {


    public static LoadBalancing INSTANCE = new LoadBalancing( );
    @Override
    public boolean isPublicService( ) {
        return true;
      }

}

