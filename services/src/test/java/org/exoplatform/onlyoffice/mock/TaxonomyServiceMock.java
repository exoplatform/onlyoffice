package org.exoplatform.onlyoffice.mock;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyAlreadyExistsException;
import org.exoplatform.services.cms.taxonomy.impl.TaxonomyNodeAlreadyExistsException;

public class TaxonomyServiceMock implements TaxonomyService {

  @Override
  public Node getTaxonomyTree(String taxonomyName, boolean system) throws RepositoryException {
    return null;
  }

  @Override
  public Node getTaxonomyTree(String taxonomyName) throws RepositoryException {
    return null;
  }

  @Override
  public List<Node> getAllTaxonomyTrees(boolean system) throws RepositoryException {
    return null;
  }

  @Override
  public List<Node> getAllTaxonomyTrees() throws RepositoryException {
    return null;
  }

  @Override
  public boolean hasTaxonomyTree(String taxonomyName) throws RepositoryException {
    return false;
  }

  @Override
  public void addTaxonomyTree(Node taxonomyTree) throws RepositoryException, TaxonomyAlreadyExistsException {
  }

  @Override
  public void updateTaxonomyTree(String taxonomyName, Node taxonomyTree) throws RepositoryException {
  }

  @Override
  public void removeTaxonomyTree(String taxonomyName) throws RepositoryException {
  }

  @Override
  public void addTaxonomyNode(String workspace,
                              String parentPath,
                              String taxoNodeName,
                              String creator) throws RepositoryException, TaxonomyNodeAlreadyExistsException {
  }

  @Override
  public void removeTaxonomyNode(String workspace, String absPath) throws RepositoryException {
  }

  @Override
  public void moveTaxonomyNode(String workspace, String srcPath, String destPath, String type) throws RepositoryException {
  }

  @Override
  public boolean hasCategories(Node node, String taxonomyName) throws RepositoryException {
    return false;
  }

  @Override
  public boolean hasCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    return false;
  }

  @Override
  public List<Node> getCategories(Node node, String taxonomyName) throws RepositoryException {
    return null;
  }

  @Override
  public List<Node> getCategories(Node node, String taxonomyName, boolean system) throws RepositoryException {
    return null;
  }

  @Override
  public List<Node> getAllCategories(Node node) throws RepositoryException {
    return null;
  }

  @Override
  public List<Node> getAllCategories(Node node, boolean system) throws RepositoryException {
    return null;
  }

  @Override
  public void removeCategory(Node node, String taxonomyName, String categoryPath) throws RepositoryException {
  }

  @Override
  public void removeCategory(Node node, String taxonomyName, String categoryPath, boolean system) throws RepositoryException {
  }

  @Override
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths) throws RepositoryException {
  }

  @Override
  public void addCategories(Node node, String taxonomyName, String[] categoryPaths, boolean system) throws RepositoryException {
  }

  @Override
  public void addCategory(Node node, String taxonomyName, String categoryPath) throws RepositoryException {
  }

  @Override
  public void addCategory(Node node, String taxonomyName, String categoryPath, boolean system) throws RepositoryException {
  }

  @Override
  public Map<String, String[]> getTaxonomyTreeDefaultUserPermission() {
    return null;
  }

  @Override
  public String getCategoryNameLength() {
    return null;
  }

  @Override
  public void addTaxonomyPlugin(ComponentPlugin plugin) {
  }

  @Override
  public void init() throws Exception {
  }

}
