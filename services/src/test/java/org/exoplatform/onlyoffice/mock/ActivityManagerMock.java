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

  public void saveActivityNoReturn(Identity streamOwner, ExoSocialActivity activity) {
  }

  public void saveActivityNoReturn(ExoSocialActivity activity) {
  }

  public ExoSocialActivity getActivity(String activityId) {
    return null;
  }

  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) {
    return null;
  }

  public void updateActivity(ExoSocialActivity activity) {
  }

  public void deleteActivity(ExoSocialActivity activity) {
  }

  public void deleteActivity(String activityId) {
  }

  public void saveComment(ExoSocialActivity activity, ExoSocialActivity newComment) {
  }

  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity, boolean loadSubComments) {
    return null;
  }

  public void deleteComment(String activityId, String commentId) {
  }

  public void deleteComment(ExoSocialActivity activity, ExoSocialActivity comment) {
  }

  public void saveLike(ExoSocialActivity activity, Identity identity) {
  }

  public void deleteLike(ExoSocialActivity activity, Identity identity) {
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity identity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity ownerIdentity, Identity viewerIdentity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfConnectionsWithListAccess(Identity identity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfSpaceWithListAccess(Identity spaceIdentity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfUserSpacesWithListAccess(Identity identity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivityFeedWithListAccess(Identity identity) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity poster) {
    return null;
  }

  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity, String... activityTypes) {
    return null;
  }

  public void addProcessor(ActivityProcessor activityProcessor) {
  }

  public void addProcessorPlugin(BaseActivityProcessorPlugin activityProcessorPlugin) {
  }

  public void addActivityEventListener(ActivityListenerPlugin activityListenerPlugin) {
  }

  public RealtimeListAccess<ExoSocialActivity> getAllActivitiesWithListAccess() {
    return null;
  }

  public List<ExoSocialActivity> getSubComments(ExoSocialActivity comment) {
    return null;
  }

  public int getMaxUploadSize() {
    return 0;
  }

  public List<ExoSocialActivity> getActivities(List<String> activityIdList) {
    return null;
  }

  public boolean isActivityEditable(ExoSocialActivity activity, org.exoplatform.services.security.Identity viewer) {
    return false;
  }

}
