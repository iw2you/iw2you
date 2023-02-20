
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import kb.esb.MigrationUtil;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class PB_DTO_withXPath extends MigrationUtil {
	
	public static void main(String[] args) {

		File path = new File("D:/tmax/ProBusStudio_hsteel/workspace/LIG_ESB_LGS_CU/CommonResources/kr/co/lig/cu/ec/r002");
		String rootPath = "D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/com/test/";
		
		try {
			
			String rootPackage = "com.test";
			
			File[] fileList = path.listFiles();
			for (File file : fileList) {
				
				if( file.getName().endsWith(".dto") ) {
					System.out.println( file );
					
				
					// DTO  ===========================================================================
					Document dtoDocument = new SAXBuilder().build(file);
					Element dtoRootEle = dtoDocument.getRootElement();
					
					Namespace nsDto = dtoRootEle.getNamespace("str");
					if( nsDto == null )
						nsDto = dtoRootEle.getNamespace();
					
					Attribute attrPhysicalName = dtoRootEle.getAttribute("physicalName");
					String physicalName = attrPhysicalName.getValue().replace("Str", "");
					
					attrPhysicalName.setValue(physicalName);
					Attribute attrResourcePath = dtoRootEle.getAttribute("resourcePath");
					Attribute attrResourceId = dtoRootEle.getAttribute("resourceId");
					
//					String resourcePath = attrResourcePath.getValue().replaceAll("/", ".");
					String resourcePath = rootPackage;
					
					String fileName = physicalName + ".umsg";
					String resourceId = resourcePath + ":" + fileName.replace("Str", "");
					
					attrResourcePath.setValue(resourcePath);
					attrResourceId.setValue(resourceId);
					
					// DTO : MessageType 확인 ===========================================================================
					String oldMessagePhysicalName = null;
					String messagePhysicalName = null;
					Element messageInfo = dtoRootEle.getChild("messageInfo", nsDto);
					if( messageInfo != null ) {
						Attribute attr = messageInfo.getAttribute("messageType");
						
						Attribute attrMessagePath = messageInfo.getAttribute("messagePath");
						Attribute attrMessagePhysicalName = messageInfo.getAttribute("messagePhysicalName");
						Attribute attrMessageId = messageInfo.getAttribute("messageId");
						
//						String msgResourcePath = attrMessagePath.getValue().replaceAll("/", ".");
						String msgResourcePath = rootPackage;
						
						String messageType = attr.getValue();
						messagePhysicalName = physicalName + messageType;
						
						oldMessagePhysicalName = attrMessagePhysicalName.getValue().replace("Str", "");
						
						attrMessagePhysicalName.setValue(messagePhysicalName);
						attrMessagePath.setValue(msgResourcePath);
						attrMessageId.setValue(msgResourcePath + ":" + fileName);
					}
					
					
					
					// Umsg 생성 ===========================================================================
					Element umessage = new Element("umessage", NS_MES);
					
					// Umsg 생성 > DTO 추가 ===========================================================================
					Element umsgStructure = new Element( dtoRootEle.getName(), NS_MES );
					umessage.addContent(umsgStructure);
					
					List<Attribute> dtoAttrs = dtoRootEle.getAttributes();
					for (Attribute attribute : dtoAttrs) {
						
						String name = attribute.getName();
						String value = attribute.getValue();

						umsgStructure.setAttribute(name, value);
					}
					
					List<Element> str = dtoRootEle.getChildren("structureField", nsDto);
					for (Element element : str) {
						
						Element field = new Element( element.getName(), NS_STR );
						JDOMUtil.copyElement(element, field);
						umsgStructure.addContent(field);
						
						String filedType = field.getAttributeValue("fieldType");
						if( "include".equals(filedType) ) {
							
							// includeStructurePath="kr/co/lig/cu/ec/r001"	includeStructureName="IdcustRlnmCnfmInDTOStr"	referenceId="{urn:probus:struct:kr.co.lig.cu.ec.r001}IdcustRlnmCnfmInDTOStr"
							// includeStructurePath="com.test" 				includeStructureName="MSG_DATA" 				referenceId="com.test:MSG_DATA.umsg"
							field.setAttribute("includeStructurePath", resourcePath);
							
							Attribute includeStructureName = field.getAttribute("includeStructureName");
							String includeStructureNameValue = includeStructureName.getValue().replace("Str", "");
							includeStructureName.setValue(includeStructureNameValue);
							
							Attribute referenceId = field.getAttribute("referenceId");
							referenceId.setValue(resourcePath + ":" + includeStructureNameValue + FILE_EX_MSG);
							
						}
						
					}
					
					
					// Msg 변환 ===========================================================================
					if( messageInfo != null ) {
						File msgFile = new File(file.getParentFile().getAbsolutePath() + File.separator + oldMessagePhysicalName + ".msg");
						if( msgFile.exists() && msgFile.isFile() ) {
							
							Element dtoMsgInfo = dtoRootEle.getChild("messageInfo", nsDto);
							Element msgInfo = new Element( dtoMsgInfo.getName(), NS_STR );
							JDOMUtil.copyElement(dtoMsgInfo, msgInfo);
							umsgStructure.addContent(msgInfo);
							
							
							Document msgDocument = new SAXBuilder().build(msgFile);
							Element message = msgDocument.getRootElement();
							Namespace nsMsg = message.getNamespace();
							
							/*
							 *	<mes:message physicalName="TCP_CLOSE_BODYMsgFld" logicalName="TCP_CLOSE_BODY" 
							 *		structureId="{urn:probus:struct:kr.or.kidi.aos.common}TCP_CLOSE_BODY" 
							 *		structurePath="kr/or/kidi/aos/common" structureName="TCP_CLOSE_BODY" 
							 *		resourcePath="kr/or/kidi/aos/common" resourceType="MESSAGE" resourceGroup="" 
							 *		resourceId="{urn:probus:message:kr.or.kidi.aos.common}TCP_CLOSE_BODYMsgFld" 
							 *		messageType="FixedLength" trimFlag="rtrim" 
							 *		elementName="xml-fragment" xmlns:mes="http://www.tmaxsoft.co.kr/proframe/message">
							 */
							Attribute attrMsgPhysicalName = message.getAttribute("physicalName");
							Attribute attrMsgLogicalName = message.getAttribute("logicalName");
							Attribute attrMsgStructureId = message.getAttribute("structureId");
							Attribute attrMsgStructurePath = message.getAttribute("structurePath");
							Attribute attrMsgStructureName = message.getAttribute("structureName");
							Attribute attrMsgResourcePath = message.getAttribute("resourcePath");
							Attribute attrMsgResourceType = message.getAttribute("resourceType");
							Attribute attrMsgResourceId = message.getAttribute("resourceId");
							Attribute attrMsgMessageType = message.getAttribute("messageType");
							Attribute attrMsgElementName = message.getAttribute("elementName");
							
							String messageType = attrMsgMessageType.getValue();
							
							attrMsgPhysicalName.setValue(messagePhysicalName.replace("Str", ""));
							attrMsgLogicalName.setValue(physicalName.replace("Str", ""));
							attrMsgStructureId.setValue(resourceId);
							attrMsgStructurePath.setValue(resourcePath);
							attrMsgStructureName.setValue(attrMsgLogicalName.getValue());
							attrMsgResourcePath.setValue(attrMsgStructurePath.getValue());
							attrMsgResourceId.setValue(attrMsgStructureId.getValue());
							
							
							// Umsg 생성 > Msg 추가 ===========================================================================
							Element umsgMessage = new Element( message.getName(), NS_MES );
							JDOMUtil.copyElement(message, umsgMessage);
							umessage.addContent(umsgMessage);
							
							List<Attribute> msgAttrs = message.getAttributes();
							for (Attribute attribute : msgAttrs) {
								
								String name = attribute.getName();
								String value = attribute.getValue();
								
								umsgMessage.setAttribute(name, value);
							}
							
							
							// XML일 경우, Namespace 설정 필요
							if( messageType.equals("XML") ) {
								Attribute elementName = JDOMUtil.getAttribute(message, "elementName");
								String elementNameValue = elementName.getValue();
								String prefix = elementNameValue.substring(0, elementNameValue.indexOf(":"));
								
								Namespace ns = message.getNamespace(prefix);
								umsgMessage.addNamespaceDeclaration(ns);
								
								umsgMessage.setAttribute("complexTypeName", elementNameValue);
							}
							
							
							List<Element> msgfield = message.getChildren("messageField", nsMsg);
							for (Element element : msgfield) {
								
								Element field = new Element( element.getName(), NS_MES );
								JDOMUtil.copyElement(element, field);
								umsgMessage.addContent(field);
								
								if( messageType.equals("XML") ) {
									field.addNamespaceDeclaration( NS_XS );
								}
								
								String filedType = field.getAttributeValue("fieldType");
								if( "include".equals(filedType) ) {
									
									// includeStructurePath="kr/co/lig/cu/ec/r001"	includeStructureName="IdcustRlnmCnfmInDTOStr"	referenceId="{urn:probus:struct:kr.co.lig.cu.ec.r001}IdcustRlnmCnfmInDTOStr"
									// includeStructurePath="com.test" 				includeStructureName="MSG_DATA" 				referenceId="com.test:MSG_DATA.umsg"
									// referenceId="kr.co.lig.cu.ec.r002:InetMbrInfoDTOStr.umsg" referenceStructureName="InetMbrInfoDTOStr"
									
									Attribute referenceStructureName = field.getAttribute("referenceStructureName");
									String includeStructureNameValue = referenceStructureName.getValue().replace("Str", "");
									referenceStructureName.setValue(includeStructureNameValue);

									Attribute includeStructurePath = field.getAttribute("includeMessagePath");
									String includeStructurePathValue = includeStructurePath.getValue().replace("/", ".");
									includeStructurePath.setValue(includeStructurePathValue );

									Attribute referenceId = field.getAttribute("referenceId");
									referenceId.setValue(includeStructurePathValue + ":" + includeStructureNameValue + FILE_EX_MSG);
									
								}
								
								
							}
						}
					}

					Document document = new Document(umessage);
					
					File saveFile = new File(rootPath + File.separator + fileName);
					System.out.println( "\t ==> " + saveFile );
					saveFile.getParentFile().mkdirs();
					
					XMLOutputter xmlOutput = new XMLOutputter();
					xmlOutput.setFormat(Format.getPrettyFormat());
//					xmlOutput.output(dtoDocument, System.out);
//					xmlOutput.output(msgDocument, System.out);
//					xmlOutput.output(document, System.out);
					
					xmlOutput.output(document, new FileWriter(saveFile));
				}
			}
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
}
