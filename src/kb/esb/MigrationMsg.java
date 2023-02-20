package kb.esb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class MigrationMsg extends MigrationUtil {
	
	public static void makeMessage(Map<String, File> dtoFileMap, String messageFileName, String fullPackage) {
		
		MigrationMsg.convertMessage(dtoFileMap, messageFileName, fullPackage, null);
		
	}

	public static void makeMessage(String messageFileName, String fullPackage) {
		
		MigrationMsg.convertMessage(null, messageFileName, fullPackage, null);
		
	}
	
	/**
	 * 
	 * @param dtoFile
	 * @param messageFileName
	 * @param fullPackage
	 */
	public static void makeMessage(Map<String, File> dtoFileMap, String messageFileName, String fullPackage, String protocol) {
		
		MigrationMsg.convertMessage(dtoFileMap, messageFileName, fullPackage, protocol);
		
	}
	
	
	public static void convertMessage(Map<String, File> dtoFileMap, String messageFileName, String fullPackage, String protocol) {
		
		try {
			File dtoFile = dtoFileMap.get(messageFileName + FILE_EX_DTO);
//			System.out.println("======" + dtoFile);
			
			StringBuffer strBuf = new StringBuffer(BIZTX_WORK_PATH);
			strBuf.append(File.separator).append(MigrationUtil.pakcageChange(fullPackage));
			strBuf.append(File.separator).append(MigrationUtil.valueChange(messageFileName, protocol));
			strBuf.append(FILE_EX_UMSG);
			
			File umsgFile = new File(strBuf.toString());
			if( umsgFile.exists() ) {
//				System.out.println("return");
//				return;
			}
			
//			System.out.println("######" + umsgFile);
			// DTO  ===========================================================================
			Document dtoDocument = new SAXBuilder().build(dtoFile);
			Element dtoRootEle = dtoDocument.getRootElement();
			
			Namespace nsDto = dtoRootEle.getNamespace("str");
			if( nsDto == null )
				nsDto = dtoRootEle.getNamespace();
			
			Attribute physicalName = dtoRootEle.getAttribute("physicalName");
			String physicalNameValue = MigrationUtil.valueChange( physicalName.getValue(), protocol );
			physicalName.setValue(physicalNameValue);

			Attribute logicalName = dtoRootEle.getAttribute("logicalName");
//			logicalName.setValue(  );
			
			Attribute attrResourcePath = dtoRootEle.getAttribute("resourcePath");
			Attribute attrResourceId = dtoRootEle.getAttribute("resourceId");
			
	//		String resourcePath = attrResourcePath.getValue().replaceAll("/", ".");
			String resourcePath = fullPackage;
			
//			String fileName = physicalName + ".umsg";
//			String resourceId = resourcePath + ":" + fileName.replace("Str", "");
			String fileName = umsgFile.getName();
			String resourceId = resourcePath + ":" + fileName;
			
			attrResourcePath.setValue(resourcePath);
			attrResourceId.setValue(resourceId);
			
			// DTO : MessageType 확인 ===========================================================================
			String oldMessagePhysicalName = null;
			String messagePhysicalName = null;
			String messageIdValue = null;
			Element messageInfo = dtoRootEle.getChild("messageInfo", nsDto);
			if( messageInfo != null ) {
				Attribute attr = messageInfo.getAttribute("messageType");
				
				Attribute attrMessagePath = messageInfo.getAttribute("messagePath");
				Attribute attrMessagePhysicalName = messageInfo.getAttribute("messagePhysicalName");
				Attribute attrMessageId = messageInfo.getAttribute("messageId");
				
	//			String msgResourcePath = attrMessagePath.getValue().replaceAll("/", ".");
				String msgResourcePath = resourcePath;
				
				String messageType = attr.getValue();
				messagePhysicalName = physicalNameValue + messageType;
				
				oldMessagePhysicalName = MigrationUtil.valueChange( attrMessagePhysicalName.getValue(), protocol );
				
				attrMessagePhysicalName.setValue(messagePhysicalName);
				attrMessagePath.setValue(msgResourcePath);
				
				messageIdValue = msgResourcePath + ":" + messagePhysicalName + FILE_EX_MSG;
				attrMessageId.setValue(messageIdValue);
//				System.out.println("==============" + messageIdValue);
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
			
			if( protocol != null && PROTOCOL_PB_PFM.equals(protocol) ) {
				umsgStructure.setAttribute("superClassName", "proframe.dto.AbstractPfmDataObject");
			}
			List<Element> str = dtoRootEle.getChildren("structureField", nsDto);
			for (Element element : str) {
				
				Element field = new Element( element.getName(), NS_STR );
				JDOMUtil.copyElement(element, field);
				umsgStructure.addContent(field);
				
				String filedType = field.getAttributeValue("fieldType");
//				System.out.println("filedType==" + filedType);
				if( "include".equals(filedType) ) {
					
					// includeStructurePath="kr/co/lig/cu/ec/r001"	includeStructureName="IdcustRlnmCnfmInDTOStr"	referenceId="{urn:probus:struct:kr.co.lig.cu.ec.r001}IdcustRlnmCnfmInDTOStr"
					// includeStructurePath="com.test" 				includeStructureName="MSG_DATA" 				referenceId="com.test:MSG_DATA.umsg"
					field.setAttribute("includeStructurePath", resourcePath);
					
					Attribute includeStructureName = field.getAttribute("includeStructureName");
					String includeStructureNameValue = includeStructureName.getValue();
					
//					System.out.println(includeStructureNameValue);
					MigrationMsg.makeMessage(dtoFileMap, includeStructureNameValue, fullPackage);
					
					includeStructureNameValue = MigrationUtil.valueChange( includeStructureNameValue, protocol );
					
					includeStructureName.setValue(includeStructureNameValue);
					
					Attribute referenceId = field.getAttribute("referenceId");
					referenceId.setValue(resourcePath + ":" + includeStructureNameValue + FILE_EX_UMSG);
					
				}
				
			}
			
			
			// Msg 변환 ===========================================================================
			if( messageInfo != null ) {
				File msgFile = new File(dtoFile.getParentFile() + File.separator + oldMessagePhysicalName + FILE_EX_MSG);
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
					Attribute attrMsgResourceId = message.getAttribute("resourceId");
					Attribute attrMsgMessageType = message.getAttribute("messageType");
					
					String messageType = attrMsgMessageType.getValue();
					
					attrMsgPhysicalName.setValue(MigrationUtil.valueChange( messagePhysicalName, protocol ));
					attrMsgLogicalName.setValue( attrMsgPhysicalName.getValue() );
					attrMsgStructureId.setValue(resourceId);
					attrMsgStructurePath.setValue(resourcePath);
					attrMsgStructureName.setValue(attrMsgLogicalName.getValue());
					attrMsgResourcePath.setValue(attrMsgStructurePath.getValue());
					attrMsgResourceId.setValue(messageIdValue);
//					attrMsgResourceId.setValue(attrMsgStructureId.getValue());
					
					
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
							
							// includeStructureName
							Attribute referenceStructureName = field.getAttribute("referenceStructureName");
							String includeStructureNameValue = MigrationUtil.valueChange( referenceStructureName.getValue(), protocol );
							referenceStructureName.setValue(includeStructureNameValue + protocol);
	
							// includeStructurePath
							Attribute includeStructurePath = field.getAttribute("includeMessagePath");
							String includeStructurePathValue = includeStructurePath.getValue().replace("/", ".");
							includeStructurePath.setValue(includeStructurePathValue );
	
							Attribute referenceId = field.getAttribute("referenceId");
							referenceId.setValue(includeStructurePathValue + ":" + includeStructureNameValue + FILE_EX_UMSG);
							
						}
						
						
					}
				}
			}

		
			Document document = new Document(umessage);
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
//			xmlOutput.output(document, System.out);
			
			umsgFile.getParentFile().mkdirs();
			xmlOutput.output(document, new FileWriter(umsgFile));
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		
	}
	
}
