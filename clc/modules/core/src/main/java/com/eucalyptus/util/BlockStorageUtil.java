/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 *
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.util;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.EnumSet;
import java.util.List;

import javax.crypto.Cipher;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.blockstorage.Snapshot;
import com.eucalyptus.blockstorage.State;
import com.eucalyptus.blockstorage.Volume;
import com.eucalyptus.cloud.UserMetadata;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.component.id.Storage;
import com.eucalyptus.entities.Entities;
import com.google.common.base.Objects;

public class BlockStorageUtil {
	private static Logger LOG = Logger.getLogger(BlockStorageUtil.class);

	public static String encryptNodeTargetPassword(String password) throws EucalyptusCloudException {
    try {
      List<ServiceConfiguration> clusterList = ServiceConfigurations.listPartition( ClusterController.class, StorageProperties.NAME );
      if( clusterList.size() < 1 ) {
        String msg = "Failed to find a cluster with the corresponding partition name for this SC: " + StorageProperties.NAME + "\nFound: " + clusterList.toString( ).replaceAll( ", ", ",\n" );
        throw new EucalyptusCloudException(msg);
      } else {
        ServiceConfiguration clusterConfig = clusterList.get( 0 );
        PublicKey ncPublicKey = Partitions.lookup( clusterConfig ).getNodeCertificate( ).getPublicKey();
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, ncPublicKey);
        return new String(Base64.encode(cipher.doFinal(password.getBytes())));
      }
    } catch ( Exception e ) {
			LOG.error( "Unable to encrypt storage target password: " + e.getMessage( ), e );
			throw new EucalyptusCloudException("Unable to encrypt storage target password: " + e.getMessage(), e);
		}
	}

	public static String encryptSCTargetPassword(String password) throws EucalyptusCloudException {
		PublicKey scPublicKey = SystemCredentials.lookup(Storage.class).getKeyPair().getPublic();
		Cipher cipher;
		try {
			cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, scPublicKey);
			return new String(Base64.encode(cipher.doFinal(password.getBytes())));	      
		} catch (Exception e) {
			LOG.error("Unable to encrypted storage target password");
			throw new EucalyptusCloudException(e.getMessage(), e);
		}
	}

	public static String decryptSCTargetPassword(String encryptedPassword) throws EucalyptusCloudException {
		PrivateKey scPrivateKey = SystemCredentials.lookup(Storage.class).getPrivateKey();
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, scPrivateKey);
			return new String(cipher.doFinal(Base64.decode(encryptedPassword)));
		} catch(Exception ex) {
			LOG.error(ex);
			throw new EucalyptusCloudException("Unable to decrypt storage target password", ex);
		}
	}

  /**
   * Get the total size used for block storage.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for block storage (optionally by partition)
   */
  public static long getBlockStorageTotalSize( final String partition ) {
    return
        getBlockStorageTotalVolumeSize( partition ) +
        getBlockStorageTotalSnapshotSize( partition );
  }

  /**
   * Get the total size used for volumes.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for volumes (optionally by partition)
   */
  public static long getBlockStorageTotalVolumeSize( final String partition ) {
    return getBlockStorageTotalSize( partition, "partition", "size", Volume.class );
  }

  /**
   * Get the total size used for snapshots.
   *
   * @param partition Optional partition qualifier
   * @return The total size used for snapshots (optionally by partition)
   */
  public static long getBlockStorageTotalSnapshotSize( final String partition ) {
    return getBlockStorageTotalSize( partition, "volumePartition", "volumeSize", Snapshot.class );
  }

  private static long getBlockStorageTotalSize( final String partition,
                                                final String partitionProperty,
                                                final String sizeProperty,
                                                final Class<? extends UserMetadata<State>> sizedType ) {
    long size = -1;
    final EntityTransaction db = Entities.get( sizedType );
    try {
      size = Objects.firstNonNull( (Number) Entities.createCriteria(sizedType)
          .add(Restrictions.in("state", EnumSet.of(State.EXTANT, State.BUSY)))
          .add(partition == null ?
              Restrictions.isNotNull(partitionProperty) : // Get size for all partitions.
              Restrictions.eq(partitionProperty, partition))
          .setProjection(Projections.sum(sizeProperty))
          .setReadOnly(true)
          .uniqueResult(), 0 ).longValue();
      db.commit();
    } catch (Exception e) {
      LOG.error(e);
      db.rollback();
    }
    return size;
  }
}
