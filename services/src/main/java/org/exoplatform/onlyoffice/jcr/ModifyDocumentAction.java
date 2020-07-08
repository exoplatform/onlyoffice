package org.exoplatform.onlyoffice.jcr;

import javax.enterprise.context.Conversation;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

import org.apache.commons.chain.Context;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class ModifyDocumentAction implements Action {

  /** The Constant LOG. */
  protected static final Log      LOG = ExoLogger.getLogger(ModifyDocumentAction.class);

  private OnlyofficeEditorService editorService;

  public ModifyDocumentAction() {
    editorService = WCMCoreUtils.getService(OnlyofficeEditorService.class);
  }

  @Override
  public boolean execute(Context context) throws Exception {
    Item item = (Item) context.get("currentItem");
    Node node = (item instanceof Property) ? item.getParent() : (Node) item;
    if (node.isNodeType(NodetypeConstant.NT_RESOURCE)) {
      node = node.getParent();
    }
    if (!node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
      return false;
    }
    String propertyName = item.getName();
    if (propertyName.equals(NodetypeConstant.JCR_DATA) && editorService.isDocumentMimeSupported(node)) {
      if (node.canAddMixin("mix:referenceable")) {
        node.addMixin("mix:referenceable");
      }
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      editorService.onDocumentContentUpdated(node.getSession().getWorkspace().getName(), node.getUUID(), userId);
    }

    return false;
  }

}
