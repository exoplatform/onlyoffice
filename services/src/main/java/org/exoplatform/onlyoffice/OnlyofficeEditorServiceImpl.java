/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jcr.*;
import javax.jcr.lock.Lock;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.onlyoffice.Config.Editor;
import org.exoplatform.onlyoffice.jcr.NodeFinder;
import org.exoplatform.portal.Constants;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cache.*;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.lock.LockService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.services.security.*;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.application.SpaceActivityPublisher;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

/**
 * Service implementing {@link OnlyofficeEditorService} and {@link Startable}.
 * This component handles interactions with Onlyoffice Document Server and
 * related eXo user states.<br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: OnlyofficeEditorServiceImpl.java 00000 Jan 31, 2016 pnedonosko
 *          $
 */
public class OnlyofficeEditorServiceImpl implements OnlyofficeEditorService, Startable {

  /** The Constant LOG. */
  protected static final Log     LOG                      = ExoLogger.getLogger(OnlyofficeEditorServiceImpl.class);

  /** The Constant RANDOM. */
  protected static final Random  RANDOM                   = new Random();

  /** The Constant CONFIG_DS_HOST. */
  public static final String     CONFIG_DS_HOST           = "documentserver-host";

  /** The Constant CONFIG_DS_SCHEMA. */
  public static final String     CONFIG_DS_SCHEMA         = "documentserver-schema";

  /** The Constant CONFIG_DS_ACCESS_ONLY. */
  public static final String     CONFIG_DS_ACCESS_ONLY    = "documentserver-access-only";

  /** The Constant CONFIG_DS_SECRET. */
  public static final String     CONFIG_DS_SECRET         = "documentserver-secret";

  /**
   * Configuration key for Document Server's allowed hosts in requests from a DS
   * to eXo side.
   */
  public static final String     CONFIG_DS_ALLOWEDHOSTS   = "documentserver-allowedhosts";

  /** The Constant HTTP_PORT_DELIMITER. */
  protected static final char    HTTP_PORT_DELIMITER      = ':';

  /** The hidden folder */
  protected static final String  HIDDEN_FOLDER            = "...";

  /** The date format for Last Edited in editor bar */
  protected static final String  LAST_EDITED_DATE_FORMAT  = "dd.MM.yyyy HH:mm";

  /** The Constant TYPE_TEXT. */
  protected static final String  TYPE_TEXT                = "text";

  /** The Constant TYPE_SPREADSHEET. */
  protected static final String  TYPE_SPREADSHEET         = "spreadsheet";

  /** The Constant TYPE_PRESENTATION. */
  protected static final String  TYPE_PRESENTATION        = "presentation";

  /** The Constant DEFAULT_NAME. **/
  protected static final String  DEFAULT_NAME             = "untitled";

  /** The Constant RELATION_PROP. **/
  protected static final String  RELATION_PROP            = "exo:relation";

  /** The Consant FILE_EXPLORER_URL_SYNTAX. **/
  protected static final Pattern FILE_EXPLORER_URL_SYNTAX = Pattern.compile("([^:/]+):(/.*)");

  /** The Constant LOCK_WAIT_ATTEMTS. */
  protected static final int     LOCK_WAIT_ATTEMTS        = 20;

  /** The Constant LOCK_WAIT_TIMEOUT. */
  protected static final long    LOCK_WAIT_TIMEOUT        = 250;

  /** The Constant EMPTY_TEXT. */
  protected static final String  EMPTY_TEXT               = "".intern();

  /** The Constant CACHE_NAME. */
  public static final String     CACHE_NAME               = "onlyoffice.EditorCache".intern();

  /**
   * NewDocumentTypesConfig.
   */
  public static class DocumentTypesConfig {

    /** The mime types. */
    protected List<String> mimeTypes;

    /**
     * Gets the mime types.
     *
     * @return the mime types
     */
    public List<String> getMimeTypes() {
      return mimeTypes;
    }

    /**
     * Sets the mime types.
     *
     * @param mimeTypes the new mime types
     */
    public void setMimeTypes(List<String> mimeTypes) {
      this.mimeTypes = mimeTypes;
    }
  }

  /**
   * The Class LockState.
   */
  class LockState {

    /** The lock token. */
    final String lockToken;

    /** The lock. */
    final Lock   lock;

    /**
     * Instantiates a new lock state.
     *
     * @param lockToken the lock token
     */
    LockState(String lockToken) {
      super();
      this.lockToken = lockToken;
      this.lock = null;
    }

    /**
     * Instantiates a new lock state.
     *
     * @param lock the lock
     */
    LockState(Lock lock) {
      super();
      this.lockToken = null;
      this.lock = lock;
    }

    /**
     * Instantiates a new lock state.
     */
    LockState() {
      super();
      this.lockToken = null;
      this.lock = null;
    }

    /**
     * Check if was locked by this editor service.
     *
     * @return true, if successful
     */
    boolean wasLocked() {
      return lock != null;
    }

    /**
     * Check can edit a document associated with this lock.
     *
     * @return true, if successful
     */
    boolean canEdit() {
      return lock != null || lockToken != null;
    }
  }

  /** The jcr service. */
  protected final RepositoryService                               jcrService;

  /** The session providers. */
  protected final SessionProviderService                          sessionProviders;

  /** The identity registry. */
  protected final IdentityRegistry                                identityRegistry;

  /** The finder. */
  protected final NodeFinder                                      finder;

  /** The organization. */
  protected final OrganizationService                             organization;

  /** The authenticator. */
  protected final Authenticator                                   authenticator;

  /** The document service. */
  protected final DocumentService                                 documentService;

  /** The lock service. */
  protected final LockService                                     lockService;

  /** The listener service. */
  protected final ListenerService                                 listenerService;

  /** The trash service. */
  protected final TrashService                                    trashService;

  /** The space service. */
  protected final SpaceService                                    spaceService;

  /** The activity manager. */
  protected final ActivityManager                                 activityManager;

  /** The node hierarchy creator. */
  protected final NodeHierarchyCreator                            hierarchyCreator;

  /** The manage drive service */
  protected final ManageDriveService                              manageDriveService;

  /** Cache of Editing documents. */
  protected final ExoCache<String, ConcurrentMap<String, Config>> activeCache;

  /** Lock for updating Editing documents cache. */
  protected final ReentrantLock                                   activeLock = new ReentrantLock();

  /** The config. */
  protected final Map<String, String>                             config;

  /** The upload url. */
  protected final String                                          uploadUrl;

  /** The documentserver host name. */
  protected final String                                          documentserverHostName;

  /** The documentserver url. */
  protected final String                                          documentserverUrl;

  /** The document command service url. */
  protected final String                                          commandServiceUrl;

  /** The document server secret. */
  protected final String                                          documentserverSecret;

  /** The documentserver access only. */
  protected final boolean                                         documentserverAccessOnly;

  /** The group drives path in JCR. */
  protected final String                                          groupsPath;

  /** The user drives paths in JCR. */
  protected final String                                          usersPath;

  /** The documentserver allowed hosts (can be empty if not configured). */
  protected final Set<String>                                     documentserverAllowedhosts;

  /** The file types. */
  protected final Map<String, String>                             fileTypes  = new ConcurrentHashMap<String, String>();

  /** The listeners. */
  protected final ConcurrentLinkedQueue<OnlyofficeEditorListener> listeners  =
                                                                            new ConcurrentLinkedQueue<OnlyofficeEditorListener>();

  /** The document type plugin. */
  protected DocumentTypePlugin                                    documentTypePlugin;

  public static final String COMMONS_RESOUCE_BUNDLE_NAME = "locale.navigation.portal.intranet";

  /**
   * Cloud Drive service with storage in JCR and with managed features.
   *
   * @param jcrService {@link RepositoryService}
   * @param sessionProviders {@link SessionProviderService}
   * @param identityRegistry the identity registry
   * @param finder the finder
   * @param organization the organization
   * @param authenticator the authenticator
   * @param cacheService the cache service
   * @param documentService the document service (ECMS)
   * @param lockService the lock service
   * @param listenerService the listener service
   * @param trashService the trashService
   * @param spaceService the spaceService
   * @param activityManager the activityManager
   * @param manageDriveService the manageDriveService
   * @param hierarchyCreator the hierarchyCreator
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public OnlyofficeEditorServiceImpl(RepositoryService jcrService,
                                     SessionProviderService sessionProviders,
                                     IdentityRegistry identityRegistry,
                                     NodeFinder finder,
                                     OrganizationService organization,
                                     Authenticator authenticator,
                                     CacheService cacheService,
                                     DocumentService documentService,
                                     LockService lockService,
                                     ListenerService listenerService,
                                     TrashService trashService,
                                     SpaceService spaceService,
                                     ActivityManager activityManager,
                                     ManageDriveService manageDriveService,
                                     NodeHierarchyCreator hierarchyCreator,
                                     InitParams params)
      throws ConfigurationException {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.finder = finder;
    this.organization = organization;
    this.authenticator = authenticator;
    this.documentService = documentService;
    this.lockService = lockService;
    this.listenerService = listenerService;
    this.trashService = trashService;
    this.spaceService = spaceService;
    this.activityManager = activityManager;
    this.activeCache = cacheService.getCacheInstance(CACHE_NAME);
    this.hierarchyCreator = hierarchyCreator;
    this.manageDriveService = manageDriveService;
    if (LOG.isDebugEnabled()) {
      addDebugCacheListener();
    }
    initFileTypes();
    // configuration
    PropertiesParam param = params.getPropertiesParam("editor-configuration");

    if (param != null) {
      config = Collections.unmodifiableMap(param.getProperties());
    } else {
      throw new ConfigurationException("Property parameters editor-configuration required.");
    }

    String dsSchema = config.get(CONFIG_DS_SCHEMA);
    String dsHost = config.get(CONFIG_DS_HOST);
    this.documentserverHostName = getDocumentserverHost(dsSchema, dsHost);
    // base parameters for API
    StringBuilder documentserverUrl = new StringBuilder();
    documentserverUrl.append(dsSchema);
    documentserverUrl.append("://");
    documentserverUrl.append(dsHost);

    this.uploadUrl = new StringBuilder(documentserverUrl).append("/FileUploader.ashx").toString();
    this.documentserverUrl = new StringBuilder(documentserverUrl).append("/OfficeWeb/").toString();
    this.commandServiceUrl = new StringBuilder(documentserverUrl).append("/coauthoring/CommandService.ashx").toString();
    this.documentserverAccessOnly = Boolean.parseBoolean(config.get(CONFIG_DS_ACCESS_ONLY));
    this.documentserverSecret = config.get(CONFIG_DS_SECRET);
    this.documentserverAllowedhosts = getDocumentserverAllowedHosts(config.get(CONFIG_DS_ALLOWEDHOSTS));

    this.usersPath = hierarchyCreator.getJcrPath(BasePath.CMS_USERS_PATH);
    this.groupsPath = hierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(OnlyofficeEditorListener listener) {
    this.listeners.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(OnlyofficeEditorListener listener) {
    this.listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Config getEditor(String userId, String workspace, String path) throws OnlyofficeEditorException, RepositoryException {
    Node node = getDocument(workspace, path);
    if (node != null && node.isNodeType("mix:referenceable")) {
      return getEditor(userId, node.getUUID(), false);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Config getEditorByKey(String userId, String key) throws OnlyofficeEditorException, RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);
        return config;
      }
    }
    return null;
  }

  /**
   * Gets the editor.
   *
   * @param userId the user id
   * @param docId the node docId
   * @param createCoEditing if <code>true</code> and has no editor for given
   *          user, create a copy for co-editing if document already editing by
   *          other users
   * @return the editor
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected Config getEditor(String userId, String docId, boolean createCoEditing) throws OnlyofficeEditorException,
                                                                                   RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(docId);
    if (configs != null) {
      Config config = configs.get(userId);
      DocumentStatus.Builder statusBuilder = new DocumentStatus.Builder();
      statusBuilder.users(new String[] { userId });
      if (config == null && createCoEditing) {
        // copy editor for this user from another entry in the configs map
        try {
          Config another = configs.values().iterator().next();
          User user = getUser(userId); // and use this user language
          if (user != null) {
            config = another.forUser(user.getUserName(),
                                     user.getDisplayName(),
                                     getUserLang(userId),
                                     documentserverSecret);
            Config existing = configs.putIfAbsent(userId, config);
            if (existing == null) {
              // need update the configs in the cache (for replicated cache)
              activeCache.put(config.getDocument().getKey(), configs);
              activeCache.put(config.getDocId(), configs);
            } else {
              config = existing;
            }
            statusBuilder.config(config);
            statusBuilder.url(config.getEditorUrl());
            statusBuilder.key(config.getDocument().getKey());
            fireGet(statusBuilder.build());
          } else {
            LOG.warn("Attempt to obtain document editor (" + nodePath(another) + ") under not existing user " + userId);
            throw new BadParameterException("User not found for " + another.getDocument().getTitle());
          }
        } catch (NoSuchElementException e) { // if configs was cleaned by
                                             // closing all active editors
          config = null;
        }
      } else if (createCoEditing) {
        // otherwise: config already obtained
        statusBuilder.config(config);
        statusBuilder.url(config.getEditorUrl());
        statusBuilder.key(config.getDocument().getKey());
        fireGet(statusBuilder.build());
      }

      return config; // can be null
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Config createEditor(String schema,
                             String host,
                             int port,
                             String userId,
                             String workspace,
                             String docId) throws OnlyofficeEditorException, RepositoryException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    // XXX to let REST EditorService work with paths
    if (docId.startsWith("/")) {
      // it's path as docId
      docId = initDocument(workspace, docId);
    }
    Node node = getDocumentById(workspace, docId);
    if (node == null) {
      throw new DocumentNotFoundException("The document is not found. docId: " + docId + ", workspace: " + workspace);
    }
    String path = node.getPath();

    // only nt:file are supported for online edition
    if (!node.isNodeType("nt:file")) {
      throw new OnlyofficeEditorException("Document should be a nt:file node: " + nodePath(workspace, path));
    }
    if (!canEditDocument(node)) {
      throw new OnlyofficeEditorException("Cannot edit document: " + nodePath(workspace, path));
    }

    Config config = getEditor(userId, docId, true);
    if (config == null) {
      // we should care about concurrent calls here
      activeLock.lock();
      try {
        ConcurrentMap<String, Config> configs = activeCache.get(docId);
        if (configs != null) {
          config = getEditor(userId, docId, true);
          if (config == null) {
            // it's unexpected state as existing map SHOULD contain a config and
            // it must be copied for given user in getEditor(): client will need
            // to retry the operation
            throw new ConflictException("Cannot obtain configuration for already existing editor");
          }
          // FYI mapping by unique file key should be done by the thread that
          // created this existing map
        } else {
          // Build a new editor config and document key
          User user = getUser(userId);

          String fileType = fileType(node);
          String docType = documentType(fileType);

          Config.Builder builder = Config.editor(documentserverUrl, docType, workspace, path, docId);
          builder.owner(userId);
          builder.fileType(fileType);
          builder.uploaded(nodeCreated(node));
          builder.displayPath(getDisplayPath(node, userId));
          builder.comment(nodeComment(node));
          builder.renameAllowed(canRenameDocument(node));
          builder.isActivity(ActivityTypeUtils.getActivityId(node) != null);
          try {
            builder.folder(node.getParent().getName());
          } catch (AccessDeniedException e) {
            // TODO Current user has no permissions to read the document parent
            // - it can be an usecase of shared file.
            // As folder is a text used for "Location" in document info in
            // Onlyoffice, we could guess something like "John Anthony's document"
            // or "Product Team document" for sharing from personal docs and a
            // space respectively.
            String owner;
            try {
              owner = node.getProperty("exo:owner").getString();
            } catch (PathNotFoundException oe) {
              owner = "?";
            }
            LOG.warn("Cannot read document parent node: "
                + nodePath(workspace, node.getPath() + ". Owner: " + owner + ". Error: " + e.getMessage()));
            builder.folder(EMPTY_TEXT); // can be empty for Onlyoffice, will
                                        // mean a root folder
          }
          builder.lang(getUserLang(userId));
          builder.mode("edit");
          builder.title(nodeTitle(node));
          builder.userId(user.getUserName());
          builder.userName(user.getDisplayName());
          builder.lastModifier(getLastModifier(node));
          builder.lastModified(getLastModified(node));
          String key = generateId(workspace, path).toString();

          builder.key(key);

          StringBuilder platformUrl = platformUrl(schema, host, port);
          // REST URL for file and callback URLs fill be generated respectively
          // the platform URL and actual user
          builder.generateUrls(new StringBuilder(platformUrl).append('/')
                                                             .append(PortalContainer.getCurrentRestContextName())
                                                             .toString());
          // editor page URL
          builder.editorUrl(new StringBuilder(platformUrl).append(editorURLPath(docId)).toString());

          // ECMS explorer page URL
          String ecmsPageLink = explorerLink(path);
          builder.explorerUri(explorerUri(schema, host, port, ecmsPageLink));
          builder.secret(documentserverSecret);

          config = builder.build();
          // Create users' config map and add first user
          configs = new ConcurrentHashMap<String, Config>();
          configs.put(userId, config);

          // mapping by unique file key for updateDocument()
          activeCache.put(key, configs);
          activeCache.put(docId, configs);
        }
      } finally {
        activeLock.unlock();
      }
      DocumentStatus status = new DocumentStatus.Builder().config(config)
                                                          .users(new String[] { userId })
                                                          .url(config.getEditorUrl())
                                                          .key(config.getDocument().getKey())
                                                          .build();

      fireCreated(status);
    } else {
      // Update fields
      config.getEditorPage().setDisplayPath(getDisplayPath(node, userId));
      config.getEditorPage().setRenameAllowed(canRenameDocument(node));
      config.getEditorPage().setComment(nodeComment(node));
      config.getEditorPage().setLastModifier(getLastModifier(node));
      config.getEditorPage().setLastModified(getLastModified(node));
    }
    return config;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DocumentContent getContent(String userId, String key) throws OnlyofficeEditorException, RepositoryException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);

        // Use user session here:
        // remember real context state and session provider to restore them at
        // the end
        ConversationState contextState = ConversationState.getCurrent();
        SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
        try {
          // We all the job under actual (requester) user here
          if (!setUserConvoState(userId)) {
            logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Cannot set conversation state");
            throw new OnlyofficeEditorException("Cannot set conversation state " + userId);
          }
          // work in user session
          Node node = node(config.getWorkspace(), config.getPath());
          Node content = nodeContent(node);

          final String mimeType = content.getProperty("jcr:mimeType").getString();
          // data stream will be closed when EoF will be reached
          final InputStream data = new AutoCloseInputStream(content.getProperty("jcr:data").getStream());
          return new DocumentContent() {
            @Override
            public String getType() {
              return mimeType;
            }

            @Override
            public InputStream getData() {
              return data;
            }
          };
        } finally {
          restoreConvoState(contextState, contextProvider);
        }
      } else {
        throw new BadParameterException("User editor not found or already closed " + userId);
      }
    } else {
      throw new BadParameterException("File key not found " + key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canDownloadBy(String hostName) {
    if (documentserverAccessOnly) {
      // #19 support advanced configuration of DS's allowed hosts
      return documentserverHostName.equalsIgnoreCase(hostName) || documentserverAllowedhosts.contains(lowerCase(hostName));
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ChangeState getState(String userId, String key) throws OnlyofficeEditorException {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      if (config != null) {
        validateUser(userId, config);
        String[] users = getActiveUsers(configs);
        return new ChangeState(false, config.getError(), users);
      } else {
        throw new BadParameterException("User editor not found " + userId);
      }
    } else {
      // not found - thus already saved
      return new ChangeState(true, null, new String[0]);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateDocument(DocumentStatus status) throws OnlyofficeEditorException, RepositoryException {
    String key = status.getKey();
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(status.getUserId());
      if (config != null) {
        status.setConfig(config);
        validateUser(status.getUserId(), config);

        String nodePath = nodePath(config.getWorkspace(), config.getPath());

        // status of the document. Can have the following values:
        // 0 - no document with the key identifier could be found,
        // 1 - document is being edited (user opened an editor),
        // 2 - document is ready for saving (last user closed it),
        // 3 - document saving error has occurred,
        // 4 - document is closed with no changes.
        // 6 - document is being edited, but the current document state is
        // saved,
        // 7 - error has occurred while force saving the document.
        long statusCode = status.getStatus();

        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Onlyoffice status " + statusCode + " for " + key + ". URL: " + status.getUrl() + ". Users: "
              + Arrays.toString(status.getUsers()) + " << Local file: " + nodePath);
        }

        if (statusCode == 0) {
          // Onlyoffice doesn't know about such document: we clean our records
          // and raise an error
          activeCache.remove(key);
          activeCache.remove(config.getDocId());
          LOG.warn("Received Onlyoffice status: no document with the key identifier could be found. Key: " + key + ". Document: "
              + nodePath);
          throw new OnlyofficeEditorException("Error editing document: document ID not found");
        } else if (statusCode == 1) {
          // while "document is being edited" (1) will come just before
          // "document is ready for saving"
          // (2) we could do nothing at this point, indeed need study how
          // Onlyoffice behave in different
          // situations when user leave page open or browser
          // hangs/crashes/killed - it still could be useful
          // here to make a cleanup
          // Sync users from the status to active config: this should close
          // configs of gone users
          if (syncUsers(configs, status.getUsers())) {
            // Update cached (for replicated cache)
            activeCache.put(key, configs);
            activeCache.put(config.getDocId(), configs);
          }
        } else if (statusCode == 2) {
          Editor.User lastUser = getUser(key, status.getLastUser());
          Editor.User lastModifier = getLastModifier(key);
          // We download if there were modifications after the last saving.
          if (lastModifier.getId().equals(lastUser.getId()) && lastUser.getLastModified() > lastUser.getLastSaved()) {
            downloadClosed(status);
          } else {
            config.closed();
            broadcastEvent(status, OnlyofficeEditorService.EDITOR_CLOSED_EVENT);
          }
          activeCache.remove(key);
          activeCache.remove(config.getDocId());
        } else if (statusCode == 3) {
          // it's an error of saving in Onlyoffice
          // we sync to remote editors list first
          syncUsers(configs, status.getUsers());
          if (configs.size() <= 1) {
            // if one or zero users we can save it
            String url = status.getUrl();
            if (url != null && url.length() > 0) {
              // if URL available then we can download it assuming it's last
              // successful modification the same behaviour as for status (2)
              downloadClosed(status);
              activeCache.remove(key);
              activeCache.remove(config.getDocId());
              config.setError("Error in editor (" + status.getError() + "). Last change was successfully saved");
              fireError(status);
              broadcastEvent(status, OnlyofficeEditorService.EDITOR_ERROR_EVENT);
              LOG.warn("Received Onlyoffice error of saving document. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Error: " + status.getError()
                  + ". Last change was successfully saved for " + nodePath);
            } else {
              // if error without content URL and last user: it's error state
              LOG.warn("Received Onlyoffice error of saving document without changes URL. Key: " + key + ". Users: "
                  + Arrays.toString(status.getUsers()) + ". Document: " + nodePath + ". Error: " + status.getError());
              config.setError("Error in editor (" + status.getError() + "). No changes saved");
              // Update cached (for replicated cache)
              activeCache.put(key, configs);
              activeCache.put(config.getDocId(), configs);
              fireError(status);
              broadcastEvent(status, OnlyofficeEditorService.EDITOR_ERROR_EVENT);
              // No sense to throw an ex here: it will be caught by the
              // caller (REST) and returned to the Onlyoffice server as 500
              // response, but it doesn't deal with it and will try send the
              // status again.
            }
          } else {
            // otherwise we assume other user will save it later
            LOG.warn("Received Onlyoffice error of saving document with several editors. Key: " + key + ". Users: "
                + Arrays.toString(status.getUsers()) + ". Document: " + nodePath);
            config.setError("Error in editor. Document still in editing state");
            // Update cached (for replicated cache)
            activeCache.put(key, configs);
            activeCache.put(config.getDocId(), configs);
            fireError(status);
            broadcastEvent(status, OnlyofficeEditorService.EDITOR_ERROR_EVENT);
          }
        } else if (statusCode == 4) {
          // user(s) haven't changed the document but closed it: sync users to
          // fire onLeaved event(s)
          syncUsers(configs, status.getUsers());
          // and remove this document from active configs
          activeCache.remove(key);
          activeCache.remove(config.getDocId());
        } else if (statusCode == 6) {
          // forcedsave done, save the version with its URL
          if (LOG.isDebugEnabled()) {
            LOG.debug("Received Onlyoffice forced saved document. Key: " + key + ". Users: " + Arrays.toString(status.getUsers())
                + ". Document " + nodePath + ". URL: " + status.getUrl() + ". Download: " + status.isSaved());
          }
          // Here we decide if we need to download content or just save the link
          if (status.isSaved()) {
            status.setConfig(getEditorByKey(status.getUserId(), key));
            downloadVersion(status);
          } else {
            saveLink(status.getUserId(), key, status.getUrl());
          }

        } else if (statusCode == 7) {
          // For forcedsave error, we may decide next step according
          // status.getError():
          // TODO more precise error handling:
          // 0 No errors.
          // 1 Document key is missing or no document with such key could be
          // found.
          // 2 Callback url not correct.
          // 3 Internal server error.
          // 4 No changes were applied to the document before the forcesave
          // command was received.
          // 5 Command not correct.
          // 6 Invalid token.
          LOG.error("Received Onlyoffice error of forced saving of document. Key: " + key + ". Users: "
              + Arrays.toString(status.getUsers()) + ". Document: " + nodePath + ". Error: " + status.getError() + ". URL: "
              + status.getUrl() + ". Download: " + status.isSaved());
        } else {
          // warn unexpected status, wait for next status
          LOG.warn("Received Onlyoffice unexpected status. Key: " + key + ". URL: " + status.getUrl() + ". Users: "
              + status.getUsers() + ". Document: " + nodePath);
        }
      } else {
        throw new BadParameterException("User editor not found " + status.getUserId());
      }
    } else {
      throw new BadParameterException("File key not found " + key);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String initDocument(Node node) throws OnlyofficeEditorException, RepositoryException {
    if (node.getPrimaryNodeType().getName().equals("exo:symlink")) {
      node = (Node) finder.findItem(node.getSession(), node.getPath());
    }
    if (node.canAddMixin("mix:referenceable") && canEditDocument(node)) {
      node.addMixin("mix:referenceable");
      node.save();
    }
    return node.getUUID();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String initDocument(String workspace, String path) throws OnlyofficeEditorException, RepositoryException {
    Node node = node(workspace, path);
    return initDocument(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDocumentId(Node node) throws OnlyofficeEditorException, RepositoryException {
    if (node.isNodeType("mix:referenceable")) {
      return node.getUUID();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditorLink(Node node) throws RepositoryException, OnlyofficeEditorException {
    if (canEditDocument(node)) {
      String workspace = node.getSession().getWorkspace().getName();
      String docId = initDocument(node);
      PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
      String link = getEditorLink(pcontext.getRequest().getScheme(),
                                  pcontext.getRequest().getServerName(),
                                  pcontext.getRequest().getServerPort(),
                                  workspace,
                                  docId);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Editor link {}: {}", node.getPath(), link);
      }
      return link;
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("Editor link NOT FOUND for {}", node.getPath());
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getEditorLink(String schema, String host, int port, String workspace, String docId) {
    return platformUrl(schema, host, port).append(editorURLPath(docId)).toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getDocumentById(String workspace, String uuid) throws RepositoryException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    try {
      return nodeByUUID(workspace, uuid);
    } catch (ItemNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("The node is not found. Workspace: {}, UUID: {}", workspace, uuid);
      }
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Version> getVersions(String workspace, String docId) throws Exception {
    List<Version> versions = new ArrayList<>() ;
    Node currentNode = getDocumentById(workspace, docId);
    if(currentNode != null) {
      VersionNode rootVersion = new VersionNode(currentNode, currentNode.getSession());

      List<VersionNode> versionNodes = getNodeVersions(rootVersion.getChildren(), new ArrayList<>());
      int versionNodesLength = versionNodes.size() > 3 ? 3 : versionNodes.size();

      for (int i = 0 ; i < versionNodesLength; i++) {
        VersionNode versionNode = versionNodes.get(i);
        Version version = new Version();
        version.setAuthor(versionNode.getAuthor());
        version.setName(versionNode.getName());
        version.setDisplayName(versionNode.getDisplayName());
        version.setFullName(getUser(versionNode.getAuthor()).getDisplayName());
        version.setVersionLabels(versionNode.getVersionLabels());
        version.setCreatedTime(versionNode.getCreatedTime().getTimeInMillis());
        versions.add(version);
      }
    }

    return versions;
  }

  private List<VersionNode> getNodeVersions(List<VersionNode> children, List<VersionNode> versionNodes) throws Exception {
    for(int i = 0; i < children.size(); i ++){
      versionNodes.add(children.get(i));
      List<VersionNode> child = children.get(i).getChildren() ;
      if(!child.isEmpty()) {
        getNodeVersions(child, versionNodes) ;
      }
    }
    versionNodes.sort((v1, v2) -> {
      try {
        if (Integer.parseInt(v1.getName()) < Integer.parseInt(v2.getName())) {
          return 1;
        } else {
          return 0;
        }
      } catch (Exception e) {
        return 0;
      }
    });
    return versionNodes;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getDocument(String workspace, String path) throws RepositoryException, BadParameterException {
    if (workspace == null) {
      workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
    }
    try {
      return node(workspace, path);
    } catch (ItemNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("The node is not found. Workspace: {}, path: {}", workspace, path);
      }
      return null;
    }
  }

  /**
   * On-start initializer.
   */
  @Override
  public void start() {
    InputStream is = null;
    try {
      String workspace = jcrService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName();
      Session session = jcrService.getCurrentRepository().getSystemSession(workspace);
      ExtendedNodeTypeManager nodeTypeManager = (ExtendedNodeTypeManager) session.getWorkspace().getNodeTypeManager();
      is = OnlyofficeEditorService.class.getResourceAsStream("/conf/portal/jcr/onlyoffice-nodetypes.xml");
      nodeTypeManager.registerNodeTypes(is, ExtendedNodeTypeManager.REPLACE_IF_EXISTS, NodeTypeDataManager.TEXT_XML);
    } catch (Exception e) {
      LOG.error("Cannot update nodetypes.", e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          LOG.error("Cannot close InputStream", e);
        }
      }
    }
    LOG.info("Onlyoffice Editor service successfuly started");
  }

  /**
   * On-stop finalizer.
   */
  @Override
  public void stop() {
    LOG.info("Onlyoffice  Editor service successfuly stopped");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addTypePlugin(ComponentPlugin plugin) {
    Class<DocumentTypePlugin> pclass = DocumentTypePlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      DocumentTypePlugin newPlugin = pclass.cast(plugin);
      if (this.documentTypePlugin != null) {
        LOG.info("Replace existing DocumentTypePlugin {} with new one {}",
                 this.documentTypePlugin.getMimeTypes().stream().collect(Collectors.joining(",")),
                 newPlugin.getMimeTypes().stream().collect(Collectors.joining(",")));
      } else {
        LOG.info("Use DocumentTypePlugin {}", newPlugin.getMimeTypes().stream().collect(Collectors.joining(",")));
      }
      this.documentTypePlugin = newPlugin;
      if (LOG.isDebugEnabled()) {
        LOG.debug("Set documentTypePlugin instance of {}", plugin.getClass().getName());
      }
    } else {
      LOG.error("The documentTypePlugin plugin is not an instance of " + pclass.getName());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean canEditDocument(Node node) throws RepositoryException {
    boolean res = false;
    if (node != null) {
      if (isDocumentMimeSupported(node)) {
        String remoteUser = WCMCoreUtils.getRemoteUser();
        String superUser = WCMCoreUtils.getSuperUser();
        boolean locked = node.isLocked();
        if (locked && (remoteUser.equalsIgnoreCase(superUser) || node.getLock().getLockOwner().equals(remoteUser))) {
          locked = false;
        }
        res = !locked && PermissionUtil.canSetProperty(node);
      }
    }
    if (!res && LOG.isDebugEnabled()) {
      LOG.debug("Cannot edit: {}", node != null ? node.getPath() : null);
    }
    return res;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDocumentMimeSupported(Node node) throws RepositoryException {
    if (this.documentTypePlugin != null) {
      if (node != null) {
        String mimeType;
        if (node.isNodeType(Utils.NT_FILE)) {
          mimeType = node.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
        } else {
          mimeType = new MimeTypeResolver().getMimeType(node.getName());
        }
        return this.documentTypePlugin.getMimeTypes().contains(mimeType);
      } else {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void downloadVersion(String userId,
                              String key,
                              boolean coEdited,
                              boolean forcesaved,
                              String comment,
                              String contentUrl) {
    String docId = null;
    Config config = null;
    try {
      config = getEditorByKey(userId, key);
      docId = config.getDocId();
    } catch (RepositoryException | OnlyofficeEditorException e) {
      LOG.error("Cannot obtain config. docId: " + docId, e);
    }
    DocumentStatus status = new DocumentStatus.Builder().config(config)
                                                        .key(key)
                                                        .url(contentUrl)
                                                        .comment(comment)
                                                        .userId(userId)
                                                        .coEdited(coEdited)
                                                        .forcesaved(forcesaved)
                                                        .build();
    downloadVersion(status);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Editor.User getLastModifier(String key) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    Editor.User lastUser = null;
    if (configs != null) {
      long maxLastModified = 0;
      for (Entry<String, Config> entry : configs.entrySet()) {
        Editor.User user = entry.getValue().getEditorConfig().getUser();
        long lastModified = user.getLastModified();
        if (lastModified > maxLastModified) {
          maxLastModified = lastModified;
          lastUser = user;
        }
      }
    }
    return lastUser;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLastModifier(String key, String userId) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      config.getEditorConfig().getUser().setLastModified(System.currentTimeMillis());
      activeCache.put(key, configs);
      activeCache.put(config.getDocId(), configs);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Editor.User getUser(String key, String userId) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null && configs.containsKey(userId)) {
      return configs.get(userId).getEditorConfig().getUser();
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void forceSave(String userId, String key, boolean download, boolean coEdit, boolean forcesaved, String comment) {
    HttpURLConnection connection = null;
    try {
      Userdata userdata = new Userdata(userId, download, coEdit, forcesaved, comment);
      String json = new JSONObject().put("c", "forcesave").put("key", key).put("userdata", userdata.toJSON()).toString();
      byte[] postDataBytes = json.toString().getBytes("UTF-8");

      URL url = new URL(commandServiceUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
      connection.setDoOutput(true);
      connection.setDoInput(true);

      if (documentserverSecret != null && !documentserverSecret.trim().isEmpty()) {
        String jwtToken = Jwts.builder()
                              .setSubject("exo-onlyoffice")
                              .claim("c", "forcesave")
                              .claim("key", key)
                              .claim("userdata", userdata.toJSON())
                              .signWith(Keys.hmacShaKeyFor(documentserverSecret.getBytes()))
                              .compact();
        connection.setRequestProperty("Authorization", "Bearer " + jwtToken);
      }

      try (OutputStream outputStream = connection.getOutputStream()) {
        outputStream.write(postDataBytes);
      }
      catch(Exception e) {
        LOG.error("Error occured while sending request to Document Server: ", e);
      }
      // read the response
      InputStream in = new BufferedInputStream(connection.getInputStream());
      String response = IOUtils.toString(in, "UTF-8");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Command service responded on forcesave command: " + response);
      }
    } catch (Exception e) {
      LOG.error("Error in sending forcesave command. UserId: " + userId + ". Key: " + key + ". Download: " + download, e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  @Override
  public boolean validateToken(String token, String key) {
    if (documentserverSecret == null || documentserverSecret.trim().isEmpty()) {
      return true;
    }
    if (token != null && key != null) {
      try {
        Jws<Claims> jws = Jwts.parser().setSigningKey(Keys.hmacShaKeyFor(documentserverSecret.getBytes())).parseClaimsJws(token);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) jws.getBody().get("payload");
        if (claims != null) {
          if (claims.containsKey("key")) {
            return String.valueOf(claims.get("key")).equals(key);
          }
          if (claims.containsKey("url")) {
            return String.valueOf(claims.get("url")).endsWith(key);
          }
        }
      } catch (Exception e) {
        LOG.warn("Couldn't validate the token: {} key: {} :", token, key, e.getMessage());
      }
    }
    return false;
  }

  @Override
  public void updateTitle(String workspace, String docId, String newTitle, String userId) {
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    if (!setUserConvoState(userId)) {
      LOG.error("Cannot set user conversation state: {}", userId);
      return;
    }
    try {
      newTitle = Text.escapeIllegalJcrChars(newTitle);
      // Check and escape newTitle
      if (StringUtils.isBlank(newTitle)) {
        LOG.warn("Cannot rename document docId: " + docId + " - new title is empty");
        return;
      }
      NodeImpl node = (NodeImpl) getDocumentById(workspace, docId);
      if (node == null) {
        throw new DocumentNotFoundException("Cannot find document. docId: " + docId);
      }

      Node parentNode = node.getParent();
      if (parentNode.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)) {
        parentNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
        parentNode.save();
      }

      if (!node.hasPermission(PermissionType.REMOVE)) {
        Session systemSession = jcrService.getCurrentRepository().getSystemSession(workspace);
        NodeImpl systemNode = (NodeImpl) systemSession.getNodeByUUID(docId);
        systemNode.addMixin("exo:privilegeable");
        systemNode.setPermission(userId,
                                 new String[] { PermissionType.REMOVE, PermissionType.READ, PermissionType.ADD_NODE,
                                     PermissionType.SET_PROPERTY });
        systemNode.save();
      }

      String destPath = parentNode.getPath().equals("/") ? parentNode.getPath() + newTitle : parentNode.getPath() + "/" + newTitle;
      parentNode.getSession().move(node.getPath(), destPath);
      node.setProperty("exo:lastModifier", userId);
      node.setProperty("exo:name", newTitle);
      node.setProperty("exo:title", newTitle);
      node.refresh(true);
      parentNode.getSession().save();

    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Rename is not successful!", e);
      }
    } finally {
      restoreConvoState(contextState, contextProvider);
    }
  }

  /**
   * Gets the user.
   *
   * @param username the username
   * @return the user
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   */
  @Override
  public User getUser(String username) throws OnlyofficeEditorException {
    try {
      return organization.getUserHandler().findUserByName(username);
    } catch (Exception e) {
      throw new OnlyofficeEditorException("Error searching user " + username, e);
    }
  }

  /**
   * Addds file preferences to the node (path for opening shared doc for particular user).
   * @param node the node
   * @param userId the userId
   * @param path the path
   * @throws RepositoryException  the repositoryException
   */
  @Override
  public void addFilePreferences(Node node, String userId, String path) throws RepositoryException {
    Node preferences;
    if (!node.hasNode("eoo:preferences")) {
      if (node.canAddMixin("eoo:onlyofficeFile")) {
        node.addMixin("eoo:onlyofficeFile");
      }
      preferences = node.addNode("eoo:preferences");
    } else {
      preferences = node.getNode("eoo:preferences");
    }

    Node userPreferences;
    if (!preferences.hasNode(userId)) {
      userPreferences = preferences.addNode(userId, "eoo:userPreferences");
    } else {
      userPreferences = preferences.getNode(userId);
    }
    userPreferences.setProperty("path", path);
    node.save();
  }

  // *********************** implementation level ***************

  /**
   * Save link.
   *
   * @param userId the userId
   * @param key the key
   * @param url the url
   */
  protected void saveLink(String userId, String key, String url) {
    ConcurrentMap<String, Config> configs = activeCache.get(key);
    if (configs != null) {
      Config config = configs.get(userId);
      config.getEditorConfig().getUser().setDownloadLink(url);
      config.getEditorConfig().getUser().setLinkSaved(System.currentTimeMillis());
      activeCache.put(key, configs);
      activeCache.put(config.getDocId(), configs);
    }
  }

  /**
   * Downloads document's content to the JCR node when the editor is closed.
   * 
   * @param status the status
   */
  protected void downloadClosed(DocumentStatus status) {
    Config config = status.getConfig();
    // First mark closing, then do actual download and save in storage
    config.closing();
    broadcastEvent(status, OnlyofficeEditorService.EDITOR_CLOSED_EVENT);
    try {
      download(status);
      config.getEditorConfig().getUser().setLastSaved(System.currentTimeMillis());
      config.closed(); // reset transient closing state
    } catch (OnlyofficeEditorException | RepositoryException e) {
      LOG.error("Error occured while downloading document content [Closed]. docId: " + config.getDocId(), e);
    }
  }

  /**
   * Node title.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String nodeTitle(Node node) throws RepositoryException {
    String title = null;
    if (node.hasProperty("exo:title")) {
      title = node.getProperty("exo:title").getString();
    } else if (node.hasProperty("jcr:content/dc:title")) {
      Property dcTitle = node.getProperty("jcr:content/dc:title");
      if (dcTitle.getDefinition().isMultiple()) {
        Value[] dctValues = dcTitle.getValues();
        if (dctValues.length > 0) {
          title = dctValues[0].getString();
        }
      } else {
        title = dcTitle.getString();
      }
    } else if (node.hasProperty("exo:name")) {
      // FYI exo:name seems the same as node name
      title = node.getProperty("exo:name").getString();
    }
    if (title == null) {
      title = node.getName();
    }
    return title;
  }

  /**
   * File type.
   *
   * @param node the node
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String fileType(Node node) throws RepositoryException {
    String title = nodeTitle(node);
    int dotIndex = title.lastIndexOf('.');
    if (dotIndex >= 0 && dotIndex < title.length()) {
      String fileExt = title.substring(dotIndex + 1).trim().toLowerCase();
      if (fileTypes.containsKey(fileExt)) {
        return fileExt;
      }
    }
    return null;
  }

  /**
   * Document type.
   *
   * @param fileType the file type
   * @return the string
   */
  protected String documentType(String fileType) {
    String docType = fileTypes.get(fileType);
    if (docType != null) {
      return docType;
    }
    return TYPE_TEXT; // we assume text document by default
  }

  /**
   * Node content.
   *
   * @param node the node
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node nodeContent(Node node) throws RepositoryException {
    return node.getNode("jcr:content");
  }

  /**
   * Node created.
   *
   * @param node the node
   * @return the calendar
   * @throws RepositoryException the repository exception
   */
  protected Calendar nodeCreated(Node node) throws RepositoryException {
    return node.getProperty("jcr:created").getDate();
  }

  /**
   * Mime type.
   *
   * @param content the content
   * @return the string
   * @throws RepositoryException the repository exception
   */
  protected String mimeType(Node content) throws RepositoryException {
    return content.getProperty("jcr:mimeType").getString();
  }

  /**
   * Data.
   *
   * @param content the content
   * @return the property
   * @throws RepositoryException the repository exception
   */
  protected Property data(Node content) throws RepositoryException {
    return content.getProperty("jcr:data");
  }

  /**
   * Generate id.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the uuid
   */
  protected UUID generateId(String workspace, String path) {
    StringBuilder s = new StringBuilder();
    s.append(workspace);
    s.append(path);
    s.append(System.currentTimeMillis());
    s.append(String.valueOf(RANDOM.nextLong()));

    return UUID.nameUUIDFromBytes(s.toString().getBytes());
  }

  /**
   * Node path with the pattern workspace:path/to/node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the string
   */
  protected String nodePath(String workspace, String path) {
    return new StringBuilder().append(workspace).append(":").append(path).toString();
  }

  /**
   * Node path with the pattern workspace:path/to/node.
   *
   * @param config the config
   * @return the string
   */
  protected String nodePath(Config config) {
    return nodePath(config.getWorkspace(), config.getPath());
  }

  /**
   * Sync users.
   *
   * @param configs the configs
   * @param users the users
   * @return true, if actually changed editor config user(s)
   */
  protected boolean syncUsers(ConcurrentMap<String, Config> configs, String[] users) {
    Set<String> editors = new HashSet<String>(Arrays.asList(users));
    // remove gone editors
    boolean updated = false;
    for (Iterator<Map.Entry<String, Config>> ceiter = configs.entrySet().iterator(); ceiter.hasNext();) {
      Map.Entry<String, Config> ce = ceiter.next();
      String user = ce.getKey();
      Config config = ce.getValue();

      DocumentStatus status = new DocumentStatus.Builder().config(config)
                                                          .key(config.getDocument().getKey())
                                                          .url(config.getEditorUrl())
                                                          .users(users)
                                                          .build();

      if (editors.contains(user)) {
        if (config.isCreated() || config.isClosed()) {
          // editor was (re)opened by user
          config.open();
          fireJoined(status);
          broadcastEvent(status, OnlyofficeEditorService.EDITOR_OPENED_EVENT);
          updated = true;
        }
      } else {
        // editor was closed by user: it will be closing if closed via WebUI of
        // ECMS explorer, open in general case
        if (config.isClosing() || config.isOpen()) {
          // closed because user sync happens when someone else still editing or
          // nothing edited
          config.closed();
          fireLeaved(status);
          broadcastEvent(status, OnlyofficeEditorService.EDITOR_CLOSED_EVENT);
          updated = true;
        }
      }
    }
    return updated;
  }

  /**
   * Gets the current users.
   *
   * @param configs the configs
   * @return the current users
   */
  protected String[] getActiveUsers(ConcurrentMap<String, Config> configs) {
    // copy key set to avoid confuses w/ concurrency
    Set<String> userIds = new LinkedHashSet<String>(configs.keySet());
    // remove not existing locally (just removed), not yet open (created) or
    // already closed
    for (Iterator<String> uiter = userIds.iterator(); uiter.hasNext();) {
      String userId = uiter.next();
      Config config = configs.get(userId);
      if (config == null || config.isCreated() || config.isClosed()) {
        uiter.remove();
      }
    }
    return userIds.toArray(new String[userIds.size()]);
  }

  /**
   * Downloads document's content to the JCR node creating a new version.
   * 
   * @param status the status
   */
  protected void downloadVersion(DocumentStatus status) {
    try {
      download(status);
      status.getConfig().getEditorConfig().getUser().setLastSaved(System.currentTimeMillis());
    } catch (RepositoryException | OnlyofficeEditorException e) {
      LOG.error("Error occured while downloading document [Version]. docId: " + status.getConfig().getDocId(), e);
    }
  }

  /**
   * Downloads document's content to the JCR node.
   * 
   * @param status the status
   * @throws OnlyofficeEditorException the OnlyofficeEditorException
   * @throws RepositoryException the RepositoryException
   */
  protected void download(DocumentStatus status) throws OnlyofficeEditorException, RepositoryException {
    Config config = status.getConfig();
    String workspace = config.getWorkspace();
    String path = config.getPath();

    if (LOG.isDebugEnabled()) {
      LOG.debug(">> download(" + path + ", " + config.getDocument().getKey() + ")");
    }

    String userId = status.getUserId();
    validateUser(userId, config);
    String contentUrl = status.getUrl();
    Calendar editedTime = Calendar.getInstance();
    HttpURLConnection connection = null;
    InputStream data = null;
    // If we have content to download
    if (contentUrl != null) {
      try {
        URL url = new URL(contentUrl);
        connection = (HttpURLConnection) url.openConnection();
        data = connection.getInputStream();
        if (data == null) {
          logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Content stream is null");
          throw new OnlyofficeEditorException("Content stream is null");
        }
      } catch (MalformedURLException e) {
        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Error parsing content URL");
        throw new OnlyofficeEditorException("Error parsing content URL " + contentUrl + " for " + path, e);
      } catch (IOException e) {
        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Error reading content stream");
        throw new OnlyofficeEditorException("Error reading content stream " + contentUrl + " for " + path, e);
      }
    }

    // remember real context state and session provider to restore them at the end
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    try {
      // We want do all the job under actual (last editor) user here
      // Notable that some WCM actions (FileUpdateActivityListener) will fail if
      // user will be anonymous.
      // TODO Consider from a security point: will it be possible to hack to
      // make a download under another user?
      if (!setUserConvoState(userId)) {
        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Cannot set conversation state");
        throw new OnlyofficeEditorException("Cannot set conversation state " + userId);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(">>> download under user " + userId + " (" + path + ", " + config.getDocument().getKey() + ")");
      }
      // work in user session
      Node node = null;
      DocumentNotFoundException notFoundEx = null;
      try {
        node = getDocumentById(workspace, config.getDocId());
        // DocumentNotFoundException if it is and go out the try-catch
        if (node == null) {
          notFoundEx = new DocumentNotFoundException("The document is not found. docId: " + config.getDocId() + ", workspace: "
              + workspace);
          throw notFoundEx;
        }
        if (trashService.isInTrash(node)) {
          notFoundEx = new DocumentNotFoundException("The document is in trash. docId: " + config.getDocId() + ", workspace: "
              + workspace);
          throw notFoundEx;
        }
      } catch (AccessDeniedException e) {
        DocumentStatus errorStatus = new DocumentStatus.Builder().config(config)
                                                                 .error(OnlyofficeEditorListener.FILE_DELETED_ERROR)
                                                                 .build();
        fireError(errorStatus);

        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Access denied");
        notFoundEx = new DocumentNotFoundException("Access denied. docId: " + config.getDocId() + ", workspace: " + workspace);
      } catch (DocumentNotFoundException e) {
        DocumentStatus errorStatus = new DocumentStatus.Builder().config(config)
                                                                 .error(OnlyofficeEditorListener.FILE_DELETED_ERROR)
                                                                 .build();
        fireError(errorStatus);
        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "The document is not found.");
      }
      if (notFoundEx != null) {
        throw notFoundEx;
      }

      Node content = nodeContent(node);
      String nodePath = nodePath(workspace, node.getPath());
      // lock node first, this also will check if node isn't locked by another
      // user (will throw exception)
      final LockState lock = lock(node, config);
      if (lock.canEdit()) {
        // This modifierConfig can be different from 'config'
        Config modifierConfig = getEditor(userId, config.getDocId(), false);
        if (modifierConfig == null) {
          modifierConfig = config;
        }

        try {
          if (node.canAddMixin("eoo:onlyofficeFile")) {
            node.addMixin("eoo:onlyofficeFile");
          }
          // Use current node insted of frozen one when it doesn't exist yet.
          Node frozen = node;
          boolean versionable = node.isNodeType("mix:versionable");
          if (versionable && node.getBaseVersion().hasNode("jcr:frozenNode")) {
            frozen = node.getBaseVersion().getNode("jcr:frozenNode");
          }
          // Used in DocumentUpdateActivityListener
          boolean sameModifier = false;
          Calendar contentCreated = content.getProperty("exo:dateCreated").getDate();
          Calendar contentModified = content.getProperty("exo:dateModified").getDate();
          if (node.hasProperty("exo:lastModifier") && !contentCreated.equals(contentModified)) {
            sameModifier = userId.equals(node.getProperty("exo:lastModifier").getString());
            node.setProperty("exo:lastModifier", userId);
          }

          // TODO: Need to set SameModifier to false if last time the document was saved by forcesave
          modifierConfig.setSameModifier(sameModifier);
          modifierConfig.setPreviousModified(content.getProperty("jcr:lastModified").getDate());

          Boolean onlyofficeVersion = false;
          if (frozen.hasProperty("eoo:onlyofficeVersion")) {
            onlyofficeVersion = frozen.getProperty("eoo:onlyofficeVersion").getBoolean();
          }
          Calendar lastModified = node.getProperty("exo:lastModifiedDate").getDate();
          Calendar versionDate = frozen.getProperty("exo:lastModifiedDate").getDate();
          // Create a version of the manually uploaded draft if exists
          if (versionDate.getTimeInMillis() <= lastModified.getTimeInMillis() && !onlyofficeVersion) {
            createVersionOfDraft(node);
          }

          content.setProperty("jcr:lastModified", editedTime);
          if (content.hasProperty("exo:dateModified")) {
            content.setProperty("exo:dateModified", editedTime);
          }
          if (content.hasProperty("exo:lastModifiedDate")) {
            content.setProperty("exo:lastModifiedDate", editedTime);
          }
          if (node.hasProperty("exo:lastModifiedDate")) {
            node.setProperty("exo:lastModifiedDate", editedTime);
          }

          if (node.hasProperty("exo:dateModified")) {
            node.setProperty("exo:dateModified", editedTime);
          }

          // Add comment to the FileActivity with current file
          String commentId = null;
          String versionSummary = null;
          if (status.getComment() != null && !status.getComment().trim().isEmpty()) {
            String activityId = ActivityTypeUtils.getActivityId(node);
            if (activityId != null) {
              commentId = addComment(activityId, status.getComment(), userId);
              versionSummary = status.getComment().trim();
            }
          }

          if (commentId != null) {
            node.setProperty("eoo:commentId", commentId);
            config.getEditorPage().setComment(status.getComment());
          } else {
            node.setProperty("eoo:commentId", "");
            config.getEditorPage().setComment(null);
          }
          updateCache(config);

          // update document
          if (data != null) {
            content.setProperty("jcr:data", data);
          } else {
            // Set the same data to call listeners
            content.setProperty("jcr:data", content.getProperty("jcr:data").getStream());
          }
          node.save();
          long statusCode = status.getStatus() != null ? status.getStatus() : -1;

          String versioningUser = null;
          if (frozen.hasProperty("eoo:versionOwner")) {
            versioningUser = frozen.getProperty("eoo:versionOwner").getString();
          }

          // Version accumulation for same user
          if (!status.isForcesaved() && versionable && userId.equals(versioningUser)) {
            String versionName = node.getBaseVersion().getName();
            if (LOG.isDebugEnabled()) {
              LOG.debug("Version accumulation: removig version " + versionName + " from node " + nodePath);
            }
            node.getVersionHistory().removeVersion(versionName);
          }

          if (statusCode != 2 && !status.isForcesaved()) {
            node.setProperty("eoo:versionOwner", userId);
          } else {
            node.setProperty("eoo:versionOwner", "");
          }
          node.setProperty("eoo:onlyofficeVersion", true);

          node.save();
          // manage version only if node already mix:versionable
          if (checkout(node)) {
            // Make a new version from the downloaded state
            node.checkin();
            // Since 1.2.0-RC01 we check-out the document to let (more) other
            // actions in ECMS appear on it
            node.checkout();

            // Remove properties from node
            node.setProperty("eoo:versionOwner", "");
            node.setProperty("eoo:onlyofficeVersion", false);

            // Add version summary
            if (versionable && versionSummary != null) {
              String baseVersion = node.getBaseVersion().getName();
              try {
                node.getVersionHistory().addVersionLabel(baseVersion, versionSummary, false);
              } catch (Exception e) {
                LOG.debug("Cannot add version label {}", e.getMessage());
              }
            }
            node.save();

            // If the status code == 2, the EDITOR_SAVED_EVENT should be thrown.
            if (statusCode != 2) {
              broadcastEvent(status, OnlyofficeEditorService.EDITOR_VERSION_EVENT);
            }
          }

          fireSaved(status);
          if (statusCode == 2) {
            broadcastEvent(status, OnlyofficeEditorService.EDITOR_SAVED_EVENT);
          }
        } catch (RepositoryException e) {
          try {
            node.refresh(false); // rollback JCR modifications
          } catch (Throwable re) {
            logError(userId, nodePath, config.getDocId(), config.getDocument().getKey(), "Error rolling back failed change");
          }
          throw e; // let the caller handle it further
        } finally {
          // Remove values after usage in DocumentUdateActivityListener
          modifierConfig.setPreviousModified(null);
          modifierConfig.setSameModifier(null);
          if (data != null) {
            try {
              data.close();
            } catch (Throwable e) {
              logError(userId,
                       nodePath,
                       config.getDocId(),
                       config.getDocument().getKey(),
                       "Error closing exported content stream");
            }
          }
          if (connection != null) {
            try {
              connection.disconnect();
            } catch (Throwable e) {
              logError(userId, nodePath, config.getDocId(), config.getDocument().getKey(), "Error closing export connection");
            }
          }
          try {
            if (node.isLocked() && lock.wasLocked()) {
              unlock(node, lock);
            }
          } catch (Throwable e) {
            logError(userId,
                     config.getPath(),
                     config.getDocId(),
                     config.getDocument().getKey(),
                     "Error unlocking edited document");
          }
        }
      } else {
        logError(userId, config.getPath(), config.getDocId(), config.getDocument().getKey(), "Document locked");
        throw new OnlyofficeEditorException("Document locked " + nodePath);
      }
    } finally {
      restoreConvoState(contextState, contextProvider);
    }
  }

  /**
   * Updates config in the activeCache.
   *
   * @param config the config
   */
  protected void updateCache(Config config) {
    ConcurrentMap<String, Config> configs = activeCache.get(config.getDocument().getKey());
    if (configs != null) {
      activeCache.put(config.getDocument().getKey(), configs);
      activeCache.put(config.getDocId(), configs);
    }
  }

  protected String addComment(String activityId, String commentText, String userId) {
    if (activityId != null && !activityId.isEmpty() && commentText != null && !commentText.trim().isEmpty()) {
      IdentityManager identityManager = WCMCoreUtils.getService(IdentityManager.class);
      org.exoplatform.social.core.identity.model.Identity identity =
                                                                   identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                       userId,
                                                                                                       false);
      ExoSocialActivity activity = activityManager.getActivity(activityId);
      ExoSocialActivity comment = new ExoSocialActivityImpl(identity.getId(),
                                                            SpaceActivityPublisher.SPACE_APP_ID,
                                                            commentText,
                                                            null);
      activityManager.saveComment(activity, comment);
      return comment.getId();
    } else {
      LOG.warn("Cannot add comment. ActivityId and comment shouldn't be null or empty. activityId: {}, comment: {}",
               activityId,
               commentText);
      return null;
    }

  }

  /**
   * Creates a version of draft. Used to create version after manually uploaded
   * content.
   *
   * @param node the node
   * @throws RepositoryException the repository exception
   * @throws OnlyofficeEditorException the onlyoffice exception
   */
  protected void createVersionOfDraft(Node node) throws RepositoryException, OnlyofficeEditorException {
    ConversationState contextState = ConversationState.getCurrent();
    SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
    String userId = node.getProperty("exo:lastModifier").getString();
    if (setUserConvoState(userId)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Creating a version from draft. Path: " + node.getPath() + " user: " + userId);
      }
      try {
        node.save();
        if (checkout(node)) {
          node.checkin();
          node.checkout();
        }
      } catch (Exception e) {
        LOG.error("Couldnl't create a version from draft for user: " + userId);
      }
    } else {
      logError(userId, node.getPath(), node.getUUID(), null, "Cannot set conversation state");
      throw new OnlyofficeEditorException("Cannot set conversation state " + userId);
    }
    restoreConvoState(contextState, contextProvider);
  }

  /**
   * Node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
  protected Node node(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(userSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  /**
   * System node.
   *
   * @param workspace the workspace
   * @param path the path
   * @return the node
   * @throws BadParameterException the bad parameter exception
   * @throws RepositoryException the repository exception
   */
  protected Node systemNode(String workspace, String path) throws BadParameterException, RepositoryException {
    SessionProvider sp = sessionProviders.getSystemSessionProvider(null);
    Session sysSession = sp.getSession(workspace, jcrService.getCurrentRepository());

    Item item = finder.findItem(sysSession, path);
    if (item.isNode()) {
      return (Node) item;
    } else {
      throw new BadParameterException("Not a node " + path);
    }
  }

  /**
   * Node by UUID.
   *
   * @param workspace the workspace
   * @param uuid the UUID
   * @return the node
   * @throws RepositoryException the repository exception
   */
  protected Node nodeByUUID(String workspace, String uuid) throws RepositoryException {
    SessionProvider sp = sessionProviders.getSessionProvider(null);
    Session userSession = sp.getSession(workspace, jcrService.getCurrentRepository());
    return userSession.getNodeByUUID(uuid);
  }

  /**
   * Checkout.
   *
   * @param node the node
   * @return true, if successful
   * @throws RepositoryException the repository exception
   */
  protected boolean checkout(Node node) throws RepositoryException {
    if (node.isNodeType("mix:versionable")) {
      if (!node.isCheckedOut()) {
        node.checkout();
      }
      return true;
    }
    return false;
  }

  /**
   * Unlock given node.
   *
   * @param node the node
   * @param lock the lock
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected void unlock(Node node, LockState lock) throws OnlyofficeEditorException, RepositoryException {
    node.unlock();
    try {
      LockUtil.removeLock(node);
    } catch (Exception e) {
      if (RepositoryException.class.isAssignableFrom(e.getClass())) {
        throw RepositoryException.class.cast(e);
      } else {
        logError(null, node.getPath(), node.getUUID(), null, "Error removing document lock");
        throw new OnlyofficeEditorException("Error removing document lock", e);
      }
    }
  }

  /**
   * Lock the node by current user. If lock attempts will succeed in predefined
   * time this method will throw {@link OnlyofficeEditorException}. If node
   * isn't mix:lockable it will be added first and node saved.
   *
   * @param node {@link Node}
   * @param config {@link Config}
   * @return {@link Lock} acquired by current user.
   * @throws OnlyofficeEditorException the onlyoffice editor exception
   * @throws RepositoryException the repository exception
   */
  protected LockState lock(Node node, Config config) throws OnlyofficeEditorException, RepositoryException {
    if (!node.isNodeType("mix:lockable")) {
      if (!node.isCheckedOut() && node.isNodeType("mix:versionable")) {
        node.checkout();
      }
      node.addMixin("mix:lockable");
      node.save();
    }

    Config.Editor.User user = config.getEditorConfig().getUser();
    LockState lock;
    int attempts = 0;
    try {
      do {
        attempts++;
        if (node.isLocked()) {
          String lockToken;
          try {
            lockToken = LockUtil.getLockToken(node);
          } catch (Exception e) {
            if (RepositoryException.class.isAssignableFrom(e.getClass())) {
              throw RepositoryException.class.cast(e);
            } else {
              logError(config.getEditorConfig().getUser().getId(),
                       node.getPath(),
                       node.getUUID(),
                       config.getDocument().getKey(),
                       "Error reading document lock");
              throw new OnlyofficeEditorException("Error reading document lock", e);
            }
          }
          if (node.getLock().getLockOwner().equals(user.getId()) && lockToken != null) {
            // already this user lock
            node.getSession().addLockToken(lockToken);
            lock = new LockState(lockToken);
          } else {
            // need wait for unlock
            Thread.sleep(LOCK_WAIT_TIMEOUT);
            lock = null;
          }
        } else {
          Lock jcrLock = node.lock(true, false);
          // keep lock token for other sessions of same user
          try {
            LockUtil.keepLock(jcrLock, user.getId(), jcrLock.getLockToken());
          } catch (Exception e) {
            if (RepositoryException.class.isAssignableFrom(e.getClass())) {
              throw RepositoryException.class.cast(e);
            } else {
              logError(config.getEditorConfig().getUser().getId(),
                       node.getPath(),
                       node.getUUID(),
                       config.getDocument().getKey(),
                       "Error saving document lock");
              throw new OnlyofficeEditorException("Error saving document lock", e);
            }
          }
          lock = new LockState(jcrLock);
        }
      } while (lock == null && attempts <= LOCK_WAIT_ATTEMTS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logError(config.getEditorConfig().getUser().getId(),
               node.getPath(),
               node.getUUID(),
               config.getDocument().getKey(),
               "Error waiting for a lock");
      throw new OnlyofficeEditorException("Error waiting for lock of " + nodePath(config.getWorkspace(), config.getPath()), e);
    }
    return lock == null ? new LockState() : lock;
  }

  /**
   * Validate user.
   *
   * @param userId the user id
   * @param config the config
   * @throws BadParameterException if user not found
   * @throws OnlyofficeEditorException if error searching user in organization
   *           service
   */
  protected void validateUser(String userId, Config config) throws BadParameterException, OnlyofficeEditorException {
    User user = getUser(userId);
    if (user == null) {
      LOG.warn("Attempt to access editor document (" + nodePath(config) + ") under not existing user " + userId);
      throw new BadParameterException("User not found for " + config.getDocument().getTitle());
    }
  }

  /**
   * Gets the user lang.
   *
   * @param userId the user id
   * @return the lang can be <code>null</code> if user has no profile or
   *         language in it or user profile error
   */
  protected String getUserLang(String userId) {
    UserProfileHandler hanlder = organization.getUserProfileHandler();
    try {
      UserProfile userProfile = hanlder.findUserProfileByName(userId);
      if (userProfile != null) {
        String lang = userProfile.getAttribute(Constants.USER_LANGUAGE);
        if (lang != null) {
          // XXX Onlyoffice doesn't support country codes (as of Apr 6, 2016)
          // All supported langauges here
          // http://helpcenter.onlyoffice.com/tipstricks/available-languages.aspx
          int cci = lang.indexOf("_");
          if (cci > 0) {
            lang = lang.substring(0, cci);
          }
        } else {
          lang = null;
        }
        return lang;
      } else {
        return null;
      }
    } catch (Exception e) {
      LOG.warn("Error searching user profile " + userId, e);
      return null;
    }
  }

  /**
   * Platform url.
   *
   * @param schema the schema
   * @param host the host
   * @param port the port
   * @return the string builder
   */
  protected StringBuilder platformUrl(String schema, String host, int port) {
    StringBuilder platformUrl = new StringBuilder();
    platformUrl.append(schema);
    platformUrl.append("://");
    platformUrl.append(host);
    if (port >= 0 && port != 80 && port != 443) {
      platformUrl.append(':');
      platformUrl.append(port);
    }
    platformUrl.append('/');
    platformUrl.append(PortalContainer.getCurrentPortalContainerName());

    return platformUrl;
  }

  /**
   * ECMS explorer page URL.
   *
   * @param schema the schema
   * @param host the host
   * @param port the port
   * @param ecmsURL the ECMS URL
   * @return the string builder
   */
  @Deprecated
  protected StringBuilder explorerUrl(String schema, String host, int port, String ecmsURL) {
    StringBuilder explorerUrl = new StringBuilder();
    explorerUrl.append(schema);
    explorerUrl.append("://");
    explorerUrl.append(host);
    if (port >= 0 && port != 80 && port != 443) {
      explorerUrl.append(':');
      explorerUrl.append(port);
    }
    explorerUrl.append(ecmsURL);
    return explorerUrl;
  }

  /**
   * Explorer uri.
   *
   * @param schema the schema
   * @param host the host
   * @param port the port
   * @param ecmsLink the ecms link
   * @return the uri
   */
  protected URI explorerUri(String schema, String host, int port, String ecmsLink) {
    URI uri;
    try {
      ecmsLink = URLDecoder.decode(ecmsLink, StandardCharsets.UTF_8.name());
      String[] linkParts = ecmsLink.split("\\?");
      if (linkParts.length >= 2) {
        uri = new URI(schema, null, host, port, linkParts[0], linkParts[1], null);
      } else {
        uri = new URI(schema, null, host, port, ecmsLink, null, null);
      }
    } catch (Exception e) {
      LOG.warn("Error creating document URI", e);
      try {
        uri = URI.create(ecmsLink);
      } catch (Exception e1) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Error creating document URI from ECMS link and after error: " + e.getMessage(), e1);
        }
        uri = null;
      }
    }
    return uri;
  }

  /**
   * ECMS explorer page relative URL (within the Platform).
   *
   * @param jcrPath the jcr path
   * @return the string
   */
  protected String explorerLink(String jcrPath) {
    try {
      return documentService.getLinkInDocumentsApp(jcrPath);
    } catch (Exception e) {
      LOG.warn("Error creating document link for " + jcrPath, e);
      return new StringBuilder().append('/').append(PortalContainer.getCurrentPortalContainerName()).toString();
    }
  }

  /**
   * Platform REST URL.
   *
   * @param platformUrl the platform URL
   * @return the string builder
   */
  protected StringBuilder platformRestUrl(CharSequence platformUrl) {
    StringBuilder restUrl = new StringBuilder(platformUrl);
    restUrl.append('/');
    restUrl.append(PortalContainer.getCurrentRestContextName());

    return restUrl;
  }

  /**
   * Broadcasts an event using the listenerService.
   * 
   * @param status the status
   * @param eventType the eventType
   */
  protected void broadcastEvent(DocumentStatus status, String eventType) {
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Fire {} event. DocumentStatus: {}", eventType, status.toJSON());
      }
      listenerService.broadcast(eventType, this, status);
    } catch (Exception e) {
      LOG.error("Error firing listener with Onlyoffice {} event for user: {}, document: {}",
                eventType,
                status.getConfig().getEditorConfig().getUser().getId(),
                status.getConfig().getDocId(),
                e);
    }
  }

  /**
   * Fire created.
   *
   * @param status the status
   */
  protected void fireCreated(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onCreate(status);
      } catch (Throwable t) {
        LOG.warn("Creation listener error", t);
      }
    }
  }

  /**
   * Fire get.
   *
   * @param status the status
   */
  protected void fireGet(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onGet(status);
      } catch (Throwable t) {
        LOG.warn("Read (Get) listener error", t);
      }
    }
  }

  /**
   * Fire joined.
   *
   * @param status the status
   */
  protected void fireJoined(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onJoined(status);
      } catch (Throwable t) {
        LOG.warn("User joining listener error", t);
      }
    }
  }

  /**
   * Fire leaved.
   *
   * @param status the status
   */
  protected void fireLeaved(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onLeaved(status);
      } catch (Throwable t) {
        LOG.warn("User leaving listener error", t);
      }
    }
  }

  /**
   * Fire saved.
   *
   * @param status the status
   */
  protected void fireSaved(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onSaved(status);
      } catch (Throwable t) {
        LOG.warn("Saving listener error", t);
      }
    }
  }

  /**
   * Fire error.
   *
   * @param status the status
   */
  protected void fireError(DocumentStatus status) {
    for (OnlyofficeEditorListener l : listeners) {
      try {
        l.onError(status);
      } catch (Throwable t) {
        LOG.warn("Error listener error", t);
      }
    }
  }

  /**
   * Find or create user identity.
   *
   * @param userId the user id
   * @return the identity can be null if not found and cannot be created via
   *         current authenticator
   */
  protected Identity userIdentity(String userId) {
    Identity userIdentity = identityRegistry.getIdentity(userId);
    if (userIdentity == null) {
      // We create user identity by authenticator, but not register it in the
      // registry
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("User identity not registered, trying to create it for: " + userId);
        }
        userIdentity = authenticator.createIdentity(userId);
      } catch (Exception e) {
        LOG.warn("Failed to create user identity: " + userId, e);
      }
    }
    return userIdentity;
  }

  /**
   * Get lower case copy of the given string.
   *
   * @param str the str
   * @return the string
   */
  protected String lowerCase(String str) {
    return str.toUpperCase().toLowerCase();
  }

  /**
   * Editor URL path.
   *
   * @param docId the doc id
   * @return the string
   */
  protected String editorURLPath(String docId) {
    return new StringBuilder().append('/')
                              .append(CommonsUtils.getCurrentPortalOwner())
                              .append("/oeditor?docId=")
                              .append(docId)
                              .toString();
  }

  /**
   * Logs editor errors.
   *
   * @param userId the userId
   * @param path the path
   * @param docId the docId
   * @param key the key
   * @param reason the reason
   */
  protected void logError(String userId, String path, String docId, String key, String reason) {
    LOG.error("Editor error: " + reason + " [UserId: " + userId + ", docId: " + docId + ", path: " + path + ", key: " + key
        + "]");
  }

  /**
   * Sets ConversationState by userId.
   *
   * @param userId the userId
   * @return true if successful, false when the user is not found
   */
  @SuppressWarnings("deprecation")
  protected boolean setUserConvoState(String userId) {
    Identity userIdentity = userIdentity(userId);
    if (userIdentity != null) {
      ConversationState state = new ConversationState(userIdentity);
      // Keep subject as attribute in ConversationState.
      state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
      ConversationState.setCurrent(state);
      SessionProvider userProvider = new SessionProvider(state);
      sessionProviders.setSessionProvider(null, userProvider);
      return true;
    }
    LOG.warn("User identity not found " + userId + " for setting conversation state");
    return false;
  }

  /**
   * Restores the conversation state.
   * 
   * @param contextState the contextState
   * @param contextProvider the contextProvider
   */
  protected void restoreConvoState(ConversationState contextState, SessionProvider contextProvider) {
    ConversationState.setCurrent(contextState);
    sessionProviders.setSessionProvider(null, contextProvider);
  }

  /**
   * Initializes fileTypes map
   */
  protected void initFileTypes() {
    fileTypes.put("docx", TYPE_TEXT);
    fileTypes.put("doc", TYPE_TEXT);
    fileTypes.put("odt", TYPE_TEXT);
    fileTypes.put("txt", TYPE_TEXT);
    fileTypes.put("rtf", TYPE_TEXT);
    fileTypes.put("mht", TYPE_TEXT);
    fileTypes.put("html", TYPE_TEXT);
    fileTypes.put("htm", TYPE_TEXT);
    fileTypes.put("epub", TYPE_TEXT);
    fileTypes.put("pdf", TYPE_TEXT);
    fileTypes.put("djvu", TYPE_TEXT);
    fileTypes.put("xps", TYPE_TEXT);
    // Speadsheet formats
    fileTypes.put("xlsx", TYPE_SPREADSHEET);
    fileTypes.put("xls", TYPE_SPREADSHEET);
    fileTypes.put("ods", TYPE_SPREADSHEET);
    // Presentation formats
    fileTypes.put("pptx", TYPE_PRESENTATION);
    fileTypes.put("ppt", TYPE_PRESENTATION);
    fileTypes.put("ppsx", TYPE_PRESENTATION);
    fileTypes.put("pps", TYPE_PRESENTATION);
    fileTypes.put("odp", TYPE_PRESENTATION);
  }

  /**
   * Adds debug listener to active cache
   */
  protected void addDebugCacheListener() {
    activeCache.addCacheListener(new CacheListener<String, ConcurrentMap<String, Config>>() {

      @Override
      public void onExpire(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
        LOG.debug(CACHE_NAME + " onExpire > " + key + ": " + obj);
      }

      @Override
      public void onRemove(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
        LOG.debug(CACHE_NAME + " onRemove > " + key + ": " + obj);
      }

      @Override
      public void onPut(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
        LOG.debug(CACHE_NAME + " onPut > " + key + ": " + obj);
      }

      @Override
      public void onGet(CacheListenerContext context, String key, ConcurrentMap<String, Config> obj) throws Exception {
        LOG.debug(CACHE_NAME + " onGet > " + key + ": " + obj);
      }

      @Override
      public void onClearCache(CacheListenerContext context) throws Exception {
        LOG.debug(CACHE_NAME + " onClearCache");
      }
    });
  }

  /**
   * Gets documentserver host name.
   *
   * @param dsSchema the dsSchema
   * @param dsHost the dsHost
   * @return hostname
   * @throws ConfigurationException the configurationException
   */
  protected String getDocumentserverHost(String dsSchema, String dsHost) throws ConfigurationException {
    if (dsSchema == null || (dsSchema = dsSchema.trim()).length() == 0) {
      dsSchema = "http";
    }
    if (dsHost == null || (dsHost = dsHost.trim()).length() == 0) {
      throw new ConfigurationException("Configuration of " + CONFIG_DS_HOST + " required");
    }
    int portIndex = dsHost.indexOf(HTTP_PORT_DELIMITER);
    if (portIndex > 0) {
      // cut port from DS host to use in canDownloadBy() method
      return dsHost.substring(0, portIndex);
    }
    return dsHost;
  }

  /**
   * Gets allowed hosts.
   *
   * @param dsAllowedHost the dsAllowedHost
   * @return allowed hosts
   */
  protected Set<String> getDocumentserverAllowedHosts(String dsAllowedHost) {
    if (dsAllowedHost != null && !dsAllowedHost.isEmpty()) {
      Set<String> allowedhosts = new HashSet<>();
      for (String ahost : dsAllowedHost.split(",")) {
        ahost = ahost.trim();
        if (!ahost.isEmpty()) {
          allowedhosts.add(lowerCase(ahost));
        }
      }
      return Collections.unmodifiableSet(allowedhosts);
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Gets display path. 
   *
   * @param node the node
   * @param userId the userId
   * @return the display path
   */
  protected String getDisplayPath(Node node, String userId) {
    try {
      DriveData driveData = documentService.getDriveOfNode(node.getPath());
      List<String> elems = Arrays.asList(node.getPath().split("/"));
      String parentFolder;
      try {
        parentFolder = node.getParent().getProperty("exo:title").getString();
      } catch (Exception e) {
        LOG.debug("Couldn't get exo:title from node parent. Node {}, message {}", node.getPath(), e.getMessage());
        parentFolder = elems.get(elems.size() - 2);
      }
      String title = node.hasProperty("exo:title") ? node.getProperty("exo:title").getString() : elems.get(elems.size() - 1);
      String drive = "";
      if (driveData != null) {
        String driveName = driveData.getName();
        // User's documents
        if (node.getPath().startsWith(usersPath)) {
          drive = driveName;
          // Shared document
          if (!userId.equals(getUserId(node.getPath()))) {
            Node symlink = getSymlink(node, userId);
            if (symlink != null) {
              return getDisplayPath(symlink, userId);
            } else {
              parentFolder = HIDDEN_FOLDER;
            }
          }
          // Spaces's documents
        } else if (driveName.startsWith(".spaces.")) {
          String spacePrettyName = driveName.substring(driveName.lastIndexOf(".") + 1);
          Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
          if (space != null) {
            drive = "spaces/" + space.getDisplayName();
          } else {
            LOG.warn("Cannot find space by pretty name {}", spacePrettyName);
            drive = spacePrettyName;
          }
          // Group's documents
        } else if (driveName.startsWith(".platform.")) {
          String groupId = driveName.replaceAll("\\.", "/");
          Group group = organization.getGroupHandler().findGroupById(groupId);
          if (group != null) {
            drive = group.getLabel();
          } else {
            LOG.warn("Cannot find group by id {}", groupId);
            drive = groupId;
          }
        }
        else {
          drive = driveData.getName();
        }
      }
      return drive + ":" + parentFolder + "/" + title;
    } catch (Exception e) {
      LOG.error("Error occured while creating display path", e);
      return null;
    }
  }

  /**
   * Gets parent folder of the file based on file preferences
   * @param node the node
   * @param userId the userId
   * @return the Node
   * @throws Exception the exception
   */
  protected Node getSymlink(Node node, String userId) throws Exception {
    if (node.hasNode("eoo:preferences")) {
      Node filePreferences = node.getNode("eoo:preferences");
      if (filePreferences.hasNode(userId)) {
        Node userPreferences = filePreferences.getNode(userId);
        String path = userPreferences.getProperty("path").getString();
        return (Node) node.getSession().getItem(path);
      }
    }
    return null;
  }

  /**
   * Gets userId from node path.
   * 
   * @param path the node path
   * @return the userId
   */
  protected String getUserId(String path) {
    List<String> elems = Arrays.asList(path.split("/"));
    int position = 2;
    while (elems.get(position).endsWith("_")) {
      position++;
    }
    return elems.get(position);
  }

  /**
   * Gets comment of last version of node
   *
   * @param node the node
   * @return the comment or null
   */
  protected String nodeComment(Node node) {
    try {
      if (node.hasProperty("eoo:commentId")) {
        String commentId = node.getProperty("eoo:commentId").getString();
        if (commentId != null && !commentId.isEmpty()) {
          ExoSocialActivity comment = activityManager.getActivity(commentId);
          return comment != null ? comment.getTitle() : null;
        }
      }
    } catch (Exception e) {
      LOG.warn("Cannot get eoo:commentId of node.", e);
    }
    return null;
  }

  /**
   * Checks if current user can rename the document.
   *
   * @param node the node
   * @return true if user can rename 
   */
  protected boolean canRenameDocument(Node node) {
    try {
      NodeImpl parent = (NodeImpl) node.getParent();
      return parent.hasPermission(PermissionType.READ) && parent.hasPermission(PermissionType.ADD_NODE)
          && parent.hasPermission(PermissionType.SET_PROPERTY);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Gets lastmodified from node.
   *
   * @param node the node
   * @return the lastmodified
   * @throws ValueFormatException the valueFormatException
   * @throws PathNotFoundException the pathNotFoundException
   * @throws RepositoryException the repositoryException
   */
  protected String getLastModified(Node node) throws ValueFormatException, PathNotFoundException, RepositoryException {
    if (node.hasProperty("exo:lastModifiedDate")) {
      Calendar date = node.getProperty("exo:lastModifiedDate").getDate();
      Locale locale;
      try {
        locale = Util.getPortalRequestContext().getLocale();
      } catch (Exception e) {
        LOG.warn("Cannot get locale from portal request context. {}", e.getMessage());
        locale = Locale.getDefault();
      }
      SimpleDateFormat dateFormat = new SimpleDateFormat(LAST_EDITED_DATE_FORMAT, locale);
      return dateFormat.format(date.getTimeInMillis());
    }
    return null;
  }

  /**
   * Gets last modifier display name from node.
   * 
   * @param node the node.
   * @return the display name of last modifier
   * @throws ValueFormatException the valueFormatException
   * @throws PathNotFoundException the pathNotFoundException
   * @throws RepositoryException the repositoryException
   * @throws OnlyofficeEditorException the onlyofficeEditorException
   */
  protected String getLastModifier(Node node) throws ValueFormatException,
                                              PathNotFoundException,
                                              RepositoryException,
                                              OnlyofficeEditorException {
    if (node.hasProperty("exo:lastModifier")) {
      String lastModifierId = node.getProperty("exo:lastModifier").getString();
      User modifier = getUser(lastModifierId);
      if (modifier != null) {
        return modifier.getDisplayName();
      }
    }
    return null;
  }

}