package org.exoplatform.onlyoffice.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.IdentityProviderPlugin;
import org.exoplatform.social.core.identity.SpaceMemberFilterListAccess.Type;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.profile.*;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.webui.exception.MessageException;

public class IdentityManagerMock implements IdentityManager {
  List<Identity> identities = new ArrayList<>();

  public IdentityManagerMock() {
      int i = 0;
      Identity identity = new Identity(String.valueOf(i));
      identity.setDeleted(false);
      identity.setEnable(true);
      identity.setProviderId(OrganizationIdentityProvider.NAME);
      identity.setRemoteId("john" );
      identity.setId(String.valueOf(i));
      Profile profile = new Profile(identity);
      identity.setProfile(profile);
      identities.add(identity);
  }

  @Override
  public Identity getOrCreateIdentity(String providerId, String remoteId, boolean isProfileLoaded) {
    return identities.stream()
                     .filter(identity -> StringUtils.equals(identity.getProviderId(), providerId)
                         && StringUtils.equals(identity.getRemoteId(), remoteId))
                     .findFirst()
                     .orElse(null);
  }

  @Override
  public Identity getIdentity(String identityId, boolean isProfileLoaded) {
    return identities.stream()
                     .filter(identity -> StringUtils.equals(identity.getId(), identityId))
                     .findFirst()
                     .orElse(null);
  }

  @Override
  public void registerIdentityProviders(IdentityProviderPlugin plugin) {
    // NOOP
  }

  @Override
  public void addProfileListener(ProfileListenerPlugin plugin) {
    // NOOP
  }

  @Override
  public Identity getIdentity(String providerId, String remoteId, boolean loadProfile) {
    return getOrCreateIdentity(providerId, remoteId, loadProfile);
  }

  @Override
  public boolean identityExisted(String providerId, String remoteId) {
    return getOrCreateIdentity(providerId, remoteId, true) != null;
  }

  @Override
  public Identity getIdentity(String id) {
    return getIdentity(id, true);
  }

  @Override
  public Identity getOrCreateIdentity(String providerId, String remoteId) {
    return getOrCreateIdentity(providerId, remoteId, true);
  }

  @Override
  public Identity updateIdentity(Identity identity) {
    return identity;
  }

  @Override
  public List<Identity> getLastIdentities(int limit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteIdentity(Identity identity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void hardDeleteIdentity(Identity identity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Identity> getConnectionsWithListAccess(Identity identity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Profile getProfile(Identity identity) {
    return identity.getProfile();
  }

  @Override
  public InputStream getAvatarInputStream(Identity identity) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream getBannerInputStream(Identity identity) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateProfile(Profile specificProfile) {
    // empty
  }

  @Override
  public ListAccess<Identity> getIdentitiesByProfileFilter(String providerId,
                                                           ProfileFilter profileFilter,
                                                           boolean isProfileLoaded) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Identity> getIdentitiesForUnifiedSearch(String providerId, ProfileFilter profileFilter) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ListAccess<Identity> getSpaceIdentityByProfileFilter(Space space,
                                                              ProfileFilter profileFilter,
                                                              Type type,
                                                              boolean isProfileLoaded) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addIdentityProvider(IdentityProvider<?> identityProvider) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeIdentityProvider(IdentityProvider<?> identityProvider) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerProfileListener(ProfileListenerPlugin profileListenerPlugin) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void processEnabledIdentity(String remoteId, boolean isEnable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesByProfileFilter(String providerId, ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesByProfileFilter(String providerId,
                                                     ProfileFilter profileFilter,
                                                     long offset,
                                                     long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesByProfileFilter(ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesByProfileFilter(ProfileFilter profileFilter, long offset, long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesFilterByAlphaBet(String providerId, ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesFilterByAlphaBet(String providerId,
                                                      ProfileFilter profileFilter,
                                                      long offset,
                                                      long limit) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getIdentitiesFilterByAlphaBet(ProfileFilter profileFilter) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getIdentitiesCount(String providerId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveIdentity(Identity identity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void saveProfile(Profile profile) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Identity> getConnections(Identity ownerIdentity) throws Exception {
    throw new UnsupportedOperationException();
  }

  @Override
  public IdentityStorage getIdentityStorage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IdentityStorage getStorage() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerProfileListener(ProfileListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unregisterProfileListener(ProfileListener listener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> sortIdentities(List<String> identityRemoteIds, String sortField) {
    throw new UnsupportedOperationException();
  }

}
