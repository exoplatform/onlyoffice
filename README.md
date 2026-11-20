# eXo Onlyoffice add-on

This add-on allow editing office documents directly in eXo Platform using ONLYOFFICE™ Documents Server. Thanks to the introduced menu items, in action bar of Documents explorer, users of eXo Platform can switch to editor in file preview of any supportable office document. As for now, the add-on offers support of following formats: 
- Office Open XML (Microsoft Office 2007 and later):
  - Word (.docx)
  - Excel (.xlsx) 
  - PowerPoint (.pptx) 
 
The Document Server should be [installed](http://helpcenter.onlyoffice.com/server/document.aspx) on reachable network. ONLYOFFICE™ offers native installers for Windows and Linux, they also have [Docker installation](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx) which will be described below in details. 

A demo video where we edit Word document in eXo Platform using ONLYOFFICE editor:
[![Watch demo video](/docs/images/youtube_screen.png)](https://www.youtube.com/watch?v=Ifioa0GfG-k)

## Usage

Since add-on installed, it adds a few UI elements to the eXo Platform. 
'Edit Online' button is added to Document Explorer action bar and to file activities in the Activity Stream. Also the editor button is displayed on document preview opened from the Activity Stream.

![Editor in Action bar](/docs/images/edit-online-btn.png)

When user opens an editor for current file, it will be loaded in a separate page. As Onlyoffice requires some time to download the document content to its Document Server and then prepare an UI, users will need wait for seconds before starting to work. Large files may require more time for loading (e.g. spreadsheets with lot of cells). 

![Editor](/docs/images/editing.png)

When users start to edit a document it's important to understand how Onlyoffice editor works. All modifications will go to a temporal storage in the Documents Server and sync with eXo side in such points:
- Single user edited a document and closed the editor (sync is performed in ~10 seconds)
- User modified a document and closed the editor in CoEdit mode 
- The user clicked ‘Save’ button on the Editor Bar (the comment is optional)
- User interrupted another user in CoEdit mode (changed the document when the another user’s changes weren’t saved yet)
- Autosave timer went off (in 10 minutes after last modification if the editor page is still open)

When several users edit the same document, all modifications are synced, and each user sees actual state of the document. If a document is versionable, the Onlyoffice add-on creates separate versions for every user with their changes. When the document is saved, the 'yellow bar' is added to the document preview in ECMS and Activity Stream with 'Reload' button.

![Yellow Banner on File Activity](/docs/images/file-activity.png)

Also, Onlyoffice add-on enables users to create documents from ECMS. The 'New Document' button is available in Document Explorer and opens a popup for creating a new office document.

![New Document](/docs/images/new-document-btn.png)

## Installation

### ONLYOFFICE™ Document Server

ONLYOFFICE™ Document Server can be [installed](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx) in Docker container from ready image with pre-configured defaults. While this add-on has no constraints on how the Onlyoffice installed, Docker way is a simplest and strightforward way to add Document Office to you infra, for both development or production. Document Server is an Open Source Software, thus if you'll need to customize it, it's always possible to get the sources and make your Docker image.

This add-on requires ONLYOFFICE Docs 10.0 or a later 10.x version; a 9.x Document Server is not supported. When you have installed Docker, you need a single command to start the Document Server on-premise, with the secret it shares with eXo Platform (see [JSON Web Tokens](#json-web-tokens-to-secure-access)):

    sudo docker run -i -t -d -p 80:80 -e JWT_SECRET=SECRET_KEY onlyoffice/documentserver:10.0

The Docker image listens on port 80; the DEB and RPM packages of ONLYOFFICE Docs 10.0 listen on port 8083 by default, which `onlyoffice.documentserver.host` must then name. You also can run it on HTTPS. Follow its guides for more detailed setup.

To check you have running Document Server successfuly, open a page on its address [http://DOCUMENT\_SERVER\_HOST/OnlineEditorsExample/](http://localhost/OnlineEditorsExample/), it will show you simple demo page where you can upload a document, or use their sample, and try to edit online in your browser. After you'll see it works well, you are ready to install the add-on in eXo Platform.

### eXo Platform Add-on
After you successfuly installed ONLYOFFICE™ Document Server, you need install Onlyoffice add-on to eXo Platform server. It should be Platform version 4.3 or higher. 

If you want use released binaries from eXo Catalog, simple run command in root of the server:

    > ./addon install exo-onlyoffice

When building from sources, then go to `/packaging/target` folder of the project and extract `exo-onlyoffice-packaging.zip` to a root of your server. You also may setup local add-ons catalog for eXo Addon manager if plan development with frequent installations, and then use this archive as a local package.

### Compatibility matrix

| eXo Addon version | OnlyOffice version |
|-------------------|--------------------|
| >= 7.3.x          | 10.0 or later 10.x (required) |
| >= 2.5.x          | 7.3.3.49              |
| >= 2.4.x          | 7.2.2              |
| >= 2.3.x          | 7.0.1              |
| >= 2.2.x          | 6.3.1              |
| >= 2.1.x          | 6.1                |
| >= 2.0.x          | 5.4.2.46           |
| >= 1.5.0-M07      | 5.4.2.46           |
| < 1.5.0-M07       | 5.3.0.243          |

## Configuration

Onlyoffice add-on need to know an adress of your Document Server and eXo Platform server, and it's also required to point a protocol scheme to use (`http` or `https`). Add following lines to your `exo.properties`.

    onlyoffice.documentserver.host=YOUR_DOCUMENT_SERVER_HOST
    onlyoffice.documentserver.schema=http
    onlyoffice.documentserver.secret=SECRET_KEY

Where `YOUR_DOCUMENT_SERVER_HOST` is an IP or host name of Document Server, with its port when it is not the scheme's default (8083 for the DEB and RPM packages).

`SECRET_KEY` is **mandatory**: it is the secret of the JSON Web Tokens exchanged with the Document Server, the same value on both sides, at least 32 bytes long (256 bits). When it is empty or shorter, the server logs an error at startup, the editor, conversions and the Document Builder do not work, and every request of the Document Server to eXo Platform is refused.

### Upgrading from ONLYOFFICE Docs 9.x

Upgrade the add-on and the Document Server in the same maintenance window. This version of the add-on refuses the tokens of a 9.x Document Server, which carry no purpose, so editing and conversions are unavailable between the two upgrades. Keep the secret configured on both sides.

## Security

ONLYOFFICE™ Document Server standalone doesn't offer a user authorization or such integration with external identities. As a result it's required to place it in secure network and for production it will be mandatory to run via [HTTPS](http://helpcenter.onlyoffice.com/server/docker/document/docker-installation.aspx#RunningHTTPS) or even use [strong SSL security](https://raymii.org/s/tutorials/Strong_SSL_Security_On_nginx.html). 

As it's not possible to provide user authorization between eXo and Onlyoffice servers, but need allow the Document Server access documents in eXo Platform, this addon implements document flow based on single session random idetifier for a file. Each new editor session uses a new ID for the same file. It doesn't let to guess a concrete file ID to download its content, but if someone will get current session ID it would be possible to grab the content. 

To minimize security flaws, in addition to HTTPS, the add-on restricts an access to document content and state RESTful services by allowing requesting them only by configired document server host. The Document Server host set in `exo.properties` as `onlyoffice.documentserver.host` will be used to check client requests. Requests from other hosts will be rejected with 401 response. 

Indeed, in some cases, it may be required to allow requests from any client host (e.g. for development or in case of proxy/NAT settings), then you can disable such restriction by setting following property to `false` value:

    onlyoffice.documentserver.accessOnly=false

Allowing access from any host, if no other security protection implemented, **strongly not recommended** as mentioned RESTful end-points can be accessed by anyone (doesn't check eXo credentials to allow the Document Server work with them). 

### Allowed hosts to secure access from Document Server

When running in a complex infrastructure, the Document server's hostname/IP may differ for user requests (URLs used in editor Config) and for requests the server will made to eXo Platform REST endpoints, it may be required to point several Document Server host names that allowed to accept by the add-on backend.

For this case there is an extra configuration parameter `onlyoffice.documentserver.allowedhosts`, it extends value of `onlyoffice.documentserver.host`, if need point several hosts in it separated by a comma:

    onlyoffice.documentserver.allowedhosts=YOUR_DOCUMENT_SERVER_HOST_1,YOUR_DOCUMENT_SERVER_HOST_2
    
This parameter will work only in conjunction with `onlyoffice.documentserver.accessOnly=true`.  

### JSON Web Tokens to secure access

ONLYOFFICE Docs signs every request it sends to eXo Platform, and the add-on signs every request it sends to ONLYOFFICE Docs, with JSON Web Tokens. Both sides **must** share the same secret. Each token carries its purpose (`operation`: a save callback, a download, a command, a conversion or a Document Builder run), and each side refuses a token used for another purpose: keep ONLYOFFICE Docs' `services.CoAuthoring.token.purposeCheck` at its default, `validate`.

ONLYOFFICE Docs 10.0 enables tokens by default. With the Docker image, set the secret with the `JWT_SECRET` environment variable, and never keep its placeholder value, `developer-only-not-a-real-secret`. Otherwise, edit a configuration file which can be found (or created) at the following path:

    For Linux - /etc/onlyoffice/documentserver/local.json.
    For Windows - %ProgramFiles%\ONLYOFFICE\DocumentServer\config\local.json.

Check that tokens validation is enabled (the default in 10.0):

    services.CoAuthoring.token.enable.request.outbox
    services.CoAuthoring.token.enable.request.inbox
    services.CoAuthoring.token.enable.browser
    
Specify the secret key (minimum 256 bits) in

    services.CoAuthoring.secret.outbox.string
    services.CoAuthoring.secret.inbox.string

Save the local.json and restart the services: 

    supervisorctl restart all

For more detailed information check the [Onlyoffice API Documentation](https://api.onlyoffice.com/editors/signature/)

## Editor Events

Onlyoffice add-on publishes some eXo listener events to let end-user apps listen on them and build required data/output for collecting stats or other purposes.

The add-on broadcasts such events (the constants are available in OnlyofficeEditorService interface):

    EDITOR_OPENED_EVENT - exo.onlyoffice.editor.opened
    EDITOR_CLOSED_EVENT - exo.onlyoffice.editor.closed
    EDITOR_SAVED_EVENT - exo.onlyoffice.editor.saved
    EDITOR_VERSION_EVENT - exo.onlyoffice.editor.version
    EDITOR_ERROR_EVENT - exo.onlyoffice.editor.error

The opened and closed events are published when an user opens or closes the editor relatively. The saved event appears when the last user has closed a document and its content has been saved.
The version event is published when a new version of a document has been created due to coediting or autosave time.
The error event appears when an error has occured while working with the Document Server.

The data object passed to the event has DocumentStatus class which contains some useful information for end-user apps. For example, it contains the config, that has a full information about the editor, including the opened and closed times (for the opened and closed events). The coEdited field helps to figure out the original reason of the version event (true means that the version has been created becouse of coediting, false - due to autosave timer)
