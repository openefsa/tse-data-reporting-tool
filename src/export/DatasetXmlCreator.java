package export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import table_database.Relation;
import table_database.TableDao;
import table_skeleton.TableColumn;
import table_skeleton.TableRow;

/**
 * Class that creates a report in .xml format using the
 * information contained in a collection of {@link TableRow}.
 * @author avonva
 *
 */
public class DatasetXmlCreator {
	
	private Document xsdModel;
	
	public DatasetXmlCreator(Document xsdModel) {
		this.xsdModel = xsdModel;
	}
	
	public DatasetXmlCreator() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	/**
	 * Export a {@link TableRow} object with all its children tables.
	 * Note that each record will be created as a flat one, 
	 * in the sense that all the parent information will be
	 * replicated at the leaves level. This means that in the end we will have
	 * as number of records the number of leaves.
	 * @param root
	 * @throws IOException
	 */
	public void export(TableRow root) throws IOException {

		// this hashmap will contain all the parents retrieved
		// until the current node
		AncestorList parents = new AncestorList();
		
		Stack<TableRow> nodes = new Stack<>();  // depth-first exploration
		
		// add the root to the stack
		nodes.add(root);
		
		// until we have something
		while (!nodes.isEmpty()) {

			// get the current node (this removes it from the stack)
			TableRow currentNode = nodes.pop();
			
			//System.out.println("Processing " + currentNode.getSchema().getSheetName() + " " + currentNode.getId());

			// get all the children of the current node (i.e. the tables that
			// are directly children of the current node, not nephews etc)
			Collection<Relation> relations = currentNode.getSchema().getDirectChildren();

			// no child relation in the definition => we are in a leaf
			// therefore we print the node with its parents until here
			if (relations.isEmpty()) {
				print(currentNode, parents);
				continue;
			}
			
			// here we have some children therefore we need to explore
			// deeper the tree of dependencies
			
			// for each direct children
			for (Relation r : relations) {
				
				//System.out.println("Opening dao for " + r.getChild());
				
				// open the child dao
				TableDao dao = new TableDao(r.getChildSchema());
				
				String parentTable = r.getParent();  // get parent table name
				int parentId = currentNode.getId();  // get parent id (we use it as foreign key in the child table)
				
				// get the rows of the children related to the parent
				Collection<TableRow> children = dao.getByParentId(parentTable, parentId);
				
				// if we have something then add all the children to the
				// stack in order to process them later and put the
				// current node in the parent hashmap, since we need
				// to go deeper in the tree
				if (!children.isEmpty()) {

					// add to stack
					nodes.addAll(children);
					
					//System.out.println("Children size " + children.size());
					//System.out.println("ADD INTO PARENTS " + currentNode.getSchema().getTableIdField());
					
					// if we have children save the parent data
					// since we need to carry on the parent
					// to be able to put it in the output of
					// each row
					parents.addAncestor(currentNode);
				}
				else {

					// if no children then we have a leaf
					// and we print it
					print(currentNode);
				}
			}
		}
	}
	
	/**
	 * Get all the elements of a root node
	 * @param root
	 * @return
	 */
	private Collection<XSElement> getElements(Element root) {

		Collection<XSElement> list = new ArrayList<>();
		
		NodeList elements = root.getElementsByTagName("xs:element");
		
		for (int i = 0; i < elements.getLength(); ++i) {
			
			Element node = (Element) elements.item(i);
			
			XSElement xsElem = new XSElement();
			
			if (node.hasAttribute("name"))
				xsElem.setName(node.getAttribute("name"));
			
			if (node.hasAttribute("ref"))
				xsElem.setRef(node.getAttribute("ref"));
			
			list.add(xsElem);
		}
		
		return list;
	}
	
	/**
	 * Get a list of {@link XSElement} which represents the xml node of the data set
	 * @param datasetNodeName
	 * @return
	 */
	private Collection<XSElement> getDatasetSchema(String datasetNodeName) {
		
		Collection<XSElement> datasetElements = new ArrayList<>();
		
		Element main = xsdModel.getDocumentElement();
		
		// get first root
		Node childNode = main.getFirstChild();
		
		// search in the root nodes
		while (childNode.getNextSibling() != null) {
			
			// get next root
			childNode = childNode.getNextSibling();
			
			// get elements only
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				
				// get root node
				Element childElement = (Element) childNode;
				
				// skip element without name
				if (!childElement.hasAttribute("name"))
					continue;
				
				// if dataset node parse children nodes and return them
				// (we do not need to go on, we need just this)
				if (childElement.getAttribute("name").equals(datasetNodeName)) {
					datasetElements = getElements(childElement);
					break;
				}
			}
		}
		
		return datasetElements;
	}
	
	/**
	 * Print an xml row with all its parents values flattened
	 * @param row
	 */
	public void print(TableRow row, AncestorList parents) {
		
		//System.out.println("PRINTING LEAF " + row.getSchema().getTableIdField() + " " + row.getId());
		System.out.println("=============================");
		for (TableRow parentRow: parents) {
			print(parentRow);
		}
		
		print(row);
	}
	
	/**
	 * Print a single row with its elements
	 * @param row
	 */
	private void print(TableRow row) {
		
		for (TableColumn column : row.getSchema()) {

			// skip non output columns
			if (!column.isPutInOutput(row))
				continue;

			// get the xml tag of the column
			String xmlTag = column.getXmlTag();
			String open = "<" + xmlTag + ">";
			String close = "</" + xmlTag + ">";

			// TODO write on file
			System.out.println(open + row.get(column.getId()).getCode() + close);
		}
	}
}
