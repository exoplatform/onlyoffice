package org.exoplatform.onlyoffice.jcr;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.commons.chain.Context;

import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * The Class ModifyDocumentAction notifies the OnlyOfficeEditorService when Office document was updated.
 */
public class ModifyDocumentAction implements Action {

  /** The Constant LOG. */
  protected static final Log      LOG               = ExoLogger.getLogger(ModifyDocumentAction.class);

  /** The Constant MIX_REFERENCEABLE. */
  private static final String     MIX_REFERENCEABLE = "mix:referenceable";

  /** The editor service. */
  private OnlyofficeEditorService editorService;

  /**
   * Instantiates a new modify document action.
   */
  public ModifyDocumentAction() {
    editorService = WCMCoreUtils.getService(OnlyofficeEditorService.class);
  }

  /**
   * Handles updating JCR_DATA in office documents and notifies the editor service.
   *
   * @param context the context
   * @return true if the processing of this Context has been completed
   * @throws Exception the exception
   */
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
      if (node.canAddMixin(MIX_REFERENCEABLE)) {
        node.addMixin(MIX_REFERENCEABLE);
      }
      String userId = ConversationState.getCurrent().getIdentity().getUserId();
      editorService.onDocumentContentUpdated(node.getSession().getWorkspace().getName(), node.getUUID(), userId);
    }
    return false;
  }

}
