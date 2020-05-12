package org.exoplatform.onlyoffice;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.chain.Context;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.DocumentUpdateActivityHandler;
import org.exoplatform.services.ext.action.InvocationContext;
import org.exoplatform.services.listener.Event;
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

  /** The editor service. */
  private OnlyofficeEditorService editorService;

  /**
   * Instantiates a new document update activity listener.
   */
  public OnlyOfficeDocumentUpdateActivityHandler() {
    editorService = (OnlyofficeEditorService) ExoContainerContext.getCurrentContainer()
                                                                 .getComponentInstanceOfType(OnlyofficeEditorService.class);
  }

  

  @Override
  public boolean handleDocumentUpdateEvent(Event<Context, String> event) throws Exception {
    Context context = event.getSource();
    Property currentProperty = (Property) context.get(InvocationContext.CURRENT_ITEM);
    Node currentNode = currentProperty.getParent().getParent();
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

}
