package org.exoplatform.onlyoffice;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentUpdateActivityHandler;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.wcm.ext.component.activity.listener.FileUpdateActivityListener;

/**
 * The listener interface for receiving documentUpdateActivity events.
 * The class that is interested in processing a documentUpdateActivity
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's addDocumentUpdateActivityListener method. When
 * the documentUpdateActivity event occurs, that object's appropriate
 * method is invoked.
 *
 */
public class OnlyOfficeDocumentUpdateActivityHandler extends FileUpdateActivityListener implements DocumentUpdateActivityHandler{

  /** The Constant EVENT_DELAY in min. */
  protected static final long     EVENT_DELAY = 10;

  /** The content property of a document. */
  private static final String     JCR_DATA    = "jcr:data";

  /** The logger. */
  private static final Log        LOG         = ExoLogger.getLogger(OnlyOfficeDocumentUpdateActivityHandler.class);

  /** The editor service. */
  private OnlyofficeEditorService editorService;

  /**
   * Instantiates a new document update activity listener.
   */
  public OnlyOfficeDocumentUpdateActivityHandler() {
    editorService = ExoContainerContext.getCurrentContainer()
                                       .getComponentInstanceOfType(OnlyofficeEditorService.class);
  }

  

  @Override
  public boolean handleDocumentUpdateEvent(Event<Context, String> event) throws Exception {
    Context context = event.getSource();
    Property currentProperty = (Property) context.get(InvocationContext.CURRENT_ITEM);
    Node currentNode = currentProperty.getParent().getParent();
    ImageThumbnailService imageThumbnailService = CommonsUtils.getService(ImageThumbnailService.class);
    String nodeId = getNodeId(currentNode);
    if (nodeId != null) {
      imageThumbnailService.deleteThumbnails(OfficeDocumentsThumbnailPlugin.DOCUMENTS_OFFICE, nodeId);
      if (JCR_DATA.equals(event.getData())) {
        closeEditorsOfOutsideWrite(nodeId);
      }
    }
    // If there is no manually added comment from the editor
    if (!isCommentedNode(currentNode)) {
      String lastModifier = currentNode.getProperty("exo:lastModifier").getString();
      String workspace = currentNode.getSession().getWorkspace().getName();
      String path = currentNode.getPath();
      Config config = editorService.getEditor(lastModifier, workspace, path);
      if (config != null && config.getSameModifier() != null && config.getPreviousModified() != null) {
        boolean sameModifier = config.getSameModifier();
        Calendar previousModified = config.getPreviousModified();
        long difference = Calendar.getInstance().getTimeInMillis() - previousModified.getTimeInMillis();
        if (!sameModifier || TimeUnit.MILLISECONDS.toMinutes(difference) > EVENT_DELAY) {
          super.onEvent(event);
          return true;
        }
      } 
      return false;
    }
    return true;
  }


  /**
   * Closes the editor sessions the content write leaves stale, the write's own
   * editor session excepted, so that the next opening shows the written content.
   * A failure is logged: it never fails the write.
   *
   * @param docId the document node's UUID
   */
  protected void closeEditorsOfOutsideWrite(String docId) {
    try {
      ConversationState conversationState = ConversationState.getCurrent();
      String userId = conversationState == null ? null : conversationState.getIdentity().getUserId();
      editorService.contentUpdatedOutsideEditor(docId, userId);
    } catch (RuntimeException e) {
      LOG.warn("Cannot close the editor sessions of document {} after its content was written outside the editor", docId, e);
    }
  }

  /**
   * Checks if a node has comment.
   *
   * @param node the node
   * @return true if the node is commented
   * @throws RepositoryException the repository exception
   */
  protected boolean isCommentedNode(Node node) throws RepositoryException {
    if (node.hasProperty("eoo:commentId")) {
      String commentId = node.getProperty("eoo:commentId").getString();
      return commentId != null && !commentId.isEmpty();
    }
    return false;
  }

  private String getNodeId(Node node) throws RepositoryException {
    if (node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
      return node.getUUID();
    } else if (node instanceof NodeImpl nodeImpl) {
      return nodeImpl.getIdentifier();
    } else {
      return null;
    }
  }

}
