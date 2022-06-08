/**
 * Onlyoffice Editor client.
 */
(function($, cCometD, redux, editorbuttons, editorsupport) {
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
   * Universal client ID for use in logging, services connectivity and related
   * cases.
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
          msgLine += (typeof err === "string" ? err : JSON.stringify(err) +
            (err.toString && typeof err.toString === "function" ? "; " + err.toString() : ""));
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

  var getViewerConfig = function(workspace, fileId) {
    return $.get({
      type: "GET",
      url: prefixUrl + "/portal/rest/onlyoffice/editor/viewer/" + workspace + "/" + fileId,
    });
  };

  const DESKTOP_MODE = "desktop";
  let EMBEDDED_MODE = "";
  if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
    EMBEDDED_MODE = "mobile";
  } else {
    EMBEDDED_MODE = "embedded";
  }


  /**
   * Editor core class.
   */
  function Editor() {

    // Constants:
    const DOCUMENT_SAVED = "DOCUMENT_SAVED";
    const DOCUMENT_CONTENT_UPDATED = "DOCUMENT_CONTENT_UPDATED";
    const DOCUMENT_CHANGED = "DOCUMENT_CHANGED";
    const DOCUMENT_DELETED = "DOCUMENT_DELETED";
    const DOCUMENT_VERSION = "DOCUMENT_VERSION";
    const DOCUMENT_USERSAVED = "DOCUMENT_USERSAVED";
    const DOCUMENT_TITLE_UPDATED = "DOCUMENT_TITLE_UPDATED";
    const DOCUMENT_LINK = "DOCUMENT_LINK";
    const EDITOR_CLOSED = "EDITOR_CLOSED";
    const ONLYOFFICE = "onlyoffice";
    // Events that are dispatched to redux as actions
    var dispatchableEvents = [DOCUMENT_SAVED, DOCUMENT_CONTENT_UPDATED,  DOCUMENT_CHANGED, DOCUMENT_DELETED, DOCUMENT_VERSION, DOCUMENT_TITLE_UPDATED];

    // CometD transport bus
    var cometd, cometdContext;

    // Current config (actual for editor page only)
    var currentConfig;

    // Indicate current changes are collected by the document server (only for
    // editor page)
    var changesSaved = true;

    // Indicates the last change is made by current user or another one.
    var currentUserChanges = false;

    // Current user ID
    var currentUserId;

    // Current document ID (actual for Documents explorer)
    var explorerDocId;

    // Used to setup changesTimer on the editor page
    var changesTimer;

    // A time to issue document version save if no changes were done, but editor
    // is open
    var autosaveTimer;

    // The editor window is used while creating a new document.
    var editorWindow;

    // The page index to load in the first iteration
    var pageToLoad = 1;

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
     * Subscribes on a document updates using cometd. Dispatches events to the
     * redux store.
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
          var err = subscribeReply.error ? subscribeReply.error : (subscribeReply.failure ? subscribeReply.failure.reason :
            "Undefined");
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
            var err = unsubscribeReply.error ? unsubscribeReply.error :
              (unsubscribeReply.failure ? unsubscribeReply.failure.reason : "Undefined");
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
          editorsupport.notifyActive();
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

          // We are a editor page here: publish that the doc was changed by
          // current user
          publishDocument(currentConfig.docId, {
            "type": DOCUMENT_CHANGED,
            "userId": currentUserId,
            "clientId": clientId,
            "key": currentConfig.document.key
          });
        } else {
          currentUserChanges = false;
        }
      }
    };

    var downloadVersion = function() {
      if (currentConfig) {
        publishDocument(currentConfig.docId, {
          "type": DOCUMENT_VERSION,
          "userId": currentUserId,
          "clientId": clientId,
          "key": currentConfig.document.key
        });
      } else {
        log("WARN: Editor configuration not found. Cannot download document version.");
      }
    };

    var saveDocumentLink = function() {
      if (currentConfig) {
        publishDocument(currentConfig.docId, {
          "type": DOCUMENT_LINK,
          "userId": currentUserId,
          "clientId": clientId,
          "key": currentConfig.document.key
        });
      } else {
        log("WARN: Editor configuration not found. Cannot save document document.");
      }
    };

    var initBar = function(config) {
      var $bar = UI.initBar(config);
      // Edit title
      if (config.editorPage.renameAllowed) {
        $bar.find(".editable-title").editable({
          onChange: function(event) {
            var newTitle = event.newValue;
            var oldTitle = currentConfig.document.title;
            if (oldTitle.includes(".")) {
              var extension = oldTitle.substr(oldTitle.lastIndexOf("."));
              if (!newTitle.endsWith(extension)) {
                newTitle += extension;
              }
            }
            publishDocument(currentConfig.docId, {
              "type": DOCUMENT_TITLE_UPDATED,
              "userId": currentUserId,
              "clientId": clientId,
              "title": newTitle,
              "workspace": currentConfig.workspace
            });
          }
        });
      }
      $bar.find("#save-btn").on("click", function() {
        pageToLoad = 1;
        var comment = $bar.find("#comment-box").val();
        if (comment.length > 510) {
          UI.errorSave();
          return;
        }
        publishDocument(currentConfig.docId, {
          "type": DOCUMENT_USERSAVED,
          "userId": currentUserId,
          "clientId": clientId,
          "key": currentConfig.document.key,
          "comment": comment
        }).done(function() {
          UI.alertSave();
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

    var init = function(userId, cometdConf, userMessages) {
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
            "url": prefixUrl + cometdConf.path,
            "exoId": userId,
            "exoToken": cometdConf.token,
            "maxNetworkDelay": 30000,
            "connectTimeout": 60000
          });
          cometdContext = {
            "exoContainerName": cometdConf.containerName
          };
          cometd = cCometD;
        }
      } else {
        log("Cannot initialize user: " + userId);
      }
    };
    
    /**
     * Resolves process with config after DocsAPI script is loaded.
     */
    var waitDocsAPI = function(process, config, retries, timeout) {
      setTimeout(function() {
        if ((typeof DocsAPI !== "undefined") && (typeof DocsAPI.DocEditor !== "undefined")) {
          process.resolve(config);
        } else if (retries > 0) {
          waitDocsAPI(process, config, --retries, timeout);
        } else {
          log("ERROR: ONLYOFFICE script load timeout: " + config.documentserverJsUrl);
          process.reject("ONLYOFFICE script load timeout. Ensure Document Server is running and accessible.");
        }
      }, timeout);
    }

    /**
     * Create an viewer configuration (for use to create the viewer client UI).
     */
    var createViewer = function(config) {
      var process = $.Deferred();
      if (config) {
        config.type = EMBEDDED_MODE;
        config.height = "100%";
        config.width = "100%";
        config.editorConfig.embedded = {
          fullscreenUrl: config.editorUrl,
          saveUrl: config.downloadUrl,
          toolbarDocked: "top"
        };

        var retries = 15;
        var timeout = 200;
        waitDocsAPI(process, config, retries, timeout);
      } else {
        process.reject("Editor config not found");
      }
      return process.promise();
    };

    /**
     * Create an editor configuration (for use to create the editor client UI).
     */
    var createEditor = function(config) {
      var process = $.Deferred();
      if (config) {
        config.type = DESKTOP_MODE;
        config.height = "100%";
        config.width = "100%";
        config.events = {
          "onDocumentStateChange": onDocumentStateChange,
          "onError": onError,
          "onReady": onReady,
          "onBack": onBack
        };
        config.editorConfig.customization = {
          "chat": false,
          "compactToolbar": true,
          "goback": {
            "blank": true,
            "text": message("GoToDocument"),
            "url": config.explorerUrl
          },
          "help": true,
          "logo": {
            "image": prefixUrl + "/onlyoffice/images/exo-icone.png",
            "imageEmbedded": prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "url": prefixUrl + "/portal"
          },
          "customer": {
            "info": "eXo Platform",
            "logo": prefixUrl + "/eXoSkin/skin/images/themes/default/platform/skin/ToolbarContainer/HomeIcon.png",
            "mail": "support@exoplatform.com",
            "name": "Support",
            "www": "exoplatform.com"
          }
        };
        config.editorConfig.plugins = {
          "autostart": [],
          "pluginsData": []
        };

        log("ONLYOFFICE editor config: " + JSON.stringify(config));

        if ((typeof DocsAPI === "undefined") || (typeof DocsAPI.DocEditor === "undefined")) {
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
    this.createEditor = createEditor;
    this.createViewer = createViewer;

    this.init = init;

    /**
     * Initialize an editor page in current browser window (ECMS/Preview from Scream)
     */
    this.initEmbeddedViewer = function(config) {
      if (config) {
        log("Initialize viewer for document: " + config.docId);
        log("i18n: " + JSON.stringify(messages));
        createViewer(config).done(function(localConfig) {
          if (localConfig) {
            currentConfig = localConfig;
            currentConfig.document.title = decodeURI(decodeURI(currentConfig.document.title));
            $(function() {
              try {
                new DocsAPI.DocEditor("onlyoffice", localConfig);
              } catch (e) {
                log("Error initializing Onlyoffice client UI " + e, e);
              }
            });
          } else {
            log("ERROR: editor config not defined: " + localConfig);
            UI.showViewerError(message("PreviewNotAvailable"), message("ErrorCreateConfig"));
          }
        }).fail(function(error) {
          log("ERROR: editor config creation failed : " + error);
          UI.showViewerError(message("PreviewNotAvailable"), message("CannotLoadViewerScript"));
        });
      } else {
        log("ERROR: editor config not defined: " + config);
        UI.showViewerError(message("PreviewNotAvailable"), message("ErrorCreateConfig"));
      }
    };

    /**
     * Initialize an editor page in current browser window.
     */
    this.initEditor = function(config) {
      editorsupport.onEditorOpen(config.docId, config.workspace, ONLYOFFICE).done(function() {
        initBar(config);
        log("Initialize editor for document: " + config.docId);
        window.document.title = config.document.title + " - " + window.document.title;
        UI.initEditor();
        // customize Onlyoffice JS config
        config.type = DESKTOP_MODE;
        config.height = "100%";
        config.width = "100%";
        createEditor(config).done(function(localConfig) {
          if (localConfig) {
            currentConfig = localConfig;

            store.subscribe(function() {
              var state = store.getState();
              if (state.type === DOCUMENT_DELETED) {
                UI.showError(message("ErrorTitle"), message("ErrorFileDeletedEditor"));
              }
              if (state.type === DOCUMENT_SAVED) {
                UI.updateBar(state.displayName, state.comment, currentConfig.workspace, currentConfig.docId);
                if (state.comment) {
                  currentConfig.editorPage.comment = state.comment;
                }
                if (state.userId === currentUserId) {
                  currentUserChanges = false;
                }
              }
              if (state.type === DOCUMENT_TITLE_UPDATED) {
                console.log("Title updated");
                var oldTitle = currentConfig.document.title;
                currentConfig.document.title = state.title;
                window.document.title = window.document.title.replace(oldTitle, state.title);
                $("#editor-drawer").find(".editable-title").text(state.title).append("<i class='uiIconPencilEdit'></i>");
                var documentOldTitle = config.explorerUrl.substr(config.explorerUrl.lastIndexOf("/"));
                var url = config.explorerUrl.split(documentOldTitle)[0];
                var documentNewPath = url + "/" + state.title;
                config.explorerUrl = documentNewPath;
                $("#see-more-btn").prop("disabled", true);
                setTimeout(function() {
                  $("#see-more-btn").prop("disabled", false);
                }, 5000);
              }
            });

            // Establish a Comet/WebSocket channel from this point.
            // A new editor page will join the channel and notify when the doc
            // will be saved
            // so we'll refresh this explorer view to reflect the edited content.
            subscribeDocument(currentConfig.docId);
            // We are a editor oage here: publish that the doc was changed by
            // current user

            window.addEventListener("beforeunload", function() {
              UI.closeEditor(); // try this first, then in unload
            });

            window.addEventListener("unload", function() {
              UI.closeEditor();
              // We need to save current changes when user closes the editor
              if (currentConfig) {
                publishDocument(currentConfig.docId, {
                  "type": EDITOR_CLOSED,
                  "userId": currentUserId,
                  "explorerUrl": config.explorerUrl,
                  "key": currentConfig.document.key,
                  "changes": currentUserChanges
                });
              }
            });

            $(function() {
              try {
                UI.createEditor(currentConfig);
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

        $("#open-drawer-btn").on('click', function() {
          return UI.openDrawer();
        });

        $("#editor-drawer .header .closebtn").on('click', function() {
          return UI.closeDrawer();
        });

        $("#see-more-btn").on('click', function() {
          window.open(config.explorerUrl);
        });

        $("#load-more-btn").on('click', function() {
          UI.loadVersions(currentConfig.workspace, currentConfig.docId, 3, pageToLoad);
          pageToLoad++;
        });
      }).fail(function() {
        UI.showError(message("ErrorTitle"), message("AnotherEditorIsOpen"));
      });
    };

    /**
     * Inits the editor in 'View' mode on separate page (fullsreen)
     */
    this.initViewer = function(config) {
      log("Initialize viewer for document: " + config.docId);
      window.document.title = config.document.title + " - " + window.document.title;
      UI.initEditor();
      // customize Onlyoffice JS config
      config.type = DESKTOP_MODE;
      config.height = "100%";
      config.width = "100%";
      createEditor(config).done(function(localConfig) {
        if (localConfig) {
          currentConfig = localConfig;
          setTimeout(function() {
            store.subscribe(function() {
              var state = store.getState();
              if (state.type === DOCUMENT_CONTENT_UPDATED && state.docId === currentConfig.docId) {
                UI.addRefreshBanner(currentConfig, DESKTOP_MODE);
                // For default PFD preview
                // UI.addRefreshBannerPDF();
              }
            });
          }, 100);
          subscribeDocument(currentConfig.docId);

          $(function() {
            try {
              UI.createEditor(currentConfig);
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

      $("#open-drawer-btn").css("display", "none");
    };

    /**
     * Initializes a file activity in the activity stream.
     */
    this.initActivity = function(docId, editorLink, activityId) {
      log("Initialize activity with document: " + docId);
      // Listen to document updates
      store.subscribe(function() {
        var state = store.getState();
        if (state.type === DOCUMENT_CONTENT_UPDATED && state.docId === docId) {
          UI.addRefreshBannerActivity(activityId);
        }
      });
      subscribeDocument(docId);
      if (editorLink != null) {
        editorbuttons.addCreateButtonFn(ONLYOFFICE, function() {
          return UI.createEditorButton(editorLink);
        });
      }
    };

    /**
     * Initializes a document preview in Activity Stream
     */
    this.initPreview = function(settings) {
      if (settings) {
        init(settings.userId, settings.cometdConf, settings.messages);
        log("Initialize preview of document: " + settings.fileId);
        // We set timeout here to avoid the case when the element is rendered but
        // is going to be updated soon
        setTimeout(function() {
          store.subscribe(function() {
            var state = store.getState();
            if (state.type === DOCUMENT_CONTENT_UPDATED && state.docId === settings.fileId) {
              UI.addRefreshBanner(currentConfig);
              // For default PFD preview
              // UI.addRefreshBannerPDF();
            }
          });
        }, 100);
        subscribeDocument(settings.fileId);
        if (settings.link != null) {
          editorbuttons.addCreateButtonFn(ONLYOFFICE, function() {
            return UI.createEditorButton(settings.link);
          });
        } else if (settings.error) {
          log(message(settings.error.type) + " - " + message(settings.error.message));
        }
      }
    };

    /**
     * Initializes JCRExplorer when a document is displayed.
     */
    this.initExplorer = function(settings) {
      if (settings) {
        init(settings.userId, settings.cometdConf, settings.messages);
        log("Initialize explorer with document: " + settings.fileId);
        // Listen document updated
        store.subscribe(function() {
          var state = store.getState();
          if (state.type === DOCUMENT_CONTENT_UPDATED) {
            if (state.userId === currentUserId) {
              UI.refreshPreview(currentConfig, EMBEDDED_MODE);
              // For default PDF viewer
              // UI.refreshPDFPreview();
            } else {
              UI.addRefreshBanner(currentConfig);
              // For default PDF viewer
              // UI.addRefreshBannerPDF(currentConfig);
            }
          }
          if (state.type === DOCUMENT_DELETED) {
            UI.showError(message("ErrorTitle"), message("ErrorFileDeletedECMS"));
          }
        });
        if (settings.fileId != explorerDocId) {
          // We need unsubscribe from previous doc
          if (explorerDocId) {
            unsubscribeDocument(explorerDocId);
          }
          subscribeDocument(settings.fileId);
          explorerDocId = settings.fileId;
        }
        if (settings.link != null) {
          editorbuttons.addCreateButtonFn(ONLYOFFICE, function() {
            return UI.createEditorButton(settings.link);
          });
        } else if (settings.error) {
          log(message(settings.error.type) + " - " + message(settings.error.message));
        }
      }
    };

    /**
     * Opens a new window for the editor. We open the empty tab and then init
     * editor there. It's used to provide better UX (no delay between click on
     * the button and openning window with the editor)
     */
    this.initNewDocument = function() {
      editorWindow = window.open();
    };

    /**
     * Initializes the editor in the editorWindow. (used in creating a new
     * document)
     */
    this.initEditorPage = function(link) {
      if (editorWindow != null) {
        if (link != null) {
          editorWindow.location = link;
        } else {
          editorWindow.close();
          editorWindow = null;
          UI.showError(message("ErrorTitle"), message("ErrorLinkNotFound"));
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

    var updateVesionsList = false;

    var versionsListHeight = ($(".content").height() - 220) < 0 ? 0 : Math.round(($(".content").height() - 220) / 91);

    /**
     * Returns the html markup of the refresh banner;
     */
    var getRefreshBanner = function() {
      return "<div class='documentRefreshBanner'><div class='refreshBannerContent'>" + message("UpdateBannerTitle") +
        "<span class='refreshBannerLink'>" + message("ReloadButtonTitle") + "</span></div></div>";
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
          attributes: true
        });
      }
    };

    /**
     *  Refreshes an embedded viewer or on separate page
     */
    var refreshPreview = function(currentConfig, mode) {
      // View mode on separate page - reloads the page
      if (mode === DESKTOP_MODE) {
        window.location.reload(true);
      }
      // Get viewer config for reloading embedded viewer
      getViewerConfig(currentConfig.workspace, currentConfig.docId).then(function(config) {
        $(".onlyofficeViewerContainer .viewer").html("<div id='onlyoffice'></div>");
        config.type = EMBEDDED_MODE;
        config.height = "100%";
        config.width = "100%";
        config.editorConfig.embedded = {
          fullscreenUrl: config.editorUrl,
          saveUrl: config.downloadUrl,
          toolbarDocked: "top"
        };
        new DocsAPI.DocEditor("onlyoffice", config)
      }).catch(function(xhr, status, error) {
        log("Cannot get viewer config: " + error);
      });
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
      }, 250); // XXX we need wait for office preview server generate a new
      // preview
    };

    this.refreshPDFPreview = refreshPDFPreview;
    this.refreshPreview = refreshPreview;

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

    /**
     * Alert save changes.
     */
    this.alertSave = function() {
      $("#alert-saved").show();
      setTimeout(function() {
        $("#alert-saved").hide();
      }, 3000);
    };

    /**
     * Alert you have exceeded the limit of comment changes.
     */
    this.errorSave = function() {
      $("#alert-error").show();
      setTimeout(function() {
        $("#alert-error").hide();
      }, 3000);
    };

    /**
     * Update real time version list.
     */
    this.updateBar = function(changer, comment, workspace, docId) {
      UI.loadVersions(workspace, docId, versionsListHeight, 0);
      updateVesionsList = true;
    };

    // Function for remove accents
    this.removeAccents = function(str) {
      var accents = 'ÀÁÂÃÄÅàáâãäåÒÓÔÕÕÖØòóôõöøÈÉÊËèéêëðÇçÐÌÍÎÏìíîïÙÚÛÜùúûüÑñŠšŸÿýŽž';
      var accentsOut = "AAAAAAaaaaaaOOOOOOOooooooEEEEeeeeeCcDIIIIiiiiUUUUuuuuNnSsYyyZz";
      str = str.split('');
      var strLen = str.length;
      var i, x;
      for (i = 0; i < strLen; i++) {
        if ((x = accents.indexOf(str[i])) != -1) {
          str[i] = accentsOut[x];
        }
      }
      return str.join('');
    };

    this.createEditorButton = function(editorLink) {
      return $("<li class='hidden-tabletL'><a href='" + editorLink + "' target='_blank'>" +
        "<i class='uiIconEcmsOnlyOfficeOpen uiIconEcmsLightGray uiIconEdit'></i><span class='editorLabel'>" + message("EditButtonTitle") +
        "</span></a></li>");
    };

    this.initBar = function(config) {
      config.editorPage.displayPath = decodeURI(config.editorPage.displayPath);
      config.path = decodeURI(config.path);
      var drive = config.editorPage.drive;
      if (drive === null) {
        UI.showError(message("ErrorTitle"), message("ErrorNameDriveNull"));
        return;
      }
      var $bar = $("#editor-drawer");
      if (drive.startsWith('spaces/')) {
        var folders = config.editorPage.displayPath.split(':')[1].split('/');
        var title = folders.pop();
        var titleWithoutAccent = this.removeAccents(title);
        var spaceName = drive.split('spaces/')[1];
        var spaceNameWithoutAccent = this.removeAccents(spaceName);
        var newDrivePath = spaceNameWithoutAccent.replace(/\ /g, '_').toLowerCase();
        var pathDocument = config.path.split(newDrivePath + '/')[1].split("/" + titleWithoutAccent)[0];
        var pathDocumentWithIcon = pathDocument.replace(/\//g, function() {
          return "<i class='uiIconArrowRight'></i>";
        });
        var $avatarSpaceElem = $bar.find(".spaceAvatar img");
        $avatarSpaceElem.attr("src", "/rest/v1/social/spaces/" + newDrivePath + "/avatar");
        var $tooltipSpaceElem = $bar.find(".spaceAvatar img");
        $tooltipSpaceElem.attr("data-original-title", spaceName);
        $(".spaceAvatar img").tooltip();
      } else {
        var folders = config.editorPage.displayPath.split(':')[1].split('/');
        var title = folders.pop();
        var path = config.editorPage.displayPath.split('/')[0];
        var pathDocumentWithIcon = path.replace(/\//g, function() {
          return "<i class='uiIconArrowRight'></i>";
        });
        var $avatarSpaceElem = $bar.find(".spaceAvatar img");
        $avatarSpaceElem.attr("src", "/rest/v1/social/users/" + config.editorConfig.user.id + "/avatar");
        var $tooltipSpaceElem = $bar.find(".spaceAvatar img");
        $tooltipSpaceElem.attr("data-original-title", config.editorConfig.user.name);
        $(".spaceAvatar img").tooltip();
      }
      $(".header").append(message('SaveVersionLabel'));
      $("#alert-saved").append(message('AlertSave'));
      $("#alert-error").append(message('ErrorSave'));
      $("#save-btn").append(message('SaveButton'));
      $("#see-more-btn").attr("data-original-title", message('SeeMoreButton'));
      $("#load-more-btn").append(message('LoadMoreButton'));
      $("#open-drawer-btn").attr("data-original-title", message('OpenDrawerBtn'));
      $(".closebtn").attr("data-original-title", message('CloseButton'));
      $(".versionSummaryField").attr("placeholder", message('PlaceHolderTextarea'));
      $("#open-drawer-btn").tooltip();
      if (config.editorPage.renameAllowed) {
        $bar.find("a[rel=tooltip]").tooltip();
      } else {
        $bar.find("a[rel=tooltip]").not(".document-title a[rel=tooltip]").tooltip();
      }
      var $pathElem = $bar.find(".document-path");
      $pathElem.append("<span class='folder'>" + pathDocumentWithIcon + "</span>" + " <i class='uiIconArrowRight'></i> ");

      var $titleElem = $bar.find(".document-title a");
      $titleElem.append("<span class='editable-title'>" + title + " " + "<i class='uiIconPencilEdit'></i> </span>");
      $titleElem.attr("data-original-title", message('TitleTooltip'));

      $("#editor-drawer").ready(function() {
        UI.loadVersions(config.workspace, config.docId, versionsListHeight, 0);
      });

      var $saveBtn = $bar.find("#save-btn .uiIconSave");
      $saveBtn.on("click", function() {
        $saveBtn.css("color", "gray");
        setTimeout(function() {
          $saveBtn.css("color", "")
        }, 300)
      });
      return $bar;
    };

    this.loadVersions = function(workspace, docId, itemParPage, pageNum) {
      $.ajax({
        url: "/portal/rest/onlyoffice/editor/versions/" + workspace + "/" + docId + "/" + itemParPage + "/" + pageNum,
        success: function(data) {
          var html = "";
          for (var i = 0; i < data.length; i++) {
            html += "<table class='tableContentStyle'>" + "<tr class='tableHead'>" + "<th class='displayAvatarFullName'>" +
              "<div class='avatarCircle'>" + "<img src='/rest/v1/social/users/" +
              data[i].author +
              "/avatar'>" +
              "</div>" +
              "<div class='user-edit'>" +
              data[i].fullName +
              "</div>" +
              "<div class='created-date' rel='tooltip' data-placement='bottom'  data-original-title='" +
              UI.getAbsoluteTime(data[i].createdTime) +
              "'>" +
              UI.getRelativeTime(data[i].createdTime) +
              "</div>" +
              "</th>" +
              "</tr>" +
              "<tr class='tableContent'>" +
              "<th>" +
              "<div class='editors-comment-versions b' rel='tooltip' data-placement='bottom'  data-original-title='" +
              data[i].versionLabels + "'>" + data[i].versionLabels + "</div>" + "</th>" + "</tr>" + "</table>";
          };

          // Test update DOM versions list or load more versions
          if (updateVesionsList) {
            $("#versions").html(html);
          } else {
            $("#versions").append(html);
          };
          $(".editors-comment-versions").tooltip();
          $(".created-date").tooltip();
          updateVesionsList = false;
          $("#load-more-btn").show();

          // Disable load buton when no more versions
          if (data && data.length && data[0].versionPageNumber && (pageNum >= (data[0].versionPageNumber - 1))) {
            $("#load-more-btn").prop("disabled", true);
          } else {
            $("#load-more-btn").prop("disabled", false);
          };
        },
        error: function(xhr, thrownError) {
          $("#versions").html(
            "<div class='no-versions-icon'><i class='uiIconNoVersions'></i><div class='no-versions'> " + message('NoVersions') +
            "</div> </div>");
          log("Error fetching versions: " + xhr.responseText + "\n" + xhr.status + "\n" + thrownError, thrownError);
          $("#load-more-btn").hide();
        }
      });
    };

    this.getAbsoluteTime = function(time) {
      return new Date(time).toLocaleString(eXo.env.portal.language);
    };

    this.getRelativeTime = function(time) {
      const relativeTime = (new Date().getTime() - time) / 1000;
      let value;
      if (relativeTime < 60) {
        return message('TimeConvert.Less_Than_A_Minute');
      } else {
        if (relativeTime < 120) {
          return message('TimeConvert.About_A_Minute');
        } else {
          if (relativeTime < 3600) {
            value = Math.round(relativeTime / 60);
            return message('TimeConvert.About_X_Minutes').replace('{0}', value);
          } else {
            if (relativeTime < 7200) {
              return message('TimeConvert.About_An_Hour');
            } else {
              if (relativeTime < 86400) {
                value = Math.round(relativeTime / 3600);
                return message('TimeConvert.About_X_Hours').replace('{0}', value);
              } else {
                if (relativeTime < 172800) {
                  return message('TimeConvert.About_A_Day');
                } else {
                  if (relativeTime < 2592000) {
                    value = Math.round(relativeTime / 86400);
                    return message('TimeConvert.About_X_Days').replace('{0}', value);
                  } else {
                    if (relativeTime < 5184000) {
                      return message('TimeConvert.About_A_Month');
                    } else {
                      value = Math.round(relativeTime / 2592000);
                      return message('TimeConvert.label.About_X_Months').replace('{0}', value);
                    }
                  }
                }
              }
            }
          }
        }
      }
    };

    this.isEditorLoaded = function() {
      return $("#UIPage .onlyofficeContainer").length > 0;
    };

    /**
     * Open the drawer
     */
    this.openDrawer = function() {
      $("#editor-drawer").addClass("open");
      $("#drawer-backdrop").addClass("drawer-backdrop");
    };

    /**
     * Close the drawer
     */
    this.closeDrawer = function() {
      $("#editor-drawer").removeClass("open");
      $("#drawer-backdrop").removeClass("drawer-backdrop");
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
    this.createEditor = function(localConfig) {
      var $editorPage = $("#OnlyofficeEditorPage");
      if ($editorPage.length > 0) {
        if (!docEditor) {
          // show loading while upload to editor - it is already added by WebUI
          // side
          var $container = $editorPage.find(".onlyofficeContainer");

          // create and start editor (this also will re-use an existing editor
          // config from the server)
          docEditor = new DocsAPI.DocEditor("onlyoffice", localConfig);
          // show editor
          $container.find("#editor-drawer").show();
          $container.find(".editor").show();
          $container.find(".loading").hide();
        } else {
          log("WARN: Editor client already initialized");
        }
      } else {
        log("WARN: Editor element not found");
      }
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
     * Ads the refresh banner to the OnlyOffice document preview.
     */
    this.addRefreshBanner = function(currentConfig, mode) {
      var $container;
      if (mode === DESKTOP_MODE) {
        $container = $(".onlyofficeContainer .editor");
      } else {
        $container = $(".onlyofficeViewerContainer .viewer")
      }

      var $banner = $container.find(".documentRefreshBanner");
      if ($container.length !== 0 && $banner.length === 0) {
        $container.prepend(getRefreshBanner());
        $(".documentRefreshBanner .refreshBannerLink").click(function() {
          refreshPreview(currentConfig, mode);
        });
      }
    };

    /**
     * Show notice to user. Options support "icon" class, "hide", "closer" and
     * "nonblock" features.
     */
    this.showNotice = function(type, title, text, options) {
      var noticeOptions = {
        title: title,
        text: text,
        type: type,
        icon: "picon " + (options ? options.icon : ""),
        hide: options && typeof options.hide != "undefined" ? options.hide : false,
        closer: options && typeof options.closer != "undefined" ? options.closer : true,
        sticker: false,
        opacity: .9,
        addclass: 'onlyoffice-notification',
        shadow: true,
        width: options && options.width ? options.width : NOTICE_WIDTH,
        nonblock: options && typeof options.nonblock != "undefined" ? options.nonblock : false,
        nonblock_opacity: .25,
        after_init: function(pnotify) {
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
        icon: "picon-dialog-error",
        hide: false,
        delay: 0,
        onInit: onInit
      });
    };

    /**
     * Show error on no-preview screen of viewer.
     */
    this.showViewerError = function(title, text) {
      var $viewer = $(".onlyofficeViewerContainer .viewer");
      $viewer.addClass("onlyoffice-no-preview");
      $viewer.append("<div class='no-preview-container'><h3 class='no-preview-title'>" + title + "</h3><p class='no-preview-desc'>" + text + "</p></div>");
    };

    /**
     * Show info notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showInfo = function(title, text, onInit) {
      return UI.showNotice("info", title, text, {
        hide: true,
        delay: 8000,
        icon: "picon-dialog-information",
        onInit: onInit
      });
    };

    /**
     * Show warning notice to user. Info will be shown for 8sec and hidden then.
     */
    this.showWarn = function(title, text, onInit) {
      return UI.showNotice("exclamation", title, text, {
        hide: false,
        delay: 30000,
        icon: "picon-dialog-warning",
        onInit: onInit
      });
    };
  }

  var editor = new Editor();
  var UI = new UI();

  $(function() {
    try {
      // load jquery-ui required stylesheets in js since only 1 stylesheet can
      // be loaded for a given portlet in gatein-resources.xml
      loadStyle("/onlyoffice/skin/jquery-ui.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.css");
      loadStyle("/onlyoffice/skin/jquery.pnotify.default.icons.css");

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
})($, cCometD, Redux, editorbuttons, editorsupport);