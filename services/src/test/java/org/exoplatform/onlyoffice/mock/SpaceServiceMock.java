package org.exoplatform.onlyoffice.mock;

import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.application.PortletPreferenceRequiredPlugin;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.space.SpaceApplicationConfigPlugin;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.SpaceListAccess;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleListener;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * The Class SpaceServiceMock.
 */
public class SpaceServiceMock implements SpaceService {

  @Override
  public Space getSpaceByDisplayName(String spaceDisplayName) {
    return null;
  }

  @Override
  public Space getSpaceByPrettyName(String spacePrettyName) {
    return null;
  }

  @Override
  public Space getSpaceByGroupId(String groupId) {
    return null;
  }

  @Override
  public Space getSpaceById(String spaceId) {
    return null;
  }

  @Override
  public Space getSpaceByUrl(String spaceUrl) {
    return null;
  }

  @Override
  public ListAccess<Space> getAllSpacesWithListAccess() {
    return null;
  }

  @Override
  public ListAccess<Space> getAllSpacesByFilter(SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getMemberSpaces(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getMemberSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getAccessibleSpacesWithListAccess(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getAccessibleSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getSettingableSpaces(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getSettingabledSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getInvitedSpacesWithListAccess(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getInvitedSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getPublicSpacesWithListAccess(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getPublicSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public ListAccess<Space> getPendingSpacesWithListAccess(String userId) {
    return null;
  }

  @Override
  public ListAccess<Space> getPendingSpacesByFilter(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public Space createSpace(Space space, String creatorUserId) {
    return null;
  }

  @Override
  public Space updateSpace(Space existingSpace) {
    return null;
  }

  @Override
  public Space updateSpaceAvatar(Space existingSpace) {
    return null;
  }

  @Override
  public Space updateSpaceBanner(Space existingSpace) {
    return null;
  }

  @Override
  public void deleteSpace(Space space) {
  }

  @Override
  public void addPendingUser(Space space, String userId) {
  }

  @Override
  public void removePendingUser(Space space, String userId) {
  }

  @Override
  public boolean isPendingUser(Space space, String userId) {
    return false;
  }

  @Override
  public void addInvitedUser(Space space, String userId) {
  }

  @Override
  public void removeInvitedUser(Space space, String userId) {
  }

  @Override
  public boolean isInvitedUser(Space space, String userId) {
    return false;
  }

  @Override
  public void addMember(Space space, String userId) {
  }

  @Override
  public void removeMember(Space space, String userId) {
  }

  @Override
  public boolean isMember(Space space, String userId) {
    return false;
  }

  @Override
  public void setManager(Space space, String userId, boolean isManager) {
  }

  @Override
  public boolean isManager(Space space, String userId) {
    return false;
  }

  @Override
  public boolean isOnlyManager(Space space, String userId) {
    return false;
  }

  @Override
  public boolean hasAccessPermission(Space space, String userId) {
    return false;
  }

  @Override
  public boolean hasSettingPermission(Space space, String userId) {
    return false;
  }

  @Override
  public void registerSpaceListenerPlugin(SpaceListenerPlugin spaceListenerPlugin) {
  }

  @Override
  public void unregisterSpaceListenerPlugin(SpaceListenerPlugin spaceListenerPlugin) {
  }

  @Override
  public void setSpaceApplicationConfigPlugin(SpaceApplicationConfigPlugin spaceApplicationConfigPlugin) {
  }

  @Override
  public SpaceApplicationConfigPlugin getSpaceApplicationConfigPlugin() {
    return null;
  }

  @Override
  public List<Space> getAllSpaces() throws SpaceException {
    return null;
  }

  @Override
  public Space getSpaceByName(String spaceName) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getSpacesByFirstCharacterOfName(String firstCharacterOfName) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getSpacesBySearchCondition(String condition) throws Exception {
    return null;
  }

  @Override
  public List<Space> getSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getAccessibleSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getVisibleSpaces(String userId, SpaceFilter spaceFilter) throws SpaceException {
    return null;
  }

  @Override
  public SpaceListAccess getVisibleSpacesWithListAccess(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public SpaceListAccess getUnifiedSearchSpacesWithListAccess(String userId, SpaceFilter spaceFilter) {
    return null;
  }

  @Override
  public List<Space> getEditableSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getInvitedSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getPublicSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getPendingSpaces(String userId) throws SpaceException {
    return null;
  }

  @Override
  public Space createSpace(Space space, String creator, String invitedGroupId) throws SpaceException {
    return null;
  }

  @Override
  public void saveSpace(Space space, boolean isNew) throws SpaceException {
  }

  @Override
  public void renameSpace(Space space, String newDisplayName) throws SpaceException {
  }

  @Override
  public void renameSpace(String remoteId, Space space, String newDisplayName) throws SpaceException {
  }

  @Override
  public void deleteSpace(String spaceId) throws SpaceException {
  }

  @Override
  public void initApp(Space space) throws SpaceException {
  }

  @Override
  public void initApps(Space space) throws SpaceException {
  }

  @Override
  public void deInitApps(Space space) throws SpaceException {
  }

  @Override
  public void addMember(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void removeMember(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public List<String> getMembers(Space space) throws SpaceException {
    return null;
  }

  @Override
  public List<String> getMembers(String spaceId) throws SpaceException {
    return null;
  }

  @Override
  public void setLeader(Space space, String userId, boolean isLeader) throws SpaceException {
  }

  @Override
  public void setLeader(String spaceId, String userId, boolean isLeader) throws SpaceException {
  }

  @Override
  public boolean isLeader(Space space, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isLeader(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isOnlyLeader(Space space, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isOnlyLeader(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isMember(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean hasAccessPermission(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean hasEditPermission(Space space, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean hasEditPermission(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isInvited(Space space, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isInvited(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isPending(Space space, String userId) throws SpaceException {
    return false;
  }

  @Override
  public boolean isPending(String spaceId, String userId) throws SpaceException {
    return false;
  }

  @Override
  public void installApplication(String spaceId, String appId) throws SpaceException {
  }

  @Override
  public void installApplication(Space space, String appId) throws SpaceException {
  }

  @Override
  public void activateApplication(Space space, String appId) throws SpaceException {
  }

  @Override
  public void activateApplication(String spaceId, String appId) throws SpaceException {
  }

  @Override
  public void deactivateApplication(Space space, String appId) throws SpaceException {
  }

  @Override
  public void deactivateApplication(String spaceId, String appId) throws SpaceException {
  }

  @Override
  public void removeApplication(Space space, String appId, String appName) throws SpaceException {
  }

  @Override
  public void removeApplication(String spaceId, String appId, String appName) throws SpaceException {
  }

  @Override
  public void updateSpaceAccessed(String remoteId, Space space) throws SpaceException {
  }

  @Override
  public List<Space> getLastAccessedSpace(String remoteId, String appId, int offset, int limit) throws SpaceException {
    return null;
  }

  @Override
  public List<Space> getLastSpaces(int limit) {
    return null;
  }

  @Override
  public ListAccess<Space> getLastAccessedSpace(String remoteId, String appId) {
    return null;
  }

  @Override
  public void requestJoin(Space space, String userId) throws SpaceException {
  }

  @Override
  public void requestJoin(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void revokeRequestJoin(Space space, String userId) throws SpaceException {
  }

  @Override
  public void revokeRequestJoin(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void inviteMember(Space space, String userId) throws SpaceException {
  }

  @Override
  public void inviteMember(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void revokeInvitation(Space space, String userId) throws SpaceException {
  }

  @Override
  public void revokeInvitation(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void acceptInvitation(Space space, String userId) throws SpaceException {
  }

  @Override
  public void acceptInvitation(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void denyInvitation(Space space, String userId) throws SpaceException {
  }

  @Override
  public void denyInvitation(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void validateRequest(Space space, String userId) throws SpaceException {
  }

  @Override
  public void validateRequest(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void declineRequest(Space space, String userId) throws SpaceException {
  }

  @Override
  public void declineRequest(String spaceId, String userId) throws SpaceException {
  }

  @Override
  public void registerSpaceLifeCycleListener(SpaceLifeCycleListener listener) {
  }

  @Override
  public void unregisterSpaceLifeCycleListener(SpaceLifeCycleListener listener) {
  }

  @Override
  public void setPortletsPrefsRequired(PortletPreferenceRequiredPlugin portletPrefsRequiredPlugin) {
  }

  @Override
  public String[] getPortletsPrefsRequired() {
    return null;
  }

  @Override
  public ListAccess<Space> getVisitedSpaces(String remoteId, String appId) {
    return null;
  }

  @Override
  public boolean isSuperManager(String userId) {
    return false;
  }

  @Override
  public Space createSpace(Space space, String creator, List<Identity> identitiesToInvite) throws SpaceException {
    return null;
  }

  
}
