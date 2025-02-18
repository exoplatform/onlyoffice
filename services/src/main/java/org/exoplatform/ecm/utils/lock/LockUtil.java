/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.utils.lock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.lock.Lock;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 15, 2008 11:17:13 AM
 */
public class LockUtil {

  public static void keepLock(Lock lock) throws Exception {
    LockService lockService = ExoContainerContext.getService(LockService.class);
    String key = createLockKey(lock.getNode());
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(key,lock.getLockToken());
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId) throws Exception {
    String keyRoot = createLockKey(lock.getNode(), userId);
    LockService lockService = ExoContainerContext.getService(LockService.class);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lock.getLockToken());
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  public static void keepLock(Lock lock, String userId, String lockToken) throws Exception {
    LockService lockService = ExoContainerContext.getService(LockService.class);
    String keyRoot = createLockKey(lock.getNode(), userId);
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) {
      lockedNodesInfo = new HashMap<String,String>();
    }
    lockedNodesInfo.put(keyRoot, lockToken);
    lockService.putToLockHoding(userId, lockedNodesInfo);
  }

  public static void removeLock(Node node) throws Exception {
    String key = createLockKey(node);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    LockService lockService = ExoContainerContext.getService(LockService.class);
    if(userId == null) userId = IdentityConstants.ANONIM;
    Map<String,String> lockedNodesInfo = lockService.getLockInformation(userId);
    if(lockedNodesInfo == null) return;
    lockedNodesInfo.remove(key);
  }

  public static void changeLockToken(Node oldNode, Node newNode) throws Exception {
    ExoContainerContext.getService(LockService.class).changeLockToken(oldNode, newNode);
  }

  public static void changeLockToken(String srcPath, Node newNode) throws Exception {
    ExoContainerContext.getService(LockService.class).changeLockToken(srcPath, newNode);
  }

  public static String getLockTokenOfUser(Node node) throws Exception {
    return ExoContainerContext.getService(LockService.class).getLockTokenOfUser(node);
  }
  
  public static String getLockTokenOfUser(Node node, String userId) throws Exception {
    return ExoContainerContext.getService(LockService.class).getLockTokenOfUser(node, userId);
  }

  public static String getLockToken(Node node) throws Exception {
    return ExoContainerContext.getService(LockService.class).getLockToken(node);
  }
  
  public static String getOldLockKey(String srcPath, Node node) throws Exception {
    return ExoContainerContext.getService(LockService.class).getOldLockKey(srcPath, node);
  }

  public static String createLockKey(Node node) throws Exception {
    return ExoContainerContext.getService(LockService.class).createLockKey(node);
  }

  public static String createLockKey(Node node, String userId) throws Exception {
    return ExoContainerContext.getService(LockService.class).createLockKey(node, userId);
  }

  public static boolean isLocked(Node node) throws Exception {
    if(!node.isLocked()) return false;
    String lockToken = LockUtil.getLockTokenOfUser(node);
    if(lockToken != null) {
      node.getSession().addLockToken(LockUtil.getLockToken(node));
      return false;
    }
    return true;
  }

  /**
   * Remove a membership from lock cache.
   * If membership type is *, remove all memberships of specified group except ignored list
   *
   * @param removedMembership
   * @param ignoredMemberships
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public static void removeLockCache(String removedMembership, List<String> ignoredMemberships) throws Exception {
    OrganizationService organizationService = ExoContainerContext.getService(OrganizationService.class);
    List<MembershipType> availMembershipTypes =
        (List<MembershipType>) organizationService.getMembershipTypeHandler().findMembershipTypes();
    HashMap<String, Map<String, String>> lockHolding = ExoContainerContext.getService(LockService.class).getLockHolding();

    // Remove lock cache for specific membership
    lockHolding.remove(removedMembership);

    // If membership type is *, remove all types except ignored list
    if (removedMembership.startsWith("*")) {
      for (MembershipType membershipType : availMembershipTypes) {
        String membership = removedMembership.replace("*", membershipType.getName());
        if (!ignoredMemberships.contains(membership)) {
          lockHolding.remove(membership);
        }
      }
    }
  }
}
