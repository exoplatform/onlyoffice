/**
 * Onlyoffice Editor client.
 */
(function($, cCometD, redux) {
  "use strict";
  // ******** polyfills ********
  if (!String.prototype.endsWith) {
    String.prototype.endsWith = function(search, this_len) {
      if (this_len === undefined || this_len > this.length) {
        this_len = this.length;
      }
      return this.substring(this_len - search.length, this_len) === search;
    };
  }
  if (!String.prototype.startsWith) {
    String.prototype.startsWith = function(searchString, position) {
      position = position || 0;
      return this.substr(position, searchString.length) === searchString;
    };
  }

  // ******** Utils ********

  var getRandomArbitrary = function(min, max) {
    return Math.floor((Math.random() * (max - min - 1) + min) + 1);
  };

  /**
   * Universal client ID for use in logging, services connectivity and related cases.
   */
  var clientId = "" + getRandomArbitrary(100000, 999998);

  /**
   * Stuff grabbed from CW's commons.js
   */
  var pageBaseUrl = function(theLocation) {
    if (!theLocation) {
      theLocation = window.location;
    }

    var theHostName = theLocation.hostname;
    if (theLocation.port) {
      theHostName += ":" + theLocation.port;
    }

    return theLocation.protocol + "//" + theHostName;
  };

  /**
   * Add style to current document (to the end of head).
   */
  var loadStyle = function(cssUrl) {
    if (document.createStyleSheet) {
      document.createStyleSheet(cssUrl);
      // IE way
    } else {
      if ($("head").find("link[href='" + cssUrl + "']").length == 0) {
        var headElems = document.getElementsByTagName("head");
        var style = document.createElement("link");
        style.type = "text/css";
        style.rel = "stylesheet";
        style.href = cssUrl;
        headElems[headElems.length - 1].appendChild(style);
      } // else, already added
    }
  };

  /** For debug logging. */
  var log = function(msg, err) {
    var logPrefix = "[onlyoffice] ";
    if (typeof console != "undefined" && typeof console.log != "undefined") {
      var isoTime = " -- " + new Date().toISOString();
      var msgLine = msg;
      if (err) {
        msgLine += ". Error: ";
        if (err.name || err.message) {
          if (err.name) {
            msgLine += "[" + err.name + "] ";
          }
          if (err.message) {
            msgLine += err.message;
          }
        } else {
          msgLine += (typeof err === "string" ? err : JSON.stringify(err)
              + (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
        }

        console.log(logPrefix + msgLine + isoTime);
        if (typeof err.stack != "undefined") {
          console.log(err.stack);
        }
      } else {
        if (err !== null && typeof err !== "undefined") {
          msgLine += ". Error: '" + err + "'";
        }
        console.log(logPrefix + msgLine + isoTime);
      }
    }
  };

  var tryParseJson = function(message) {
    var src = message.data ? message.data : (message.error ? message.error : message.failure);
    if (src) {
      try {
        if (typeof src === "string" && (src.startsWith("{") || src.startsWith("["))) {
          return JSON.parse(src);
        }
      } catch (e) {
        log("Error parsing '" + src + "' as JSON: " + e, e);
      }
    }
    return src;
  };

  var messages = {}; // should be initialized by Editor.initMessages()

  var message = function(key) {
    var m = messages[key];
    return m ? m : key;
  };

  var adjustWidth = function() {
      var $editorBar = $("#editor-top-bar");
        $editorBar.ready(function(){
          setTimeout(function(){
            if ($editorBar[0].scrollHeight > $editorBar[0].offsetHeight) {
              var $commentBox = $editorBar.find(".editors-comment a");
              var comment = $commentBox.html();
              if(comment.length >= 15) {
                comment = comment.slice(0, -10) + "...";
                $commentBox.html(comment);
                adjustWidth();
              }
            } else {
                setTimeout(() => {
                  $("#editor-top-bar-loader").hide();
              }, 10);
            }
          }, 10);
        }); 
    };
  
  var formatDate = function(date) {
    var yyyy = date.getFullYear();
    var dd = date.getDate();
    var mm = (date.getMonth() + 1);
    
    if (dd < 10)
        dd = "0" + dd;
    if (mm < 10)
        mm = "0" + mm;

    var cur_day = dd + "." + mm + "." + yyyy;

    var hours = date.getHours()
    var minutes = date.getMinutes()

    if (hours < 10)
        hours = "0" + hours;

    if (minutes < 10)
        minutes = "0" + minutes;

    return cur_day + " " + hours + ":" + minutes;
  }

  // ******** REST services ********
  var prefixUrl = pageBaseUrl(location);

  /**
   * Editor core class.
   */
  function Editor() {

    // Constants:
    var DOCUMENT_SAVED = "DOCUMENT_SAVED";
    var DOCUMENT_CHANGED = "DOCUMENT_CHANGED";
    var DOCUMENT_DELETED = "DOCUMENT_DELETED";
    var DOCUMENT_VERSION = "DOCUMENT_VERSION";
    var DOCUMENT_USERSAVED = "DOCUMENT_USERSAVED";
    var DOCUMENT_TITLE_UPDATED = "DOCUMENT_TITLE_UPDATED";
    var DOCUMENT_LINK = "DOCUMENT_LINK";
    var EDITOR_CLOSED = "EDITOR_CLOSED";

    // Events that are dispatched to redux as actions
    var dispatchableEvents = [ DOCUMENT_SAVED, DOCUMENT_CHANGED, DOCUMENT_DELETED, DOCUMENT_VERSION, DOCUMENT_TITLE_UPDATED ];

    // CometD transport bus
    var cometd, cometdContext;

    // Current config (actual for editor page only)
    var currentConfig;

    // Indicate current changes are collected by the document server (only for editor page)
    var changesSaved = true;

    // Indicates the last change is made by current user or another one.
    var currentUserChanges = false;

    // Current user ID
    var currentUserId;

    // Current document ID (actual for Documents explorer)
    var explorerDocId;

    // Used to setup changesTimer on the editor page
    var changesTimer;

    // A time to issue document version save if no changes were done, but editor is open
    var autosaveTimer;

    // The editor window is used while creating a new document.
    var editorWindow;

    // Redux store for dispatching document updates inside the app
    var store = redux.createStore(function(state, action) {
      if (dispatchableEvents.includes(action.type)) {
        // TODO do we need merge with previous state as Redux assumes?
        return action;
      } else if (action.type.startsWith("@@redux/INIT")) {
        // it's OK (at least for initialization)
      } else {
        log("Unknown action type:" + action.type);
      }
      return state;
    });
    var subscribedDocuments = {};

    /**
     * Subscribes on a document updates using cometd. Dispatches events to the redux store.
     */
    var subscribeDocument = function(docId) {
      // Use only one channel for one document
      if (subscribedDocuments.docId) {
        return;
      }
      var subscription = cometd.subscribe("/eXo/Application/Onlyoffice/editor/" + docId, function(message) {
        // Channel message handler
        var result = tryParseJson(message);
        if (dispatchableEvents.includes(result.type)) {
          store.dispatch(result);
        }
      }, cometdContext, function(subscribeReply) {
        // Subscription status callback
        if (subscribeReply.successful) {
          // The server successfully subscribed this client to the channel.
          log("Document updates subscribed successfully: " + JSON.stringify(subscribeReply));
          subscribedDocuments.docId = subscription;
        } else {
          var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason
              : "Undefined");
          log("Document updates subscription failed for " + docId, err);
        }
      });
    };

    var unsubscribeDocument = function(docId) {
      var subscription = subscribedDocuments.docId;
      if (subscription) {
        cometd.unsubscribe(subscription, {}, function(unsubscribeReply) {
          if (unsubscribeReply.successful) {
            // The server successfully unsubscribed this client to the channel.
            log("Document updates unsubscribed successfully for: " + docId);
            delete subscribedDocuments.docId;
          } else {
            var err = unsubscribeReply.error ? unsubscribeReply.error
                : (unsubscribeReply.failure ? unsubscribeReply.failure.reason : "Undefined");
            log("Document updates unsubscription failed for " + docId, err);
          }
        });
      }
    };

    var publishDocument = function(docId, data) {
      var deferred = $.Deferred();
      cometd.publish("/eXo/Application/Onlyoffice/editor/" + docId, data, cometdContext, function(publishReply) {
        // Publication status callback
        if (publishReply.successful) {
          deferred.resolve();
          // The server successfully subscribed this client to the channel.
          log("Document update published successfully: " + JSON.stringify(publishReply));
        } else {
          deferred.reject();
          var err = publishReply.error ? publishReply.error : (publishReply.failure ? publishReply.failure.reason : "Undefined");
          log("Document updates publication failed for " + docId, err);
        }
      });
      return deferred;
    };

    var onError = function(event) {
      log("ONLYOFFICE Document Editor reports an error: " + JSON.stringify(event));
    };

    var onReady = function() {
      log("ONLYOFFICE Document Editor is ready");
    };

    var onBack = function() {
      log("ONLYOFFICE Document Editor on Back");
    };

    var onDocumentStateChange = function(event) {
      log("documentChange: " + JSON.stringify(event));
      if (event.data) {
        log("ONLYOFFICE The document changed");
        changesSaved = false;
        // Document changed locally, soon it will be sent to Document Server.
        // FYI We may get prepared here for a soon call of downloadAs().
      } else {
        // We use this check to avoid publishing updates from other users
        // and publishing when user hasn't made any changes yet (opened editor)

        // New autosave should be done by a client that makes changes
        if (autosaveTimer && !currentUserChanges) {
          log("Reset autosave timer...");
          clearTimeout(autosaveTimer);
          autosaveTimer = null;
        }

        if (changesTimer) {
          log("Reset changes timer...");
          clearTimeout(changesTimer);
          changesTimer = null;
        }

        if (!changesSaved) {
          log("ONLYOFFICE Changes are collected on document editing service");
          changesSaved = true;
          currentUserChanges = true;
          changesTimer = setTimeout(function() {
            log("Getting document link after a timeout...");
            saveDocumentLink();
            if (autosaveTimer) {
              clearTimeout(autosaveTimer);
            }
            autosaveTimer = setTimeout(function() {
              log("It's time to make autosave of document version...");
              // Publish autosave version for download
              downloadVersion();
              currentUserChanges = false;
            }, 600000); // 10min for autosave
          }, 20000); // 20 sec to save the download link

          // We are a editor page here: publish that the doc was changed by current user
          publishDocument(currentConfig.docId, {
            "type" : DOCUMENT_CHANGED,
            "userId" : currentUserId,
            "clientId" : clientId,
            "key" : currentConfig.document.key
          });
        } else {
          currentUserChanges = false;
        }
      }
    };

    var downloadVersion = function() {
      if (currentConfig) {
        publishDocument(currentConfig.docId, {
          "type" : DOCUMENT_VERSION,
          "userId" : currentUserId,
          "clientId" : clientId,
          "key" : currentConfig.document.key
        });
      } else {
        log("WARN: Editor configuration not found. Cannot download document version.");
      }
    };

    var saveDocumentLink = function() {
      if (currentConfig) {
        publishDocument(currentConfig.docId, {
          "type" : DOCUMENT_LINK,
          "userId" : currentUserId,
          "clientId" : clientId,
          "key" : currentConfig.document.key
        });
      } else {
        log("WARN: Editor configuration not found. Cannot save document document.");
      }
    };

    var initBar = function(config) {
      var $bar = UI.initBar(config);
      // Edit title
      if(config.editorPage.renameAllowed) {
        $bar.find(".editable-title").editable({
          onChange : function(event) {
            var newTitle = event.newValue;
            var oldTitle = currentConfig.document.title;
            if (oldTitle.includes(".")) {
              var extension = oldTitle.substr(oldTitle.lastIndexOf("."));
              if (!newTitle.endsWith(extension)) {
                newTitle += extension;
              }
            }
            publishDocument(currentConfig.docId, {
              "type" : DOCUMENT_TITLE_UPDATED,
              "userId" : currentUserId,
              "clientId" : clientId,
              "title" : newTitle,
              "workspace" : currentConfig.workspace
            });
          }
        });
      }
      $bar.find("#save-btn").on("click", function() {
        var comment = $bar.find("#comment-box").val();
        publishDocument(currentConfig.docId, {
          "type" : DOCUMENT_USERSAVED,
          "userId" : currentUserId,
          "clientId" : clientId,
          "key" : currentConfig.document.key,
          "comment" : comment
        });
        $bar.find("#comment-box").val('');
        // Reset all timers after forcesaving
        if (autosaveTimer) {
          log("Reset autosave timer...");
          clearTimeout(autosaveTimer);
          autosaveTimer = null;
        }
        if (changesTimer) {
          log("Reset changes timer...");
          clearTimeout(changesTimer);
          changesTimer = null;
        }
      });

      $bar.find(".close-btn").on("click", function() {
        window.close();
      });
    };

    /**
     * Create an editor configuration (for use to create the editor client UI).
     */
    var create = function(config) {
      var process = $.Deferred();
      if (config) {
        // prepare config
        log("ONLYOFFICE editor config: " + JSON.stringify(config));

        // customize Onlyoffice JS config
        config.type = "desktop";
        config.height = "100%";
        config.width = "100%";
        config.embedded = {
          fullscreenUrl : config.document.url,
          saveUrl : config.document.url,
          embedUrl : config.document.url,
          shareUrl : config.document.url,
          toolbarDocked : "top"
        };
        config.events = {
          "onDocumentStateChange" : onDocumentStateChange,
          "onError" : onError,
          "onReady" : onReady,
          "onBack" : onBack
        };
        config.editorConfig.customization = {
          "chat" : false,
          "compactToolbar" : true,
          "goback" : {
            "blank" : true,
            "text" : "Go to Document",
            "url" : config.explorerUrl
          },
          "help" : true,
          "logo" : {
            "image" : prefixUrl + "/onlyoffice/images/exo-icone.png",
            "imageEmbedded" : prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "url" : prefixUrl + "/portal"
          },
          "customer" : {
            "info" : "eXo Platform",
            "logo" : prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "mail" : "support@exoplatform.com",
            "name" : "Support",
            "www" : "exoplatform.com"
          }
        };
        config.editorConfig.plugins = {
          "autostart" : [],
          "pluginsData" : []
        };

        log("ONLYOFFICE editor config: " + JSON.stringify(config));

        if((typeof DocsAPI === "undefined") || (typeof DocsAPI.DocEditor === "undefined")) {
          log("ERROR: ONLYOFFICE script load timeout: " + config.documentserverJsUrl);
          process.reject("ONLYOFFICE script load timeout. Ensure Document Server is running and accessible.");
        } else {
          process.resolve(config);
        }
      } else {
        process.reject("Editor config not found");
      }
      return process.promise();
    };
    this.create = create;

    this.init = function(userId, cometdConf, userMessages) {
      if (userId == currentUserId) {
        log("Already initialized user: " + userId);
      } else if (userId) {
        currentUserId = userId;
        log("Initialize user: " + userId);
        if (userMessages) {
          messages = userMessages;
        }
        if (cometdConf) {
          cCometD.configure({
            "url" : prefixUrl + cometdConf.path,
            "exoId" : userId,
            "exoToken" : cometdConf.token,
            "maxNetworkDelay" : 30000,
            "connectTimeout" : 60000
          });
          cometdContext = {
            "exoContainerName" : cometdConf.containerName
          };
          cometd = cCometD;
        }
      } else {
        log("Cannot initialize user: " + userId);
      }
    };

    /**
     * Initialize an editor page in current browser window.
     */
    this.initEditor = function(config) {
      initBar(config);
      log("Initialize editor for document: " + config.docId);
      window.document.title = config.document.title + " - " + window.document.title;
      UI.initEditor();

      create(config).done(function(localConfig) {
        if (localConfig) {
          currentConfig = localConfig;

          store.subscribe(function() {
            var state = store.getState();
            if (state.type === DOCUMENT_DELETED) {
              UI.showError(message("ErrorTitle"), message("ErrorFileDeletedEditor"));
            }
            if(state.type === DOCUMENT_SAVED) {
              UI.updateBar(state.displayName, state.comment);
              if(state.comment){
                currentConfig.editorPage.comment = state.comment;
              }
              if(state.userId === currentUserId){
                currentUserChanges = false;
              }
            }
            if(state.type === DOCUMENT_TITLE_UPDATED) {
              console.log("Title updated");
              var oldTitle = currentConfig.document.title;
              currentConfig.document.title = state.title;
              window.document.title = window.document.title.replace(oldTitle, state.title);
              $("#editor-top-bar").find(".editable-title").text(state.title);
              adjustWidth();
            }
          });

          // Establish a Comet/WebSocket channel from this point.
          // A new editor page will join the channel and notify when the doc will be saved
          // so we'll refresh this explorer view to reflect the edited content.
          subscribeDocument(currentConfig.docId);

          // We are a editor oage here: publish that the doc was changed by current user

          window.addEventListener("beforeunload", function() {
            UI.closeEditor(); // try this first, then in unload
          });

          window.addEventListener("unload", function() {
            UI.closeEditor();
            // We need to save current changes when user closes the editor
            if (currentConfig) {
              publishDocument(currentConfig.docId, {
                "type" : EDITOR_CLOSED,
                "userId" : currentUserId,
                "key" : currentConfig.document.key,
                "changes" : currentUserChanges
              });
            }
          });

          $(function() {
            try {
              UI.create(currentConfig);
            } catch (e) {
              log("Error initializing Onlyoffice client UI " + e, e);
            }
          });
        } else {
          log("ERROR: editor config not defined: " + localConfig);
          UI.showError(message("ErrorTitle"), message("ErrorConfigNotDefined"));
        }
      }).fail(function(error) {
        log("ERROR: editor config creation failed : " + error);
        UI.showError(message("ErrorTitle"), message("ErrorCreateConfig"));
      });
    };

    /**
     * Initializes a file activity in the activity stream.
     */
    this.initActivity = function(docId, editorLink, activityId) {
      log("Initialize activity " + activityId + " with document: " + docId);
      // Listen to document updates
      store.subscribe(function() {
        var state = store.getState();
        if (state.type === DOCUMENT_SAVED && state.docId === docId) {
          UI.addRefreshBannerActivity(activityId);
        }
      });
      subscribeDocument(docId);
      if (editorLink) {
        UI.addEditorButtonToActivity(editorLink, activityId);
      }
    };

    /**
     * Initializes a document preview (from the activity stream).
     */
    this.initPreview = function(docId, editorLink, clickSelector) {
      $(clickSelector).click(function() {
        log("Initialize preview " + clickSelector + " of document: " + docId);
        // We set timeout here to avoid the case when the element is rendered but is going to be updated soon
        setTimeout(function() {
          store.subscribe(function() {
            var state = store.getState();
            if (state.type === DOCUMENT_SAVED && state.docId === docId) {
              UI.addRefreshBannerPDF();
            }
          });
        }, 100);
        subscribeDocument(docId);
      });
      if (editorLink) {
        UI.addEditorButtonToPreview(editorLink, clickSelector);
      }
    };

    /**
     * Initializes JCRExplorer when a document is displayed.
     */
    this.initExplorer = function(docId, editorLink) {
      log("Initialize explorer with document: " + docId);
      // Listen document updated
      store.subscribe(function() {
        var state = store.getState();
        if (state.type === DOCUMENT_SAVED) {
          if (state.userId === currentUserId) {
            UI.refreshPDFPreview();
          } else {
            UI.addRefreshBannerPDF();
          }
        }
        if (state.type === DOCUMENT_DELETED) {
          UI.showError(message("ErrorTitle"), message("ErrorFileDeletedECMS"));
        }
      });
      if (docId != explorerDocId) {
        // We need unsubscribe from previous doc
        if (explorerDocId) {
          unsubscribeDocument(explorerDocId);
        }
        subscribeDocument(docId);
        explorerDocId = docId;
      }
      UI.addEditorButtonToExplorer(editorLink);

    };

    /**
     * Sets the onClick listener for Create Document button (used in creating a new document)
     */
    this.initNewDocument = function() {
      $("#UINewDocumentForm .newDocumentButton").on('click', function() {
        editorWindow = window.open();
      });
    };

    /**
     * Initializes the editor in the editorWindow. (used in creating a new document)
     */
    this.initEditorPage = function(link) {
      if (editorWindow != null) {
        if (link != null) {
          editorWindow.location = link;
        } else {
          editorWindow.close();
          editorWindow = null;
        }
      }
    };

    this.showInfo = function(title, text) {
      UI.showInfo(title, text);
    };

    this.showError = function(title, text) {
      UI.showError(title, text);
    };
  }

  /**
   * Online Editor WebUI integration.
   */
  function UI() {
    var NOTICE_WIDTH = "380px";

    var docEditor;

    var notification;

    /**
     * Returns the html markup of the 'Edit Online' button.
     */
    var getEditorButton = function(editorLink) {
      return "<li class='hidden-tabletL'><a href='" + editorLink + "' target='_blank'>"
          + "<i class='uiIconEcmsOnlyofficeOpen uiIconEcmsLightGray uiIconEdit'></i>" + message("EditButtonTitle") + "</a></li>";
    };

    var getNoPreviewEditorButton = function(editorLink) {
      return "<a class='btn editOnlineBtn hidden-tabletL' href='#' onclick='javascript:window.open(\"" + editorLink + "\");'>"
          + "<i class='uiIconEcmsOnlyofficeOpen uiIconEcmsLightGray uiIconEdit'></i>" + message("EditButtonTitle") + "</a>";
    };

    /**
     * Returns the html markup of the refresh banner;
     */
    var getRefreshBanner = function() {
      return "<div class='documentRefreshBanner'><div class='refreshBannerContent'>" + message("UpdateBannerTitle")
          + "<span class='refreshBannerLink'>" + message("ReloadButtonTitle") + "</span></div></div>";
    };

    /**
     * Adds the 'Edit Online' button to No-preview screen (from the activity stream) when it's loaded.
     */
    var tryAddEditorButtonNoPreview = function(editorLink, attempts, delay) {
      var $elem = $("#documentPreviewContainer .navigationContainer.noPreview");
      if ($elem.length == 0 || !$elem.is(":visible")) {
        if (attempts > 0) {
          setTimeout(function() {
            tryAddEditorButtonNoPreview(editorLink, attempts - 1, delay);
          }, delay);
        } else {
          log("Cannot find .noPreview element");
        }
      } else if ($elem.find("a.editOnlineBtn").length == 0) {
        var $detailContainer = $elem.find(".detailContainer");
        var $downloadBtn = $detailContainer.find(".uiIconDownload").closest("a.btn");
        if ($downloadBtn.length != 0) {
          $downloadBtn.after(getNoPreviewEditorButton(editorLink));
        } else {
          $detailContainer.append(getNoPreviewEditorButton(editorLink));
        }
      }
    };
    /**
     * Adds the 'Edit Online' button to a preview (from the activity stream) when it's loaded.
     */
    var tryAddEditorButtonToPreview = function(editorLink, attempts, delay) {
      var $elem = $("#uiDocumentPreview .previewBtn");
      if ($elem.length == 0 || !$elem.is(":visible")) {
        if (attempts > 0) {
          setTimeout(function() {
            tryAddEditorButtonToPreview(editorLink, attempts - 1, delay);
          }, delay);
        } else {
          log("Cannot find element " + $elem);
        }
      } else {
        $elem.append("<div class='onlyOfficeEditBtn'>" + getEditorButton(editorLink) + "</div>");
      }
    };

    // Use this in on-close window handler.
    var saveAndDestroy = function() {
      if (docEditor) {
        var theEditor = docEditor;
        docEditor = null;
        try {
          theEditor.processSaveResult(true);
          theEditor.destroyEditor();
        } catch (e) {
          log("Error saving and destroying ONLYOFFICE editor", e);
        }
      }
    };

    /**
     * Refreshes an activity preview by updating preview picture.
     */
    var refreshActivityPreview = function(activityId, $banner) {
      $banner.find(".refreshBannerContent")
          .append("<div class='loading'><i class='uiLoadingIconSmall uiIconEcmsGray'></i></div>");
      var $refreshLink = $banner.find(".refreshBannerLink");
      $refreshLink.addClass("disabled");
      $refreshLink.on('click', function() {
        return false;
      });
      $refreshLink.attr("href", "#");
      var $img = $("#Preview" + activityId + "-0 #MediaContent" + activityId + "-0 img");
      if ($img.length !== 0) {
        var src = $img.attr("src");
        if (src.includes("version=")) {
          src = src.substring(0, src.indexOf("version="));
        }
        var timestamp = new Date().getTime();

        src += "version=oview_" + timestamp;
        src += "&lastModified=" + timestamp;

        $img.on('load', function() {
          $banner.remove();
        });
        $img.attr("src", src);

        // Hide banner when there no preview image
        var $mediaContent = $("#Preview" + activityId + "-0 #MediaContent" + activityId + "-0");
        var observer = new MutationObserver(function(mutations) {
          mutations.forEach(function(mutation) {
            if (mutation.attributeName === "class") {
              var attributeValue = $(mutation.target).prop(mutation.attributeName);
              if (attributeValue.includes("NoPreview")) {
                log("Cannot load preview for activity " + activityId + ". Hiding refresh banner");
                $banner.remove();
              }
            }
          });
        });
        observer.observe($mediaContent[0], {
          attributes : true
        });
      }
    };

    /**
     * Refreshes a document preview (PDF) by reloading viewer.js script.
     */
    var refreshPDFPreview = function() {
      var $banner = $(".document-preview-content-file #toolbarContainer .documentRefreshBanner");
      if ($banner.length !== 0) {
        $banner.remove();
      }
      setTimeout(function() {
        var $vieverScript = $(".document-preview-content-file script[src$='/viewer.js']")
        var viewerSrc = $vieverScript.attr("src");
        $vieverScript.remove();
        $(".document-preview-content-file").append("<script src='" + viewerSrc + "'></script>");
      }, 250); // XXX we need wait for office preview server generate a new preview
    };

    this.refreshPDFPreview = refreshPDFPreview;

    /**
     * Init editor page UI.
     */
    this.initEditor = function() {
      // We don't need this (thx to OnlyofficeEditorLifecycle)
      // $("#NavigationPortlet").remove();
      // But we may need this for some cases of first page loading
      $("#LeftNavigation").parent(".LeftNavigationTDContainer").remove();
      // Specific styles to add to hide/fix portal layout
      // We prefer to add to an element with already applied Platform skin style
      var $body = $("#UIWorkingWorkspace");
      if ($body.length == 0) {
        $body = $("#UIPortalApplication");
        if ($body.length == 0) {
          // Otherwise, use the whole page
          $body = $("body");
        }
      }
      $body.addClass("onlyofficeEditorBody");
    };
    
    this.updateBar = function(changer, comment) {
      var $bar = $("#editor-top-bar");
      var $commentBox = $bar.find(".editors-comment a");
      $commentBox.attr("data-original-title", comment);
      $commentBox.empty();
      if(comment){
        $commentBox.append("\"" + comment + "\"");
      }
      var $lastEditedElem = $bar.find(".last-edited");
      $lastEditedElem.empty();
      $lastEditedElem.append("Last edited by " + changer + " " + formatDate(new Date()));
      adjustWidth();
    };

    this.initBar = function(config) {
      var drive = config.editorPage.displayPath.split(':')[0];
      var folders = config.editorPage.displayPath.split(':')[1].split('/');
      var title = folders.pop();
      var $bar = $("#editor-top-bar");
      if(config.editorPage.renameAllowed){
        $bar.find("a[rel=tooltip]").tooltip();
      } else {
        $bar.find("a[rel=tooltip]").not(".document-title a[rel=tooltip]").tooltip();
      }
      if(!config.activity) {
        $bar.find("#comment-box").prop("disabled", true);
      }
      var $pathElem = $bar.find(".document-path");
      $pathElem.append(drive + " : ");
      $pathElem.append("<span class='folder'>" + folders[0] + "</span>" + " <i class='uiIconArrowRight'></i> ");
     
      var $titleElem = $bar.find(".document-title a");
      $titleElem.append("<span class='editable-title'>" + title + "</span>");

      var $lastEditedElem = $bar.find(".last-edited");
      $lastEditedElem.append("Last edited by " + config.editorPage.lastModifier + " " + config.editorPage.lastModified);
      if(config.editorPage.comment){
        var $comment = $bar.find(".editors-comment a");
        $comment.append("\"" + config.editorPage.comment + "\"");
        $comment.attr("data-original-title", config.editorPage.comment);
      }

      var $saveBtn = $bar.find("#save-btn .uiIconSave");
      $saveBtn.on("click", function(){
        $saveBtn.css("color", "gray");
        setTimeout(function(){
          $saveBtn.css("color", "")
        }, 300)
      });
      setTimeout(function() { 
        adjustWidth();
      }, 1500);
      return $bar;
    };

    this.isEditorLoaded = function() {
      return $("#UIPage .onlyofficeContainer").length > 0;
    };

    /**
     * Use it when user close the page, to notify in the channel doc is closed.
     */
    this.closeEditor = function() {
      saveAndDestroy();
    };

    /**
     * Create an editor client UI on current page.
     */
    this.create = function(localConfig) {
      var $editorPage = $("#OnlyofficeEditorPage");
      if ($editorPage.length > 0) {
        if (!docEditor) {
          // show loading while upload to editor - it is already added by WebUI side
          var $container = $editorPage.find(".onlyofficeContainer");

          // create and start editor (this also will re-use an existing editor config from the server)
          docEditor = new DocsAPI.DocEditor("onlyoffice", localConfig);
          console.log('>>>> ' + JSON.stringify(localConfig))
          // show editor
          $container.find("#editor-top-bar").show("blind");
          $container.find("#editor-top-bar-loader").show("blind");
          $container.find(".editor").show("blind");
          $container.find(".loading").hide("blind");
        } else {
          log("WARN: Editor client already initialized");
        }
      } else {
        log("WARN: Editor element not found");
      }
    };

    /**
     * Ads the 'Edit Online' button to the JCRExplorer when a document is displayed.
     */
    this.addEditorButtonToExplorer = function(editorLink) {
      var $button = $("#UIJCRExplorer #uiActionsBarContainer i.uiIconEcmsOnlyofficeOpen");
      $button.addClass("uiIconEdit");
      $button.closest("li").addClass("hidden-tabletL");
      var $noPreviewContainer = $("#UIJCRExplorer .navigationContainer.noPreview");
      if (editorLink != null && $noPreviewContainer.length != 0) {
        var $detailContainer = $noPreviewContainer.find(".detailContainer");
        var $downloadBtn = $detailContainer.find(".uiIconDownload").closest("a.btn");
        if ($downloadBtn.length != 0) {
          $downloadBtn.after(getNoPreviewEditorButton(editorLink));
        } else {
          $detailContainer.append(getNoPreviewEditorButton(editorLink));
        }
      }
    };

    /**
     * Ads the 'Edit Online' button to an activity in the activity stream.
     */
    this.addEditorButtonToActivity = function(editorLink, activityId) {
      $("#activityContainer" + activityId).find("div[id^='ActivityContextBox'] > .actionBar .statusAction.pull-left").append(
          getEditorButton(editorLink));
    };

    /**
     * Ads the 'Edit Online' button to a preview (opened from the activity stream).
     */
    this.addEditorButtonToPreview = function(editorLink, clickSelector) {
      $(clickSelector).click(function() {
        // We set timeout here to avoid the case when the element is rendered but is going to be updated soon
        setTimeout(function() {
          tryAddEditorButtonToPreview(editorLink, 100, 100);
          // We need wait for about 2min when doc cannot generate its preview
          tryAddEditorButtonNoPreview(editorLink, 600, 250);
        }, 100);
      });
    };

    /**
     * Ads the refresh banner to an activity in the activity stream.
     */
    this.addRefreshBannerActivity = function(activityId) {
      var $previewParent = $("#Preview" + activityId + "-0").parent();
      // If there is no preview
      if ($previewParent.length === 0 || $previewParent.find(".mediaContent.docTypeContent.NoPreview").length !== 0) {
        return;
      }
      // If the activity contains only one preview
      if ($previewParent.find("#Preview" + activityId + "-1").length === 0) {
        if ($previewParent.find(".documentRefreshBanner").length === 0) {
          $previewParent.prepend(getRefreshBanner());
          var $banner = $previewParent.find(".documentRefreshBanner");
          $(".documentRefreshBanner .refreshBannerLink").click(function() {
            refreshActivityPreview(activityId, $banner);
          });
        }
        log("Activity document: " + activityId + " has been updated");
      }
    };

    /**
     * Ads the refresh banner to the PDF document preview.
     */
    this.addRefreshBannerPDF = function() {
      var $toolbarContainer = $(".document-preview-content-file #toolbarContainer");
      if ($toolbarContainer.length !== 0 && $toolbarContainer.find(".documentRefreshBanner").length === 0) {
        $toolbarContainer.append(getRefreshBanner());
        $(".documentRefreshBanner .refreshBannerLink").click(function() {
          refreshPDFPreview();
        });
      }
    };

    /**
     * Show notice to user. Options support "icon" class, "hide", "closer" and "nonblock" features.
     */
    this.showNotice = function(type, title, text, options) {
      var noticeOptions = {
        title : title,
        text : text,
        type : type,
        icon : "picon " + (options ? options.icon : ""),
        hide : options && typeof options.hide != "undefined" ? options.hide : false,
        closer : options && typeof options.closer != "undefined" ? options.closer : true,
        sticker : false,
        opacity : .9,
        addclass : 'onlyoffice-notification',
        shadow : true,
        width : options && options.width ? options.width : NOTICE_WIDTH,
        nonblock : options && typeof options.nonblock != "undefined" ? options.nonblock : false,
        nonblock_opacity : .25,
        after_init : function(pnotify) {
          if (options && typeof options.onInit == "function") {
            options.onInit(pnotify);
          }
        }
      };
      if (notification) {
        notification.remove();
      }

      notification = $.pnotify(noticeOptions);
      return notification;
    };

    /**
     * Show error notice to user. Error will stick until an user close it.
     */
    this.showError = function(title, text, onInit) {
      return UI.showNotice("error", title, text, {
        icon : "picon-dialog-error",
        hide : false,
        delay : 0,
        onInit : onInit
      });
    };

    /**
     * Show info notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showInfo = function(title, text, onInit) {
      return UI.showNotice("info", title, text, {
        hide : true,
        delay : 8000,
        icon : "picon-dialog-information",
        onInit : onInit
      });
    };

    /**
     * Show warning notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showWarn = function(title, text, onInit) {
      return UI.showNotice("exclamation", title, text, {
        hide : false,
        delay : 30000,
        icon : "picon-dialog-warning",
        onInit : onInit
      });
    };
  }

  var editor = new Editor();
  var UI = new UI();

  $(function() {
    try {
      // load required styles (it didn't work right via gatein-resources.xml in PLF 5.0)
      loadStyle("/onlyoffice/skin/jquery-ui.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.icons.css");
      loadStyle("/onlyoffice/skin/onlyoffice.css");

      // configure Pnotify
      $.pnotify.defaults.styling = "jqueryui";
      // use jQuery UI css
      $.pnotify.defaults.history = false;
      // no history roller in the right corner
    } catch (e) {
      log("Error configuring Onlyoffice Editor style.", e);
    }
  });
  return editor;
})($, cCometD, Redux);