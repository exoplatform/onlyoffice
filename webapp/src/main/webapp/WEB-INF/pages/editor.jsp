<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<div class="onlyofficeContainer">
  <div class="loading">
    <div class="waitThrobber"></div>
  </div>
  <%-- div class="exitWindow onlyofficeEditorClose" style="display: none;">
    <a class="uiIconClose uiIconWhite" title="Close"></a>
  </div --%>
  <div id="editor-top-bar" style="display: none">
    <div class="document-path"></div>
    <div class="document-title">
      <a data-placement="bottom" rel="tooltip" data-original-title="Click to edit"> </a>
    </div>
    <div class="last-edited"></div>
    <div class="editors-comment" data-placement="bottom" rel="tooltip" data-original-title=""></div>
    <div class="comment">
      <input type="text" id="comment-box" placeholder="Add your summary"></input>
      <div id="save-btn">
        <a data-placement="bottom" rel="tooltip" data-original-title="Save">
          <i class="uiIconEcmsLightGray uiIconSave"></i>
        </a>
      </div>
    </div>
    <div class="close-btn">
      <a data-placement="left" rel="tooltip" data-original-title="Close">
        <i class="uiIconEcmsLightGray uiIconRemove"></i>
      </a>
    </div>
  </div>
  <div class="editor" style="display: none;">
    <div id="onlyoffice"></div>
  </div>
</div>
