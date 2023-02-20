

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class PB_Flow_withXPath {

	private static final Namespace NS_NEW_TMAX = Namespace.getNamespace("tmax", "http://www.tmaxsoft.com/anylink/XPDL20/");
	private static final Namespace NS_XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	
	public static void main(String[] args) {

		File readFile = new File("C:/Users/wegen/Downloads/LIG_ESB_LGS_CU/CU_SG/CUECR001_SvcFlow.sfdl");
		File saveFile = new File("D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/com/test/FLW_TEST.sfdl");
		
		
		try {
			
			String newPackage = "com.test";
			String newFlowName = "FLW_TEST";
			
			int YCoordinate = 107;
			int startX = 107;
			
			int evtWidth = 36;
			int width = 100;
			int height = 50;
			int margin = 40;
			
			int[] xarr = {
					startX, 
					startX + evtWidth + margin,
					startX + evtWidth + margin + width + margin, 
					startX + evtWidth + margin + width + margin + width + margin
				};
			
			Document document = new SAXBuilder().build(readFile);
			Element rootEle = document.getRootElement();
			
			Namespace nsXpdl = rootEle.getNamespace("xpdl");
			Namespace nsTmax = rootEle.getNamespace("tmax");
			
			rootEle.addNamespaceDeclaration(NS_XSI);
			
			JDOMUtil.getElement(document, "xpdl:Pool", nsXpdl).getAttribute("Process").setValue(newFlowName);
			
			Element workflowProcess = JDOMUtil.getElement(document, "xpdl:WorkflowProcess", nsXpdl);
			workflowProcess.getAttribute("Id").setValue(newFlowName);
			workflowProcess.getAttribute("Name").setValue(newFlowName);
			workflowProcess.getAttribute("Package", nsTmax).setValue(newPackage);
			
			
			List<Element> dataFieldList = JDOMUtil.getElements(document, "xpdl:DataField", nsXpdl);
			for(Element dataField : dataFieldList) {
				
				Element attrs = dataField.getChild("ExtendedAttributes", dataField.getNamespace());
				Element attr = attrs.getChild("ExtendedAttribute", attrs.getNamespace());
				Element var = attr.getChildren().get(0);
				
				var.getAttribute("scope").setValue("instance");
				
				/*
				ProBus ====================
					messageID="{urn:probus:struct:kr.or.kidi.aos.common}TB_AOS_CLOSE_DATA"
					messageClassName="kr.or.kidi.aos.common.TB_AOS_CLOSE_DATA"
				
				AnyLink7 ====================
					messageID="kr.or.kidi.aos.common:TB_AOS_CLOSE_DATA.umsg"
					messageClassName="kr.or.kidi.aos.common.TB_AOS_CLOSE_DATA"
				*/
				Attribute attrMsgId = var.getAttribute("messageID");
				Attribute attrMsgClassName = var.getAttribute("messageClassName");

				String msgClsName = attrMsgClassName.getValue();
				int idx = msgClsName.lastIndexOf(".");
				
				StringBuffer strBufMsgId = new StringBuffer();
//				strBufMsgId.append( msgClsName.substring(0, idx) ).append(":");
				strBufMsgId.append( newPackage ).append(":");
				strBufMsgId.append( msgClsName.substring(idx + 1) ).append(".umsg");

				StringBuffer strBufMsgClsName = new StringBuffer();
				strBufMsgClsName.append( newPackage ).append(".");
				strBufMsgClsName.append( msgClsName.substring(idx + 1) );
			
				attrMsgId.setValue( strBufMsgId.toString() );
				attrMsgClassName.setValue( strBufMsgClsName.toString() );
				
			}
			
			List<Element> activityList = JDOMUtil.getElements(document, "xpdl:Activity", nsXpdl);
			for(Element activity : activityList) {

				/*
				 * _START
				 * _HTTP
				 * _RPL
				 * _END
				*/
				String actName = activity.getAttributeValue("Id");
				if( actName.endsWith("_HTTP") ) {
					
					Element tExAttr = activity.getChild("ExtendedAttributes", nsXpdl).
							getChild("ExtendedAttribute", nsXpdl).
							getChildren().get(0);
					tExAttr.getAttribute("type").setValue("HTTP");

				}
				
				
				Element nodeGraphicsInfo = activity.getChild("NodeGraphicsInfos", nsXpdl).getChild("NodeGraphicsInfo", nsXpdl);
				Element cooridinates = nodeGraphicsInfo.getChild("Coordinates", nsXpdl);

				Attribute attrHeight = nodeGraphicsInfo.getAttribute("Height");
				if( attrHeight == null ) {
					// 이벤트
					cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(YCoordinate + 7) );
					
					if( actName.endsWith("_START") ) {
						
						cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(xarr[0]) );
						
					} else if( actName.endsWith("_END") ) {
						
						cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(xarr[3]) );
						
					}
					
					
				} else {
					
					attrHeight.setValue(String.valueOf(height));
					nodeGraphicsInfo.getAttribute("Width").setValue(String.valueOf(width));
					
					cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(YCoordinate) );
					
					
					if( actName.endsWith("_RPL") ) {
						cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(xarr[2]) );
					} else {
						cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(xarr[1]) );
					}
					
					nodeGraphicsInfo.getAttribute("FillColor").setValue("6384bb");
					nodeGraphicsInfo.getAttribute("fontColor", nsTmax).setValue("3e3f40");
					
				}
				
			}
			
			
			Document newDocument = new Document();
			
			Element newRootEle = new Element("Package", nsXpdl);
			List<Element> chList = rootEle.getChildren();
			for (Element element : chList) {
				
				Element newEle = new Element(element.getName(), element.getNamespace());
				JDOMUtil.copyElementChildren(element, newEle, NS_NEW_TMAX);
				
				newRootEle.addContent(newEle);
			}
			newRootEle.addNamespaceDeclaration(NS_NEW_TMAX);
			newRootEle.addNamespaceDeclaration(NS_XSI);
			newDocument.addContent(newRootEle);
			
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			
//			xmlOutput.output(document, System.out);   
//			xmlOutput.output(newDocument, System.out);   
			
			System.out.println(saveFile);
			xmlOutput.output(newDocument, new FileWriter(saveFile));
			
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
