package util;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;


public class JDOMUtil {
	
	public static Element copyElement(Element src, Element dest) {
		
		dest.setText( src.getText() );
		
		List<Attribute> fieldAttrs = src.getAttributes();
		for (Attribute attribute : fieldAttrs) {
			
			String name = attribute.getName();
			String value = attribute.getValue();
			
			dest.setAttribute(name, value, attribute.getNamespace());
		}
		
		return dest;
	}
	
	public static Element copyElement(Element src, Element dest, Namespace nsTmax) {
		
		List<Attribute> fieldAttrs = src.getAttributes();
		for (Attribute attribute : fieldAttrs) {
			
			String name = attribute.getName();
			String value = attribute.getValue();
			
			String prefix = attribute.getNamespacePrefix();
			if( prefix.equals("tmax") ) {
				dest.setAttribute(name, value, nsTmax);
			} else
				dest.setAttribute(name, value);
			
		}
		
		return dest;
	}

	public static Element copyElementChildren(Element src, Element dest, Namespace nsTmax) {
		
		List<Element> list = src.getChildren();
		for (Element element : list) {
			
			String prefix = element.getNamespace().getPrefix();
			
			Element newEle = null;
			if( prefix.equals("tmax") )
				newEle = new Element(element.getName(), nsTmax);
			else
				newEle = new Element(element.getName(), element.getNamespace());
			copyElement(element, newEle);
			
			if( element.getChildren().size() > 0 )
				copyElementChildren(element, newEle, nsTmax);
			
			dest.addContent(newEle);
			
		}
		
		return dest;
	}

	public static Element getElement(Document document, String ename, Namespace ...nss) {
		
		XPathExpression<Element> compile = XPathFactory.instance().compile(String.format("//%s", ename), 
				Filters.element(), null, nss);
		return compile.evaluateFirst(document);
		
	}

	public static List<Element> getElements(Document document, String ename, Namespace ...nss) {
		
		XPathExpression<Element> compile = XPathFactory.instance().compile(String.format("//%s", ename), 
				Filters.element(), null, nss);
		return compile.evaluate(document);
		
	}
	
	public static Element createElement(Element parent, String cname) {
		
		return createElement(parent, cname, null, null);
	}
	
	public static Element createElement(Element parent, String cname, Namespace ns) {
		
		return createElement(parent, cname, ns, null);
	}
	
	public static Element createElement(Element parent, String cname, Namespace ns, String text) {
		
		Element element = null;
		if( ns == null )
			element = new Element(cname);
		else
			element = new Element(cname, ns);
		parent.addContent(element);

		element.setText(text);
		
		return element;
	}

	
	public static String getText(Element parent, String cname) {

		return getText(parent, cname, null);
	
	}
	
	public static String getText(Element parent, String cname, Namespace ns) {
		
		Element element = null;
		if( ns == null )
			element = parent.getChild(cname);
		else
			element = parent.getChild(cname, ns);
		
		return element.getText();
	}
	
	public static Element getChild(Element parent, String cname) {
		
		return getChild(parent, cname, null);
		
	}
	
	public static Element getChild(Element parent, String cname, Namespace ns) {
		
		Element child = null;
		if( ns == null )
			child = parent.getChild(cname);
		else
			child = parent.getChild(cname, ns);
			
		return child;
		
	}
	
	public static Attribute getAttribute(Element element, String aname) {
		
		return getAttribute(element, aname, null);
		
	}

	public static Attribute getAttribute(Element element, String aname, Namespace ns) {
		
		Attribute attribute = null;
		if( ns == null )
			attribute = element.getAttribute(aname);
		else
			attribute = element.getAttribute(aname, ns);
		
		return attribute;
		
	}
	
}
