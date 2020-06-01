package org.exoplatform.onlyoffice.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.lock.LockService;

/**
 * The Class LockServiceMock.
 */
public class LockServiceMock implements LockService{

  /**
   * Gets the pre setting lock list.
   *
   * @return the pre setting lock list
   * @throws Exception the exception
   */
  @Override
  public List<String> getPreSettingLockList() throws Exception {
    return null;
  }

  /**
   * Gets the all groups or users for lock.
   *
   * @return the all groups or users for lock
   * @throws Exception the exception
   */
  @Override
  public List<String> getAllGroupsOrUsersForLock() throws Exception {
    return null;
  }

  /**
   * Adds the groups or users for lock.
   *
   * @param groupsOrUsers the groups or users
   * @throws Exception the exception
   */
  @Override
  public void addGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    
  }

  /**
   * Removes the groups or users for lock.
   *
   * @param groupsOrUsers the groups or users
   * @throws Exception the exception
   */
  @Override
  public void removeGroupsOrUsersForLock(String groupsOrUsers) throws Exception {
    
  }

  /**
   * Gets the lock holding.
   *
   * @return the lock holding
   */
  @Override
  public HashMap<String, Map<String, String>> getLockHolding() {
    return null;
  }

  /**
   * Put to lock hoding.
   *
   * @param userId the user id
   * @param lockedNodesInfo the locked nodes info
   */
  @Override
  public void putToLockHoding(String userId, Map<String, String> lockedNodesInfo) {
    
  }

  /**
   * Gets the lock information.
   *
   * @param userId the user id
   * @return the lock information
   */
  @Override
  public Map<String, String> getLockInformation(String userId) {
    return null;
  }

  /**
   * Removes the locks of user.
   *
   * @param userId the user id
   */
  @Override
  public void removeLocksOfUser(String userId) {
  }

  /**
   * Removes the locks.
   */
  @Override
  public void removeLocks() {
  }

  /**
   * Gets the lock token of user.
   *
   * @param node the node
   * @return the lock token of user
   * @throws Exception the exception
   */
  @Override
  public String getLockTokenOfUser(Node node) throws Exception {
    return null;
  }

  /**
   * Creates the lock key.
   *
   * @param node the node
   * @return the string
   * @throws Exception the exception
   */
  @Override
  public String createLockKey(Node node) throws Exception {
    return null;
  }

  /**
   * Creates the lock key.
   *
   * @param node the node
   * @param userId the user id
   * @return the string
   * @throws Exception the exception
   */
  @Override
  public String createLockKey(Node node, String userId) throws Exception {
    return null;
  }

  /**
   * Gets the lock token.
   *
   * @param node the node
   * @return the lock token
   * @throws Exception the exception
   */
  @Override
  public String getLockToken(Node node) throws Exception {
    return null;
  }

  /**
   * Change lock token.
   *
   * @param srcPath the src path
   * @param newNode the new node
   * @throws Exception the exception
   */
  @Override
  public void changeLockToken(String srcPath, Node newNode) throws Exception {
  }

  /**
   * Change lock token.
   *
   * @param oldNode the old node
   * @param newNode the new node
   * @throws Exception the exception
   */
  @Override
  public void changeLockToken(Node oldNode, Node newNode) throws Exception {
  }

  /**
   * Gets the old lock key.
   *
   * @param srcPath the src path
   * @param node the node
   * @return the old lock key
   * @throws Exception the exception
   */
  @Override
  public String getOldLockKey(String srcPath, Node node) throws Exception {
    return null;
  }

  @Override
  public String getLockTokenOfUser(Node node, String userId) throws Exception {
    return null;
  }

}
