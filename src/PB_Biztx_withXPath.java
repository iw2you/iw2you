

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class PB_Biztx_withXPath {

	public static void main(String[] args) {

		File readFile = new File("D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/com/test/test.biztx");
		String rootPath = "D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/kb/esb/";
		
		try {
			
			String[] arrBizTxId = {"Root", "Middle", "Leaf"};
			
			String rootPackage = "kb.esb";
			String nodeType = arrBizTxId[0];
			
			String newBizTxId = arrBizTxId[2];
			String newBizTxName = newBizTxId;
			
			
			Document document = new SAXBuilder().build(readFile);
			Element rootEle = document.getRootElement();
			
			Namespace ns0 = rootEle.getNamespace("ns0");
			
			Element eleSysId = JDOMUtil.getElement(document, "ns0:sysId", ns0);
			Element eleId = JDOMUtil.getElement(document, "ns0:id", ns0);
			Element eleName = JDOMUtil.getElement(document, "ns0:name", ns0);
			Element elePackage = JDOMUtil.getElement(document, "ns0:package", ns0);
			Element eleNodeType = JDOMUtil.getElement(document, "ns0:nodeType", ns0);
			
			String id = "tx" + newBizTxId;
			String name = "[" + newBizTxName + "]";
			
			String parentId = null;
			String sysId = null;
			if( nodeType.equals("Root") ) {
				sysId = rootPackage + "." + id;
				parentId = rootPackage;
			} else if( nodeType.equals("Leaf") || nodeType.equals("Middle") ) {
				/**
				 * package == parentId
				 * sysId = package + id
				 */
				if( nodeType.equals("Middle") ) {
					parentId = rootPackage + ".tx" + arrBizTxId[0];
					sysId = parentId + "." + id;
				} else if( nodeType.equals("Leaf") ) {
					parentId = rootPackage + ".tx" + arrBizTxId[0] + ".tx" + arrBizTxId[1];
					sysId = parentId + "." + id;
				}
				
				Element addParentId = new Element("parentId", ns0);
				addParentId.setText(parentId);
				rootEle.addContent(addParentId);

				/**
				 * Leaf는 callService/serviceId 필수
				 */
				if( nodeType.equals("Leaf") ) {
					Element addCallService = new Element("callService", ns0);
					Element addServiceId = new Element("serviceId", ns0);
					addCallService.addContent(addServiceId);
					
					rootEle.addContent(addCallService);
				}
			}
			
			eleSysId.setText(sysId);
			eleId.setText(id);
			eleName.setText(name);
			elePackage.setText(parentId);
			
			File saveFile = null;
			switch (nodeType) {
			case "Root":
				eleNodeType.setText("Root");
				saveFile = new File(rootPath + "/" + id + "/" + id + ".biztx");
				
				break;
			case "Middle":
				eleNodeType.setText("Middle");
				saveFile = new File(rootPath + "/tx" + arrBizTxId[0] + "/tx" + arrBizTxId[1] + "/" + id + ".biztx");
				
				break;
			case "Leaf":
				eleNodeType.setText("Leaf");
				saveFile = new File(rootPath + "/tx" + arrBizTxId[0] + "/tx" + arrBizTxId[1] + "/" + id + "/" + id + ".biztx");
				
				break;

			default:
				break;
			}
			
			System.out.println( saveFile );
			saveFile.getParentFile().mkdirs();
			
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
//			xmlOutput.output(document, System.out);   
			
			xmlOutput.output(document, new FileWriter(saveFile));
			
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
