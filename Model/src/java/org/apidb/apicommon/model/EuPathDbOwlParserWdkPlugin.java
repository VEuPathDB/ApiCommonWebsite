package org.apidb.apicommon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apidb.apicommon.model.ontology.OBOentity;
import org.apidb.apicommon.model.ontology.OWLReasonerRunner;
import org.apidb.apicommon.model.ontology.OntologyManipulator;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.ontology.JavaOntologyPlugin;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class EuPathDbOwlParserWdkPlugin implements JavaOntologyPlugin {

  public static final String orderAnnotPropStr = "http://purl.obolibrary.org/obo/EUPATH_0000274"; // Display
                                                                                                  // Order
                                                                                                  // annotation
                                                                                                  // property
                                                                                                  // IRI
  public static final String reasonerName = "hermit";
  public static final String owlFileFullPathParamName = "owlFileFullPath";

  @Override
  public TreeNode<Map<String, List<String>>> getTree(Map<String, String> parameters) {
    String inputOwlFile = parameters.get(owlFileFullPathParamName);
    // load OWL format ontology
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ont = OntologyManipulator.load(inputOwlFile, manager);
    OWLDataFactory df = manager.getOWLDataFactory();

    // reasoning the ontology
    OWLReasoner reasoner = OWLReasonerRunner.runReasoner(manager, ont, reasonerName);

    // get root node
    Node<OWLClass> topNode = reasoner.getTopClassNode();
    OWLClass owlClass = topNode.getEntities().iterator().next(); // get first one
    TreeNode <Map<String, List<String>>> tree = new TreeNode<Map<String, List<String>>>(convertToMap(ont, df, owlClass));
    build(topNode, reasoner, ont, df, orderAnnotPropStr, tree);
    return tree;
  }
  
  public static void build(Node<OWLClass> parent, OWLReasoner reasoner, OWLOntology ont, OWLDataFactory df, String orderAnnotPropStr, TreeNode <Map<String, List<String>>>tree) {
    // We don't want to print out the bottom node (containing owl:Nothing
    // and unsatisfiable classes) because this would appear as a leaf node
    // everywhere
    if (parent.isBottomNode()) {
        return;
    }
    
    // get children of a parent node and sort children based on their display order
    List<TermNode> childList = new ArrayList<TermNode>();
    
    for (Node<OWLClass> child : reasoner.getSubClasses(parent.getRepresentativeElement(), true)) {          
        for (Iterator<OWLClass> it = child.getEntities().iterator(); it.hasNext();) {
            OWLClass cls = it.next();
            
            OWLAnnotationProperty orderAnnotProp = df.getOWLAnnotationProperty(IRI.create(orderAnnotPropStr));
            String orderAnnotPropVal = OBOentity.getStringAnnotProps (cls, df, ont, orderAnnotProp);
            // if no order annotProperty associated with a term, 0 will be assigned 
            if (orderAnnotPropVal.length() == 0) {
                orderAnnotPropVal = "0";
            }
            TermNode t = new TermNode(child, Integer.parseInt(orderAnnotPropVal));
            if (t != null) {
                childList.add(t);
            }
            
            if (it.hasNext()) {
                System.out.print("node has multiple entity" + child.getSize());
            }
        }
    }

    Collections.sort(childList);
    
    for (int i = 0; i < childList.size(); i ++) {
        Node<OWLClass> cNode = childList.get(i).getNode();
        for (Iterator<OWLClass> it = cNode.getEntities().iterator(); it.hasNext();) {
            OWLClass cls = it.next();  
            Map<String,List<String>> content = convertToMap (ont, df, cls);
            if (content != null) {
                TreeNode <Map<String, List<String>>> t = new TreeNode <Map<String, List<String>>> (content);
                tree.addChildNode(t);
            }
            if (it.hasNext()) {
                break;
            }
        }
        
        build(cNode, reasoner, ont, df, orderAnnotPropStr, tree);
    }
}
        
private static Map<String,List<String>> convertToMap (OWLOntology ont, OWLDataFactory df, OWLClass cls) {
    Map<String,List<String>> node = new HashMap<String,List<String>>();
    Set<OWLAnnotation> annotList = cls.getAnnotations(ont);
    
    if (annotList.size() == 0) {
        return null;
    }
    
    Iterator<OWLAnnotation> it = annotList.iterator();
    while(it.hasNext()){
        OWLAnnotation currAnnot = it.next();
        OWLAnnotationProperty annotProp = currAnnot.getProperty();
        String annotPropLabel = OBOentity.getLabel(annotProp, ont, df);
        ArrayList<String> annotValues = OBOentity.getStringArrayAnnotProps (cls, df, ont, annotProp);
        
        if (annotValues.size() > 0 && !node.containsKey(annotPropLabel)) {
            node.put(annotPropLabel, annotValues);

                // for debugging
                System.out.print(annotPropLabel + ": ");
                for (int i = 0;  i < annotValues.size() ; i++ ){
                    System.out.print(annotValues.get(i) + ",");
                } 
                System.out.print("\n");
                // end of debugging
        }
    }
    
    // for nice output in debugging
    System.out.print("---create a new node \n\n"); 
    // end of debugging
    
    return node;
}


  @Override
  public void validateParameters(Map<String, String> parameters) throws WdkModelException {
    // TODO Auto-generated method stub

  }

   
  public static class TermNode implements Comparable<TermNode>{
    private Node<OWLClass> node;
    private Integer order;
    
    public TermNode (Node<OWLClass> node, Integer order) {
        this.node = node;
        this.order = order;
    }

    public void setNode (Node<OWLClass> node) {
        this.node = node;
    }
    
    public Node<OWLClass> getNode () {
        return node;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public int compareTo(TermNode term)
    {
        return getOrder().compareTo(term.getOrder());
    }
}



}
