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
package org.exoplatform.onlyoffice.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.onlyoffice.BadParameterException;
import org.exoplatform.onlyoffice.ChangeState;
import org.exoplatform.onlyoffice.Config;
import org.exoplatform.onlyoffice.DocumentContent;
import org.exoplatform.onlyoffice.DocumentStatus;
import org.exoplatform.onlyoffice.OnlyofficeEditorException;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.onlyoffice.Userdata;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;

/**
 * REST service implementing Onlyoffice config storage service. <br>
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: EditorService.java 00000 Feb 12, 2016 pnedonosko $
 */
@Path("/onlyoffice/editor")
public class EditorService implements ResourceContainer {

  /** The Constant API_VERSION. */
  public static final String API_VERSION = "1.1";

  /** The Constant LOG. */
  protected static final Log LOG         = ExoLogger.getLogger(EditorService.class);

  /**
   * Response builder for connect and state.
   */
  class EditorResponse extends ServiceResponse {

    /** The config. */
    Config config;

    /** The editor Url. */
    String editorUrl;

    /** The error. */
    String error;

    /**
     * Config.
     *
     * @param config the config
     * @return the editor response
     */
    EditorResponse config(Config config) {
      this.config = config;
      return this;
    }

    /**
     * Document.
     *
     * @param editorUrl the editor url
     * @return the editor response
     */
    EditorResponse document(String editorUrl) {
      this.editorUrl = editorUrl;
      return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @return the editor response
     */
    EditorResponse error(String error) {
      this.error = error;
      return this;
    }

    /**
     * Error.
     *
     * @param error the error
     * @param host the host
     * @return the editor response
     */
    EditorResponse error(String error, String host) {
      this.error = error;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the response
     * @inherritDoc
     */
    @Override
    Response build() {
      if (config != null) {
        super.entity(config);
      } else if (editorUrl != null) {
        super.entity(new StringBuilder("{\"editorUrl\":\"").append(editorUrl).append("\"}").toString());
      } else if (error != null) {
        super.entity(new StringBuilder("{\"error\":\"").append(error).append("\"}").toString());
      }
      return super.build();
    }
  }

  /** The editors. */
  protected final OnlyofficeEditorService editors;

  /** The initiated. */
  protected final Map<UUID, Config>       initiated = new ConcurrentHashMap<UUID, Config>();

  /**
   * REST cloudDrives uses {@link OnlyofficeEditorService} for actual job.
   *
   * @param editors the editors
   */
  public EditorService(OnlyofficeEditorService editors) {
    this.editors = editors;
  }

  /**
   * Config status posted from the Document Server. <br>
   * WARNING! It is publicly accessible service but access from the Documents
   * Server host can be restricted (by default).
   *
   * @param uriInfo - request info
   * @param request the request
   * @param userId the user id
   * @param key - config key generated when requested editor config
   * @param statusText the status text
   * @return {@link Response}
   */
  @POST
  @Path("/status/{userId}/{key}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response status(@Context UriInfo uriInfo,
                         @Context HttpServletRequest request,
                         @PathParam("userId") String userId,
                         @PathParam("key") String key,
                         String statusText) {

    String clientHost = getClientHost(request);
    String clientIp = getClientIpAddr(request);

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document status: " + userId + "@" + key + " " + statusText + " from " + clientHost + "(" + clientIp
          + ")");
    }

    EditorResponse resp = new EditorResponse();
    if (editors.canDownloadBy(clientHost) || editors.canDownloadBy(clientIp)) {
      try {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(statusText);
        JSONObject jsonObj = (JSONObject) obj;
        String token = request.getHeader("Authorization");
        if (token != null) {
          token = token.replace("Bearer", "").trim();
        }
        String statusKey = (String) jsonObj.get("key");
        String userdataJson = (String) jsonObj.get("userdata");
        long statusCode = (long) jsonObj.get("status");
        String statusUrl = (String) jsonObj.get("url");
        Object errorObj = jsonObj.get("error");
        long error = errorObj != null ? Long.parseLong(errorObj.toString()) : 0;

        // Oct 2017: When Document server calls with status 4 (user closed w/o
        // modification), the users array will be null
        JSONArray statusUsersArray = (JSONArray) jsonObj.get("users");

        @SuppressWarnings("unchecked")
        String[] statusUsers = statusUsersArray != null ? (String[]) statusUsersArray.toArray(new String[statusUsersArray.size()])
                                                        : new String[0];

        if (key != null && key.length() > 0) {
          if (userId != null && userId.length() > 0) {
            if (editors.validateToken(token, key)) {
              DocumentStatus.Builder statusBuilder = new DocumentStatus.Builder();
              statusBuilder.key(statusKey != null && statusKey.length() > 0 ? statusKey : key)
                           .status(statusCode)
                           .url(statusUrl)
                           .users(statusUsers)
                           .error(error);
              Userdata userdata = userdataJson != null ? new ObjectMapper().readValue(userdataJson, Userdata.class) : null;
              if (userdata != null) {
                statusBuilder.userId(userdata.getUserId());
                statusBuilder.coEdited(userdata.getCoEdited());
                statusBuilder.forcesaved(userdata.isForcesaved());
                statusBuilder.saved(userdata.isDownload());
                statusBuilder.comment(userdata.getComment());
              } else if (statusUsers != null && statusUsers.length > 0) {
                // Last user
                statusBuilder.userId(statusUsers[0]);
              } else {
                statusBuilder.userId(userId);
              }
              try {
                editors.updateDocument(statusBuilder.build());
                resp.entity("{\"error\": 0}");
              } catch (BadParameterException e) {
                LOG.warn("Bad parameter to update status for " + key + ". " + e.getMessage());
                resp.error(e.getMessage()).status(Status.BAD_REQUEST);
              } catch (OnlyofficeEditorException e) {
                LOG.error("Error handling status for " + key, e);
                resp.error("Error handling status. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
              } catch (RepositoryException e) {
                LOG.error("Storage error while handling status for " + key, e);
                resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
              } catch (Throwable e) {
                LOG.error("Runtime error while handling status for " + key, e);
                resp.error("Runtime error.").status(Status.INTERNAL_SERVER_ERROR);
              }
            } else {
              LOG.warn("Error processing editor status. The token is not valid");
              resp.error("The token is not valid").status(Status.UNAUTHORIZED);
            }
          } else {
            LOG.warn("Error processing editor status. User not provided");
            resp.error("User not provided").status(Status.BAD_REQUEST);
          }
        } else {
          resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
        }
      } catch (ParseException | IOException e) {
        LOG.warn("JSON parse error while handling status for " + key + ". JSON: " + statusText, e);
        resp.error("JSON parse error: " + e.getMessage()).status(Status.BAD_REQUEST);
      }
    } else {
      LOG.warn("Attempt to update status by not allowed host: " + clientHost + "(" + clientIp + ")");
      resp.error("Not a document server").status(Status.UNAUTHORIZED);
    }
    return resp.build();
  }

  /**
   * Document content download link. <br>
   * WARNING! It is publicly accessible service but access from the Documents
   * Server host can be restricted (by default).
   *
   * @param uriInfo - request info
   * @param request the request
   * @param userId the user id
   * @param key - file key generated by /config method
   * @return {@link Response}
   */
  @GET
  @Path("/content/{userId}/{key}")
  public Response content(@Context UriInfo uriInfo,
                          @Context HttpServletRequest request,
                          @PathParam("userId") String userId,
                          @PathParam("key") String key) {
    String clientHost = getClientHost(request);
    String clientIp = getClientIpAddr(request);

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice document content: " + userId + "@" + key + " to " + clientHost + "(" + clientIp + ")");
    }

    EditorResponse resp = new EditorResponse();
    if (editors.canDownloadBy(clientHost) || editors.canDownloadBy(clientIp)) {
      if (key != null && key.length() > 0) {
        String token = request.getHeader("Authorization");
        if (token != null) {
          token = token.replace("Bearer", "").trim();
        }
        if (editors.validateToken(token, key)) {
          try {
            if (userId != null && userId.length() > 0) {
              DocumentContent content = editors.getContent(userId, key);
              resp.entity(content.getData()).type(content.getType()).ok();
            } else {
              LOG.error("Error downloading content. User identity not provided");
              resp.error("User not provided").status(Status.BAD_REQUEST);
            }
          } catch (BadParameterException e) {
            LOG.warn("Bad parameter to downloading content for " + key + ". " + e.getMessage());
            resp.error(e.getMessage()).status(Status.BAD_REQUEST);
          } catch (OnlyofficeEditorException e) {
            LOG.error("Error downloading content for " + key, e);
            resp.error("Error downloading content. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
          } catch (RepositoryException e) {
            LOG.error("Storage error while downloading content for " + key, e);
            resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
          } catch (Throwable e) {
            LOG.error("Runtime error while downloading content for " + key, e);
            resp.error("Runtime error.").status(Status.INTERNAL_SERVER_ERROR);
          }
        } else {
          LOG.warn("Error downloading content. The token is not valid");
          resp.error("The token is not valid").status(Status.UNAUTHORIZED);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
      }
    } else {
      LOG.warn("Attempt to download content by not allowed host: " + clientHost + "(" + clientIp + ")");
      resp.error("Not a document server").status(Status.UNAUTHORIZED);
    }
    return resp.build();
  }

  /**
   * Create configuration for Onlyoffice JS.
   *
   * @param uriInfo - request with base URI
   * @param request the request
   * @param workspace the workspace
   * @param path the path
   * @return response with
   */
  @POST
  @Path("/config/{workspace}/{path:.*}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response configPost(@Context UriInfo uriInfo,
                             @Context HttpServletRequest request,
                             @PathParam("workspace") String workspace,
                             @PathParam("path") String path) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("> Onlyoffice configPost: " + workspace + ":" + path);
    }

    EditorResponse resp = new EditorResponse();
    if (workspace != null) {
      if (path != null) {
        if (!path.startsWith("/")) {
          path = "/" + path;
        }
        try {
          ConversationState convo = ConversationState.getCurrent();
          if (convo != null) {
            String username = convo.getIdentity().getUserId();
            URI requestUri = uriInfo.getRequestUri();
            Config config = editors.createEditor(requestUri.getScheme(),
                                                 requestUri.getHost(),
                                                 requestUri.getPort(),
                                                 username,
                                                 workspace,
                                                 path);
            if (config.getEditorConfig().getLang() == null) {
              if (request.getLocale() != null) {
                // If user lang not defined use current request one
                config.getEditorConfig().setLang(request.getLocale().getLanguage());
              } else {
                // Otherwise use system default one
                config.getEditorConfig().setLang(Locale.getDefault().getLanguage());
              }
            }
            if (LOG.isDebugEnabled()) {
              LOG.debug("> Onlyoffice document config: " + workspace + ":" + path + " -> " + config.getDocument().getKey());
            }
            resp.config(config).ok();
          } else {
            LOG.warn("ConversationState not set to create editor config");
            resp.error("User not authenticated").status(Status.UNAUTHORIZED);
          }
        } catch (BadParameterException e) {
          LOG.warn("Bad parameter for creating editor config " + workspace + ":" + path + ". " + e.getMessage());
          resp.error(e.getMessage()).status(Status.BAD_REQUEST);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Error creating editor config " + workspace + ":" + path, e);
          resp.error("Error creating editor config. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
        } catch (RepositoryException e) {
          LOG.error("Storage error while creating editor config " + workspace + ":" + path, e);
          resp.error("Storage error.").status(Status.INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
          LOG.error("Runtime error while creating editor config " + workspace + ":" + path, e);
          resp.error("Error creating editor config.").status(Status.INTERNAL_SERVER_ERROR);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null path.");
      }
    } else {
      resp.status(Status.BAD_REQUEST).error("Null workspace.");
    }
    return resp.build();
  }

  /**
   * Return Onlyoffice REST API version.
   *
   * @param uriInfo - request with base URI
   * @param request the request
   * @return response with
   */
  @GET
  @Path("/api/version")
  @Produces(MediaType.APPLICATION_JSON)
  public Response versionGet(@Context UriInfo uriInfo, @Context HttpServletRequest request) {

    String title = this.getClass().getPackage().getImplementationTitle();
    String version = this.getClass().getPackage().getImplementationVersion();

    String clientHost = getClientHost(request);
    String clientIp = getClientIpAddr(request);
    return Response.ok()
                   .entity("{\"user\": \"" + request.getRemoteUser() + "\",\n\"requestIP\": \"" + clientIp
                       + "\",\n\"requestHost\": \"" + clientHost + "\",\n\"product\":{ \"name\": \"" + title
                       + "\",\n\"version\": \"" + version + "\"},\n\"version\": \"" + API_VERSION + "\"}")
                   .type(MediaType.APPLICATION_JSON)
                   .build();
  }

  /**
   * Editing document state in local storage.
   *
   * @param uriInfo - request info
   * @param userId the user id
   * @param key - config key generated when requested editor config
   * @return {@link Response}
   */
  @GET
  @Path("/state/{userId}/{key}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response localState(@Context UriInfo uriInfo, @PathParam("userId") String userId, @PathParam("key") String key) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("> localState: " + userId + "@" + key);
    }

    EditorResponse resp = new EditorResponse();
    if (userId != null) {
      if (key != null) {
        try {
          ChangeState status = editors.getState(userId, key);
          resp.entity(status).ok();
        } catch (BadParameterException e) {
          LOG.warn("Bad parameter for getting document state " + userId + "@" + key + ". " + e.getMessage());
          resp.error(e.getMessage()).status(Status.BAD_REQUEST);
        } catch (OnlyofficeEditorException e) {
          LOG.error("Error getting document state " + userId + "@" + key, e);
          resp.error("Error getting document state. " + e.getMessage()).status(Status.INTERNAL_SERVER_ERROR);
        } catch (Throwable e) {
          LOG.error("Runtime error while getting document state " + userId + "@" + key, e);
          resp.error("Error getting document state.").status(Status.INTERNAL_SERVER_ERROR);
        }
      } else {
        resp.status(Status.BAD_REQUEST).error("Null or empty file key.");
      }
    } else {
      LOG.warn("Error getting document state. User identity not provided");
      resp.error("User not provided").status(Status.BAD_REQUEST);
    }

    return resp.build();
  }

  /**
   * Gets the client ip addr.
   *
   * @param request the request
   * @return the client ip addr
   */
  protected String getClientIpAddr(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (isValidHost(ip)) {
      // In case of several proxy: X-Forwarded-For: client, proxy1, proxy2
      int commaIdx = ip.indexOf(',');
      if (commaIdx > 0 && commaIdx < ip.length() - 1) {
        // use only client IP
        ip = ip.substring(0, commaIdx);
      }
      return ip;
    }
    ip = request.getHeader("X-Real-IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("Proxy-Client-IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("WL-Proxy-Client-IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_CLIENT_IP");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_FORWARDED");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
    if (isValidHost(ip)) {
      return ip;
    }
    // http://stackoverflow.com/questions/1634782/what-is-the-most-accurate-way-to-retrieve-a-users-correct-ip-address-in-php
    ip = request.getHeader("HTTP_FORWARDED_FOR");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("HTTP_FORWARDED");
    if (isValidHost(ip)) {
      return ip;
    }
    ip = request.getHeader("REMOTE_ADDR");
    if (isValidHost(ip)) {
      return ip;
    }
    // last chance to get it from Servlet request
    ip = request.getRemoteAddr();
    if (isValidHost(ip)) {
      return ip;
    }
    return null;
  }

  /**
   * Gets the client host.
   *
   * @param request the request
   * @return the client host
   */
  protected String getClientHost(HttpServletRequest request) {
    String host = request.getHeader("X-Forwarded-Host");
    if (isValidHost(host)) {
      // This header contain requested (!) host name, not a client one, but in
      // case of multi-layer infra
      // (several proxy/firewall) where one of proxy hosts stands in front of
      // actual Document Server and set
      // this header, it will do the job.
      return host;
    }
    // Oct 19, 2017: Solution based on X-Forwarded-For proposed in #3 to work
    // correctly behind reverse proxy
    String clientIp = request.getHeader("X-Forwarded-For");
    if (notEmpty(clientIp)) {
      // In case of several proxy: X-Forwarded-For: client, proxy1, proxy2
      int commaIdx = clientIp.indexOf(',');
      if (commaIdx > 0 && commaIdx < clientIp.length() - 1) {
        // use only client IP
        clientIp = clientIp.substring(0, commaIdx);
      }
    } else {
      // And a case of nginx, try X-Real-IP
      clientIp = request.getHeader("X-Real-IP");
    }
    if (notEmpty(clientIp)) {
      try {
        // XXX For this to work, in server.xml, enableLookups="true" and it can
        // be resource consumption call
        // Thus it could be efficient to use the hosts file of the server
        host = InetAddress.getByName(clientIp).getHostName();
        if (notEmpty(host)) { // host here still may be an IP due to security
                              // restriction
          return host;
        }
      } catch (Exception e) {
        LOG.warn("Cannot obtain client hostname by its IP " + clientIp + ": " + e.getMessage());
      }
    }
    host = request.getRemoteHost();
    if (isValidHost(host)) {
      return host;
    }
    return clientIp; // was null - Dec 20, 2017
  }

  /**
   * Check string is not empty.
   *
   * @param str the str
   * @return true, if not empty, false otherwise
   */
  protected boolean notEmpty(String str) {
    return str != null && str.length() > 0;
  }

  /**
   * Checks if is valid host. It's a trivial check for <code>null</code>, non
   * empty string and not "unknown" text.
   *
   * @param host the host name or IP address
   * @return true, if is valid host
   */
  protected boolean isValidHost(String host) {
    if (notEmpty(host) && !"unknown".equalsIgnoreCase(host)) {
      return true;
    }
    return false;
  }

}
