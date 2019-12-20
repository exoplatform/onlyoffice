<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<div class="onlyofficeContainer">
  <div class="loading">
    <div class="waitThrobber"></div>
  </div>
  <%-- div class="exitWindow onlyofficeEditorClose" style="display: none;">
    <a class="uiIconClose uiIconWhite" title="Close"></a>
  </div --%>
  <div id="open-drawer-btn" class="btn-primary open-drawer-btn" rel="tooltip" data-placement="bottom" data-original-title="">
    <a class="icon-place">
      <i class="uiIconSaveDrawer"></i>
    </a>
  </div>

  <div id="editor-drawer" class="drawer">

    <div class="header">
      <a class="closebtn" rel="tooltip" data-placement="bottom" data-original-title="">×</a>
    </div>

    <div class="content">
      <div class="contentDisplay">
        <div class="spaceAvatar">
          <img alt="" rel="tooltip" data-placement="right" data-original-title="">
        </div>
        <div class="displayPathTitle">
          <div class="document-path"></div>
          <div class="document-title">
            <a data-placement="bottom" rel="tooltip" data-original-title=""> </a>
          </div>
        </div>
      </div>
      <div class="comment">
        <div id="alert-saved" class="alert alert-success" style="display: none;"><i class="uiIconSuccess"></i></div>
        <div id="alert-error" class="alert alert-error" style="display: none;"><i class="uiIconError"></i></div>
        <textarea class="versionSummaryField" type="text" id="comment-box" placeholder=""></textarea>
      </div>
      <div id="save-btn" type="button" class="btn btn-primary"></div>
      <div id="versions"></div>
    </div>
    <div class="footer">
      <button id="see-more-btn" type="button" class="btn lineStyle"></button>
    </div>
  </div>
  <div id="drawer-backdrop"></div>

  <div class="editor" style="display: none;">
    <div id="onlyoffice"></div>
  </div>
</div>
