package org.exoplatform.onlyoffice.mock;

import java.util.List;

import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * The Class ActivityManagerMock.
 */
public class ActivityManagerMock implements ActivityManager {

  @Override
  public void saveActivityNoReturn(Identity streamOwner, ExoSocialActivity activity) {
  }

  @Override
  public void saveActivityNoReturn(ExoSocialActivity activity) {
  }

  @Override
  public void saveActivity(Identity streamOwner, String type, String title) {
  }

  @Override
  public ExoSocialActivity getActivity(String activityId) {
    return null;
  }

  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) {
    return null;
  }

  @Override
  public void updateActivity(ExoSocialActivity activity) {
  }

  @Override
  public void deleteActivity(ExoSocialActivity activity) {
  }

  @Override
  public void deleteActivity(String activityId) {
  }

  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity newComment) {
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity, boolean loadSubComments) {
    return null;
  }

  @Override
  public void deleteComment(String activityId, String commentId) {
  }

  @Override
  public void deleteComment(ExoSocialActivity activity, ExoSocialActivity comment) {
  }

  @Override
  public void saveLike(ExoSocialActivity activity, Identity identity) {
  }

  @Override
  public void deleteLike(ExoSocialActivity activity, Identity identity) {
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity identity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity ownerIdentity, Identity viewerIdentity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfConnectionsWithListAccess(Identity identity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfSpaceWithListAccess(Identity spaceIdentity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfUserSpacesWithListAccess(Identity identity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivityFeedWithListAccess(Identity identity) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity poster) {
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, String... activityTypes) {
    return null;
  }

  @Override
  public void addProcessor(ActivityProcessor activityProcessor) {
  }

  @Override
  public void addProcessorPlugin(BaseActivityProcessorPlugin activityProcessorPlugin) {
  }

  @Override
  public void addActivityEventListener(ActivityListenerPlugin activityListenerPlugin) {
  }

  @Override
  public ExoSocialActivity saveActivity(Identity streamOwner, ExoSocialActivity activity) {
    return null;
  }

  @Override
  public ExoSocialActivity saveActivity(ExoSocialActivity activity) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity, long start, long limit) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity,
                                                            int offset,
                                                            int length) throws ActivityStorageException {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfUserSpaces(Identity ownerIdentity) {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity identity) throws ActivityStorageException {
    return null;
  }

  @Override
  public void removeLike(ExoSocialActivity activity, Identity identity) throws ActivityStorageException {
  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity activity) throws ActivityStorageException {
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, String type, String title) throws ActivityStorageException {
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, ExoSocialActivity activity) throws Exception {
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner,
                                          String type,
                                          String title,
                                          String body) throws ActivityStorageException {
    return null;
  }

  @Override
  public int getActivitiesCount(Identity owner) throws ActivityStorageException {
    return 0;
  }

  @Override
  public void processActivitiy(ExoSocialActivity activity) {
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getAllActivitiesWithListAccess() {
    return null;
  }

  @Override
  public List<ExoSocialActivity> getSubComments(ExoSocialActivity comment) {
    return null;
  }

  @Override
  public int getMaxUploadSize() {
    return 0;
  }

  @Override
  public List<ExoSocialActivity> getActivities(List<String> activityIdList) {
    return null;
  }

  @Override
  public boolean isActivityEditable(ExoSocialActivity activity, org.exoplatform.services.security.Identity viewer) {
    return false;
  }

}
