package kb.esb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
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

public class MigrationFlow extends MigrationUtil {

	private final static int startX = 107;
	private final static int startY = 107;
	
	private final static int evtWidth = 36;
	/**
	 * 이벤트 액티비티와 일반 액티비티의 높이가 다름
	 * 동일 선상에 두려면 +7 하면 맞음
	 */
	private final static int evtHeight = 7;
	
	private final static int width = 100;
	private final static int height = 50;
	
	// 액티비티 간 간격
	private final static int marginWidth = 40;
	private final static int marginHeight = 80;
	
	private final static int[] XCoordinates = {
		startX, 														// 0 - 시작, 		에러 이벤트
		startX + evtWidth + marginWidth, 								// 1 - 매핑 요청, 	에러 매핑
		startX + evtWidth + marginWidth + (width + marginWidth) * 1,	// 2 - HTTP, 		에러 응답
		startX + evtWidth + marginWidth + (width + marginWidth) * 2,	// 3 - 매핑 응답, 	비정상 종료
		startX + evtWidth + marginWidth + (width + marginWidth) * 3,	// 4 - 응답
		startX + evtWidth + marginWidth + (width + marginWidth) * 4,	// 5 - 종료
	};
	
	
	private final static String VAR_ERROR_CODE = "errorCode";
	private final static String VAR_ERROR_STRING = "errorString";
	private final static String VAR_HTTP_URL = "httpURL";
	
	/**
	 * 플로우 Migration 진행(ProBus 5 -> AnyLink 7)
	 * 
	 * @param readFile
	 * @param newFlowId
	 * @param fullPackage
	 */
	public static void migrationFlow(File readFile, String newFlowId, String fullPackage, 
			File bizTxPath, String oruleSysId, Map<String, String> flowValueMap, Map<String, File> dtoFileMap) {
		
		try {
			
			String inProtocol = flowValueMap.get("inProtocol");
			String outProtocol = null;
			
			Element errorReplyActivity = null;
			
			Document document = new SAXBuilder().build(readFile);
			Element root = document.getRootElement();
			
			Namespace nsXpdl = root.getNamespace("xpdl");
			Namespace nsTmax = root.getNamespace("tmax");
			
			root.addNamespaceDeclaration(NS_NEW_XSI);
			
			JDOMUtil.getElement(document, "xpdl:Pool", nsXpdl).setAttribute("Process", newFlowId);
			
			Element workflowProcess = JDOMUtil.getElement(document, "xpdl:WorkflowProcess", nsXpdl);
			workflowProcess.setAttribute("Id", newFlowId);
			workflowProcess.setAttribute("Name", newFlowId);
			workflowProcess.setAttribute("Package", fullPackage, nsTmax);
				
			// Process Exception Handler 삭제
			workflowProcess.removeChild("ExtendedAttributes", nsXpdl);
			
			// RedefinableHeader 삭제
			workflowProcess.removeChild("RedefinableHeader", nsXpdl);
			

			List<Element> activityList = JDOMUtil.getElements(document, "xpdl:Activity", nsXpdl);
			for(Element activity : activityList) {

				Attribute attrId = activity.getAttribute("Id");
				
				String actName = attrId.getValue();
				if( actName.endsWith(ACT_OLD_NAME.START.getName()) ) {
					attrId.setValue( newFlowId + "_" + ACT_NAME.START.getName() );
					
					List<Element> exAttrList = activity.getChild("ExtendedAttributes", nsXpdl).getChildren();
					for (Element exAttr : exAttrList) {

						String attrValue = exAttr.getAttributeValue("Name");
						if( "MessageEventExtendedAttribute".equals( attrValue ) ) {
							
							Element tActExAttr = exAttr.getChild("EventExtendedAttribute", nsTmax);
							tActExAttr.getChild("mappingInfo", nsTmax).removeContent();
						}
					}
					
				} else if( actName.endsWith(ACT_OLD_NAME.RPL.getName()) ) {
					attrId.setValue( newFlowId + "_" + ACT_NAME.RPL.getName() );

					Element taskActExtAttr = activity.getChild("ExtendedAttributes", nsXpdl).
							getChild("ExtendedAttribute", nsXpdl).getChild("TaskActivityExtendedAttribute", nsTmax);
					taskActExtAttr.setAttribute("requestNodeId", newFlowId + "_" + ACT_NAME.START.getName());
					
					// 에러 응답 액티비티
					errorReplyActivity = makeErrorReply(newFlowId, nsXpdl, nsTmax, activity);
					
				} else if( actName.endsWith(ACT_OLD_NAME.END.getName()) ) {
					attrId.setValue( newFlowId + "_" + ACT_NAME.END.getName() );
					
				} else if( actName.endsWith(ACT_OLD_NAME.ERROR_START.getName()) ) {
					attrId.setValue( newFlowId + "_" + ACT_NAME.ERROR_START.getName() );
					
					// 에러 이벤트 - IntermediateEvent 삭제
					Element eleEvent = activity.getChild("Event", nsXpdl);
					eleEvent.removeChild("IntermediateEvent", nsXpdl);
					
					// 에러 이벤트 - Start 추가
					Element addStartEvent = new Element("StartEvent", nsXpdl);
					eleEvent.addContent(addStartEvent);
					addStartEvent.setAttribute("Trigger", "Error");
					
					Element addResultError = new Element("ResultError", nsXpdl);
					addStartEvent.addContent(addResultError);
					addResultError.setAttribute("ErrorCode", "All");
					
				} else if( actName.endsWith(ACT_OLD_NAME.ERROR_MAP.getName()) ) {
					attrId.setValue( newFlowId + "_" + ACT_NAME.ERROR_MAP.getName() );
					
				} else {
				
					// 아웃바운드 룰 액티비티 설정
					outProtocol = outboundActivity(newFlowId, outProtocol, root, nsXpdl, nsTmax, activity, attrId, fullPackage, oruleSysId);
					
				}
				
				// XY 좌표 재설정
				cooridinatesSetting(startY, evtHeight, width, height,
						marginHeight, XCoordinates, nsXpdl, nsTmax, activity, actName);
				
			}
			
			// DataField 설정
			List<Element> dataFieldList = dataFieldSetting(fullPackage, document, nsXpdl, inProtocol, outProtocol);
			
			/**
			 * 1. 변수명 변경
			 * 2. 변수 리스트 엘리먼트 변경 및 삭제
			 */
			changeInOutVariableList(document, nsTmax);
			

			// 오류 응답 매핑 추가
			Element activities = JDOMUtil.getElement(document, "xpdl:Activities", nsXpdl);
			activities.addContent(errorReplyActivity);
			
			// 요청/응답 매핑 추가
			activities.addContent( addMapping(newFlowId, "REQ", nsXpdl, String.valueOf(XCoordinates[1]), String.valueOf(startY)) );
			activities.addContent( addMapping(newFlowId, "RES", nsXpdl, String.valueOf(XCoordinates[3]), String.valueOf(startY)) );
			
			// 비정상 종료 이벤트 추가
			addErrorEnd(newFlowId, document, nsXpdl);
			
			// Transitions 그리기
			drawTransitions(newFlowId, document, nsXpdl, outProtocol);
			
			// 변수 정렬
			modifyDataFields(document, nsXpdl, nsTmax, dataFieldList, inProtocol, outProtocol, fullPackage, dtoFileMap);
			
			// 플로우 저장
			saveNewFlow(newFlowId, root, nsXpdl, bizTxPath);
			
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
	}



	/**
	 * 응답 액티비티를 복사 해서 에러 응답 액티비티 추가
	 * 
	 * @param newFlowId
	 * @param nsXpdl
	 * @param nsTmax
	 * @param activity
	 * @return
	 */
	private static Element makeErrorReply(String newFlowId, Namespace nsXpdl,
			Namespace nsTmax, Element activity) {
		
		Element errorReplyActivity = activity.clone();
		errorReplyActivity.setAttribute("Id", newFlowId + "_" + ACT_NAME.ERROR_RPL.getName());
		errorReplyActivity.setAttribute("Name", ACT_NAME.ERROR_RPL.getName());
		
		Element nodeGraphicsInfo = errorReplyActivity.getChild("NodeGraphicsInfos", nsXpdl).getChild("NodeGraphicsInfo", nsXpdl);
		Element cooridinates = nodeGraphicsInfo.getChild("Coordinates", nsXpdl);
		
		nodeGraphicsInfo.getAttribute("FillColor").setValue("6384bb");
		nodeGraphicsInfo.getAttribute("fontColor", nsTmax).setValue("3e3f40");
		
		Attribute attrHeight = nodeGraphicsInfo.getAttribute("Height");
		attrHeight.setValue(String.valueOf(height));
		nodeGraphicsInfo.getAttribute("Width").setValue(String.valueOf(width));
		
		cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[2]) );
		cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY + marginHeight) );
		
		return errorReplyActivity;
	}



	/**
	 * XY 좌표 설정
	 * 
	 * @param startY
	 * @param evtHeight
	 * @param width
	 * @param height
	 * @param marginHeight
	 * @param XCoordinates
	 * @param nsXpdl
	 * @param nsTmax
	 * @param activity
	 * @param actName
	 */
	private static void cooridinatesSetting(int startY, int evtHeight,
			int width, int height, int marginHeight, int[] XCoordinates,
			Namespace nsXpdl, Namespace nsTmax, Element activity, String actName) {
		
		Element nodeGraphicsInfo = activity.getChild("NodeGraphicsInfos", nsXpdl).getChild("NodeGraphicsInfo", nsXpdl);
		Element cooridinates = nodeGraphicsInfo.getChild("Coordinates", nsXpdl);

		Attribute attrHeight = nodeGraphicsInfo.getAttribute("Height");
		if( attrHeight == null ) {
			
			// 이벤트
			if( actName.endsWith(ACT_OLD_NAME.START.getName()) ) {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[0]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY + evtHeight) );
				
			} else if( actName.endsWith(ACT_OLD_NAME.ERROR_START.getName()) ) {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[0]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY + marginHeight + evtHeight) );
				
			} else if( actName.endsWith(ACT_OLD_NAME.END.getName()) ) {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[5]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY + evtHeight) );
			
			}
			
		} else {
			
			attrHeight.setValue(String.valueOf(height));
			nodeGraphicsInfo.getAttribute("Width").setValue(String.valueOf(width));
			
			if( actName.endsWith(ACT_OLD_NAME.ERROR_MAP.getName()) ) {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[1]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY + marginHeight) );
			
			} else if( actName.endsWith(ACT_OLD_NAME.RPL.getName()) ) {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[4]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY) );
			
			} else {
				cooridinates.getAttribute("XCoordinate").setValue( String.valueOf(XCoordinates[2]) );
				cooridinates.getAttribute("YCoordinate").setValue( String.valueOf(startY) );
			
			}
			
			nodeGraphicsInfo.getAttribute("FillColor").setValue("6384bb");
			nodeGraphicsInfo.getAttribute("fontColor", nsTmax).setValue("3e3f40");
			
		}
	}



	/**
	 * 아웃바운드 액티비티 설정
	 * 
	 * @param newFlowId
	 * @param outProtocol
	 * @param rootEle
	 * @param nsXpdl
	 * @param nsTmax
	 * @param activity
	 * @param attrId
	 * @param oruleSysId
	 * @return
	 */
	private static String outboundActivity(String newFlowId, String outProtocol, Element rootEle, 
			Namespace nsXpdl, Namespace nsTmax, Element activity, Attribute attrId, String fullPackage, String oruleSysId) {
		
		Attribute type = null;
		
		List<Element> exAttrList = activity.getChild("ExtendedAttributes", nsXpdl).getChildren();
		for (Element exAttr : exAttrList) {

			/**
			 * 프로프레임 액티비티일 경우엔, ExtendedAttribute가 2개 임
			 * 	ProframeExtendedAtrribute, TaskActivityExtendedAttribute
			 * 
			 */
			String attrValue = exAttr.getAttributeValue("Name");
			Element tActExAttr = exAttr.getChild(attrValue, nsTmax);
			if( "ProframeExtendedAtrribute".equals( attrValue ) ) {
				
				String appName = tActExAttr.getAttributeValue("applicationName");
				String svcName = tActExAttr.getAttributeValue("serviceName");
				String opName = tActExAttr.getAttributeValue("opName");
//							System.out.println(String.format("[ProframeWAS] - %s_%s_%s", appName, svcName, opName));
				
				Element artifacts = new Element("Artifacts", nsXpdl);
				rootEle.addContent(artifacts);

				Element artifact = new Element("Artifact", nsXpdl);
				artifacts.addContent(artifact);
				artifact.setAttribute("annotationType", "Note", NS_NEW_TMAX);
				artifact.setAttribute("Id", "Note_" + newFlowId);
				artifact.setAttribute("Name", "Note_" + newFlowId);
				artifact.setAttribute("ArtifactType", "Annotation");
				
				Element annotationDescription = new Element("AnnotationDescription", NS_NEW_TMAX);
				artifact.addContent(annotationDescription);
				
				String text = String.format("Application Name : %s\n"
						+ "Service Name : %s\n"
						+ "Operation Name : %s", appName, svcName, opName);
				annotationDescription.setText(text);

				Element nodeGraphicsInfos = new Element("NodeGraphicsInfos", nsXpdl);
				artifact.addContent(nodeGraphicsInfos);

				Element nodeGraphicsInfo = new Element("NodeGraphicsInfo", nsXpdl);
				nodeGraphicsInfos.addContent(nodeGraphicsInfo);
				nodeGraphicsInfo.setAttribute("fontColor", "000000", NS_NEW_TMAX);
				nodeGraphicsInfo.setAttribute("parentId", newFlowId, NS_NEW_TMAX);
				nodeGraphicsInfo.setAttribute("FillColor", "ffffe1");
				nodeGraphicsInfo.setAttribute("Height", "90");
				nodeGraphicsInfo.setAttribute("Width", "200");
				
				Element coordinates = new Element("Coordinates", nsXpdl);
				nodeGraphicsInfo.addContent(coordinates);
				coordinates.setAttribute("XCoordinate", "856");
				coordinates.setAttribute("YCoordinate", "53");

			} else if( "TaskActivityExtendedAttribute".equals( attrValue ) ) {
				
				type = tActExAttr.getAttribute("type");
				if( type != null ) {
					String typeValue = type.getValue();
					
					if( typeValue.contains(PROTOCOL_PB_HTTP) ) {
						typeValue = PROTOCOL_AL_HTTP;
					} else if( typeValue.contains(PROTOCOL_PB_SAP) ) {
						typeValue = PROTOCOL_AL_SAP;
					} else if( typeValue.contains(PROTOCOL_PB_WS) ) {
						typeValue = PROTOCOL_AL_WS;
					} else if( typeValue.contains(PROTOCOL_PB_PFM) ) {
						typeValue = PROTOCOL_AL_PFM;
					}
					type.setValue(typeValue);
					
					
					outProtocol = typeValue;
					if( typeValue.contains(PROTOCOL_PB_PFM) )
						outProtocol = PROTOCOL_AL_PFM;
					attrId.setValue( newFlowId + "_" + outProtocol );
					
				}
				

				/**
				 * 아웃바운드 룰 설정
				 * - Attribute 변경 : serviceid -> serviceId
				 */
				tActExAttr.removeAttribute("serviceid");
				tActExAttr.setAttribute("serviceId", oruleSysId);
				
				tActExAttr.removeChildren("inputMapping", nsTmax);
				tActExAttr.removeChildren("outputMapping", nsTmax);
				
				
				if( PROTOCOL_AL_HTTP.equals(outProtocol) ) {
					
					tActExAttr.setAttribute("useHttpUrlSplitPath", "false");
					
					Element etceteraMapping = JDOMUtil.createElement(tActExAttr, "etceteraMapping", nsTmax);
					etceteraMapping.setAttribute("owner", "MINE");
					
					Element resourceClassName = JDOMUtil.createElement(etceteraMapping, "resourceClassName", nsTmax);
					resourceClassName.setText(newFlowId + "_" + outProtocol + "_ETC");
	
					Element resourceId = JDOMUtil.createElement(etceteraMapping, "resourceId", nsTmax);
					resourceId.setText(fullPackage + ":" + resourceClassName.getText() + FILE_EX_MAP);
	
					Element resourcePath = JDOMUtil.createElement(etceteraMapping, "resourcePath", nsTmax);
					resourcePath.setText(fullPackage);

					Element resourceContent = JDOMUtil.createElement(etceteraMapping, "resourceContent", nsTmax);
					
					Element map = JDOMUtil.createElement(resourceContent, "map");
					Namespace nsMap = Namespace.getNamespace("map", "http://www.tmaxsoft.com/promapper/map");
					map.setNamespace(nsMap);
					
					map.setAttribute("resourceType", "MAP");
					map.setAttribute("resourcePath", fullPackage);
					map.setAttribute("physicalName", resourceClassName.getText());
					map.setAttribute("logicalName", resourceId.getText());
					

					String srcVar = VAR_HTTP_URL;
					srcVar = "context" + srcVar.substring(0, 1).toUpperCase() + srcVar.substring(1) + "_SRC";
					
					Element input = JDOMUtil.createElement(map, "input", nsMap);
					input.setAttribute("alias", srcVar);
					input.setAttribute("arraySize", "0");
					input.setAttribute("logicalName", VAR_HTTP_URL);
					input.setAttribute("physicalName", VAR_HTTP_URL);
//					input.removeNamespaceDeclaration(nsMap);

					Element variableInput = JDOMUtil.createElement(input, "variable", nsMap);
					variableInput.setAttribute("name", VAR_HTTP_URL);
					variableInput.setAttribute("scope", "context");
					variableInput.setAttribute("size", "0");

					Element primitiveInput = JDOMUtil.createElement(variableInput, "primitive", nsMap);
					primitiveInput.setText("String");
					

					String tgtVar = "contextURL_TGT";

					Element output = JDOMUtil.createElement(map, "output", nsMap);
					output.setAttribute("alias", tgtVar);
					output.setAttribute("arraySize", "0");
					output.setAttribute("logicalName", "URL");
					output.setAttribute("physicalName", "URL");
//					output.removeNamespaceDeclaration(nsMap);
					
					Element variableOutpu = JDOMUtil.createElement(output, "variable", nsMap);
					variableOutpu.setAttribute("name", "URL");
					variableOutpu.setAttribute("scope", "context");
					variableOutpu.setAttribute("size", "0");
					
					Element primitiveOutput = JDOMUtil.createElement(variableOutpu, "primitive", nsMap);
					primitiveOutput.setText("String");

					Element assign = JDOMUtil.createElement(map, "assign", nsMap);
					assign.setAttribute("fromExpression", srcVar);
					assign.setAttribute("toExpression", tgtVar);

					Element fromField = JDOMUtil.createElement(assign, "fromField", nsMap);
					fromField.setAttribute("physicalName", VAR_HTTP_URL);
					fromField.setAttribute("logicalName", VAR_HTTP_URL);
					fromField.setAttribute("fieldType", "String");
					
					Element toField = JDOMUtil.createElement(assign, "toField", nsMap);
					toField.setAttribute("physicalName", "URL");
					toField.setAttribute("logicalName", "URL");
					toField.setAttribute("fieldType", "String");
					
				}
				
			}
		}
		
		

		return outProtocol;
	}



	/**
	 * @param fullPackage
	 * @param document
	 * @param nsXpdl
	 * @return
	 */
	private static List<Element> dataFieldSetting(String fullPackage, Document document, Namespace nsXpdl, 
			String inProtocol, String outProtocol) {
		
		List<Element> dataFieldList = JDOMUtil.getElements(document, "xpdl:DataField", nsXpdl);
		for(Element dataField : dataFieldList) {
			
			processVariableChange(dataField);
			
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
			
			String dataFieldId = dataField.getAttributeValue("Id");
			if( ( PROTOCOL_PB_PFM.equals( inProtocol ) && ( dataFieldId.equals("vInReq") || dataFieldId.equals("vInRes") )
					|| ( PROTOCOL_AL_PFM.equals( outProtocol ) && ( dataFieldId.equals("vOutReq") || dataFieldId.equals("vOutRes") ) ) ) ) {
				
				attrMsgId.setValue( msgClsName + FILE_EX_UMSG );
				attrMsgClassName.setValue( msgClsName );
				
			} else {
				int idx = msgClsName.lastIndexOf(".");

				StringBuffer strBufMsgId = new StringBuffer();
				strBufMsgId.append( fullPackage ).append(":");
				strBufMsgId.append( msgClsName.substring(idx + 1) ).append( FILE_EX_UMSG );
				
				
				StringBuffer strBufMsgClsName = new StringBuffer();
				strBufMsgClsName.append( fullPackage ).append(".");
				strBufMsgClsName.append( msgClsName.substring(idx + 1) );
			
				attrMsgId.setValue( strBufMsgId.toString() );
				attrMsgClassName.setValue( strBufMsgClsName.toString() );
			
			}
			
		}
		
		return dataFieldList;
	}
	


	/**
	 * 신규 플로우 저장
	 * 
	 * @param newFlowName
	 * @param rootEle
	 * @param nsXpdl
	 * @throws IOException
	 */
	private static void saveNewFlow(String newFlowName, Element rootEle, Namespace nsXpdl, File bizTxFile) throws IOException {
		
		File saveFile = new File(bizTxFile + File.separator + newFlowName + FILE_EX_FLOW);
		
		Document newDocument = new Document();
		
		Element newRootEle = new Element("Package", nsXpdl);
		List<Element> chList = rootEle.getChildren();
		for (Element element : chList) {
			
			Element newEle = new Element(element.getName(), element.getNamespace());
			JDOMUtil.copyElementChildren(element, newEle, NS_NEW_TMAX);
			
			newRootEle.addContent(newEle);
		}
		newRootEle.addNamespaceDeclaration(NS_NEW_TMAX);
		newRootEle.addNamespaceDeclaration(NS_NEW_XSI);
		newDocument.addContent(newRootEle);
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
		
//			xmlOutput.output(document, System.out);   
//			xmlOutput.output(newDocument, System.out);   
		
//		if( !saveFile.getParentFile().exists() )
//			saveFile.getParentFile().mkdirs();
		
		System.out.println("-----------------------" + saveFile);
		xmlOutput.output(newDocument, new FileWriter(saveFile));
		System.out.println(saveFile);
	}


	/**
	 * 변수 순서 조정<br>
	 * 
	 * vInReq, vInRes<br>
	 * -- pfmHeader, sysHeader<br>
	 * vOutReq, vOutRes
	 * 
	 * @param document
	 * @param nsXpdl
	 * @param dataFieldList
	 * @param outProtocol
	 */
	private static Element modifyDataFields(Document document, Namespace nsXpdl, Namespace nsTmax,
			List<Element> dataFieldList, String inProtocol, String outProtocol, String fullPackage, Map<String, File> dtoFileMap) {
		
		Map<Integer, Element> dataFieldMap = new HashMap<Integer, Element>();
		for (Element dataField : dataFieldList) {
			
			Element cloneDataField = dataField.clone();
			String dataFieldId = cloneDataField.getAttributeValue("Id");
			
//			System.out.println( dataFieldId + "||" + outProtocol );
			if( dataFieldId.equals("vInReq") ) {
				dataFieldMap.put(1, cloneDataField);
			
			} else if( dataFieldId.equals("vInRes") ) {
				dataFieldMap.put(2, cloneDataField);
				
			} else if( dataFieldId.equals("vOutReq") ) {
				
				if( PROTOCOL_AL_PFM.equals( outProtocol ) )
					dataFieldMap.put(5, cloneDataField);
				else
					dataFieldMap.put(3, cloneDataField);
				
			} else if( dataFieldId.equals("vOutRes") ) {

				if( PROTOCOL_AL_PFM.equals( outProtocol ) )
					dataFieldMap.put(6, cloneDataField);
				else
					dataFieldMap.put(4, cloneDataField);
			
			} else if( dataFieldId.equals("vPfmHeader") ) {
				
				dataFieldMap.put(3, cloneDataField);

			} else if( dataFieldId.equals("vSysHeader") ) {

				dataFieldMap.put(4, cloneDataField);

			}
			
		}
		
//		System.out.println("=============================");
		Element dataFields = JDOMUtil.getElement(document, "xpdl:DataFields", nsXpdl);
		
		// 기존 DataField 삭제
		dataFields.removeContent();
		
		
		/**
		 * 기본 Variable 추가
		 * - errorCode, errorString
		 * - HTTP 사용시 httpUrl
		 */
		addVariable(dataFields, nsXpdl, nsTmax, VAR_ERROR_CODE);
		addVariable(dataFields, nsXpdl, nsTmax, VAR_ERROR_STRING);
		
		if( PROTOCOL_AL_HTTP.equals( outProtocol ) )
			addVariable(dataFields, nsXpdl, nsTmax, VAR_HTTP_URL);
			
		
		// DataField 순서대로 추가
		for(int idxFd = 1; idxFd <= dataFieldMap.size(); idxFd++ ) {

			Element dataField = dataFieldMap.get(idxFd);
			
			Element extendedAttributes = JDOMUtil.getChild(dataField, "ExtendedAttributes", nsXpdl);
			Element extendedAttribute = JDOMUtil.getChild(extendedAttributes, "ExtendedAttribute", nsXpdl);
			Element variableExtendedAttribute = JDOMUtil.getChild(extendedAttribute, "VariableExtendedAttribute", nsTmax);
			
			
			Attribute messageID = variableExtendedAttribute.getAttribute("messageID");
			Attribute messageClassName = variableExtendedAttribute.getAttribute("messageClassName");
			
			String messageClassNameValue = messageClassName.getValue();
			int idx = messageClassNameValue.lastIndexOf(".");
			
			String dataFieldId = dataField.getAttributeValue("Id");
			if( PROTOCOL_PB_PFM.equals( inProtocol ) ) {
				
				String messageClassPackage = messageClassNameValue.substring(0, idx);
				messageClassNameValue = messageClassNameValue.substring(idx + 1);
//				System.out.println("============" + messageClassPackage);
//				System.out.println("============" + messageClassNameValue);
				
				MigrationBizTx.makeBizTx(messageClassPackage);
				
				if( dataFieldId.equals("vInReq") || dataFieldId.equals("vInRes") ) {
					
					MigrationMsg.makeMessage(dtoFileMap, messageClassNameValue, messageClassPackage, inProtocol);
				
				} else if( dataFieldId.equals("vOutReq") || dataFieldId.equals("vOutRes") ) {
					
					MigrationMsg.makeMessage(dtoFileMap, messageClassNameValue, messageClassPackage, outProtocol);

				}
				
				messageID.setValue(String.format("%s:%s%s", messageClassPackage, MigrationUtil.valueChange(messageClassNameValue, inProtocol), FILE_EX_UMSG));
				messageClassName.setValue(String.format("%s.%s", messageClassPackage, MigrationUtil.valueChange(messageClassNameValue, inProtocol)));
				
			} else {
				
				messageClassNameValue = messageClassNameValue.substring( idx );
				
				messageID.setValue(String.format("%s:%s%s", fullPackage, MigrationUtil.valueChange(messageClassNameValue, inProtocol), FILE_EX_UMSG));
				messageClassName.setValue(String.format("%s.%s", fullPackage, MigrationUtil.valueChange(messageClassNameValue, inProtocol)));
				
			}
			
			dataFields.addContent(dataField);
		}
		
		return dataFields;
	}


	private static void addVariable(Element dataFields, Namespace nsXpdl, Namespace nsTmax, String ename) {
		
		Element addDataField = JDOMUtil.createElement(dataFields, "DataField", nsXpdl);
		addDataField.setAttribute("Id", ename);
		addDataField.setAttribute("Name", ename);
		addDataField.setAttribute("visible", "true", nsTmax);
		
		Element dataType = JDOMUtil.createElement(addDataField, "DataType", nsXpdl);
		Element basicType = JDOMUtil.createElement(dataType, "BasicType", nsXpdl);
		basicType.setAttribute("Type", "STRING");

		Element extendedAttributes = JDOMUtil.createElement(addDataField, "ExtendedAttributes", nsXpdl);
		Element extendedAttribute = JDOMUtil.createElement(extendedAttributes, "ExtendedAttribute", nsXpdl);
		extendedAttribute.setAttribute("Name", "VariableExtendedAttribute");

		Element variableExtendedAttribute = JDOMUtil.createElement(extendedAttribute, "VariableExtendedAttribute", nsTmax);
		variableExtendedAttribute.setAttribute("scope", "instance");
		variableExtendedAttribute.setAttribute("messageClassName", "");
		variableExtendedAttribute.setAttribute("messageID", "");
		
	}
	
	
	/**
	 * 매핑 액티비티 추가
	 * 
	 * @param newFlowName
	 * @param reqRes
	 * @param nsXpdl
	 * @param x
	 * @param y
	 * @return
	 */
	private static Element addMapping(String newFlowName, String reqRes, Namespace nsXpdl, String x, String y) {
		
		Element addMap = new Element("Activity", nsXpdl);
		addMap.setAttribute("Id", 	String.format("%s_%s_%s", newFlowName, "MAP", reqRes));
		addMap.setAttribute("Name",	String.format("%s_%s", "MAP", reqRes));
		addMap.setAttribute("StartMode", "Automatic");
		addMap.setAttribute("FinishMode", "Automatic");
		
		Element addDescription = new Element("Description", nsXpdl);
		addMap.addContent(addDescription);
		addDescription.setText("Activity");

		Element addImplementation = new Element("Implementation", nsXpdl);
		addMap.addContent(addImplementation);

		Element addTask = new Element("Task", nsXpdl);
		addImplementation.addContent(addTask);

		Element addTaskManual = new Element("TaskManual", nsXpdl);
		addTask.addContent(addTaskManual);

		
		Element addPriority = new Element("Priority", nsXpdl);
		addMap.addContent(addPriority);
		addPriority.setText("50");

		Element addExtendedAttributes = new Element("ExtendedAttributes", nsXpdl);
		addMap.addContent(addExtendedAttributes);

		Element addExtendedAttribute = new Element("ExtendedAttribute", nsXpdl);
		addExtendedAttributes.addContent(addExtendedAttribute);
		addExtendedAttribute.setAttribute("Name", "TaskActivityExtendedAttribute");
		
		Element addTaskActivityExtendedAttribute = new Element("TaskActivityExtendedAttribute", NS_NEW_TMAX);
		addExtendedAttribute.addContent(addTaskActivityExtendedAttribute);
		addTaskActivityExtendedAttribute.setAttribute("noLogging", "false");
		addTaskActivityExtendedAttribute.setAttribute("checkPoint", "false");
		addTaskActivityExtendedAttribute.setAttribute("savePoint", "false");
		addTaskActivityExtendedAttribute.setAttribute("type", "MAPPING");
		addTaskActivityExtendedAttribute.setAttribute("taskMode", "Mapping");
		
		Element addInputVariableList = new Element("inputVariableList", NS_NEW_TMAX);
		addTaskActivityExtendedAttribute.addContent(addInputVariableList);

		Element addInVariableId = new Element("variableId", NS_NEW_TMAX);
		addInputVariableList.addContent(addInVariableId);
		addInVariableId.setText( reqRes.equals("REQ") ? "vInReq" : "vOutRes" );

		Element addOutputVariableList = new Element("outputVariableList", NS_NEW_TMAX);
		addTaskActivityExtendedAttribute.addContent(addOutputVariableList);
		
		Element addOutVariableId = new Element("variableId", NS_NEW_TMAX);
		addOutputVariableList.addContent(addOutVariableId);
		addOutVariableId.setText( reqRes.equals("REQ") ? "vOutReq" : "vInRes" );
		
		Element addNodeGraphicsInfos = new Element("NodeGraphicsInfos", nsXpdl);
		addMap.addContent(addNodeGraphicsInfos);

		Element addNodeGraphicsInfo = new Element("NodeGraphicsInfo", nsXpdl);
		addNodeGraphicsInfos.addContent(addNodeGraphicsInfo);
		addNodeGraphicsInfo.setAttribute("fontColor", "3e3f40", NS_NEW_TMAX);
		addNodeGraphicsInfo.setAttribute("Height", "50");
		addNodeGraphicsInfo.setAttribute("Width", "100");
		addNodeGraphicsInfo.setAttribute("FillColor", "6384bb");
		
		Element addCoordinates = new Element("Coordinates", nsXpdl);
		addNodeGraphicsInfo.addContent(addCoordinates);
		addCoordinates.setAttribute("YCoordinate", y);
		addCoordinates.setAttribute("XCoordinate", x);

		return addMap;
	}

	/**
	 * 1. 변수명 변경<p>
	 * 
	 * 2. 변수 리스트 엘리먼트 변경
	 * <pre>
	 * 변수 리스트 엘리먼트 변경
	 * 		userClassInputVariableList	->	inputVariableList
	 * 		userClassOutputVariableList	->	outputVariableList
	 * 변수 리스트 엘리먼트 삭제
	 * 		userClassInputVariableList
	 * 		userClassOutputVariableList
	 * </pre>
	 * @param document
	 * @param nsTmax
	 */
	private static void changeInOutVariableList(Document document, Namespace nsTmax) {
		
		/**
		 * 변수명 변경
		 */
		List<Element> variableIdList = JDOMUtil.getElements(document, "tmax:variableId", nsTmax);
		for (Element variableId : variableIdList) {
			variableId.setText( variableChange( variableId.getText() ) );
		}
		
		
		// userClassInputVariableList	->	inputVariableList
		List<Element> userClassInputVariableList = JDOMUtil.getElements(document, "tmax:userClassInputVariableList", nsTmax);
		for (Element userClassInputVariable : userClassInputVariableList) {
			
			Element parent = userClassInputVariable.getParentElement();
			Element addInputVariableList = new Element("inputVariableList", NS_NEW_TMAX);
			parent.addContent(addInputVariableList);
			
			List<Element> variableIds = userClassInputVariable.getChildren("variableId", nsTmax);
			for (Element variableId : variableIds) {
				Element addVariableId = new Element("variableId", NS_NEW_TMAX);
				addInputVariableList.addContent(addVariableId);

				JDOMUtil.copyElement(variableId, addVariableId);
			}
			
			// 변수 리스트 엘리먼트 삭제 userClassInputVariableList
			parent.removeChild("userClassInputVariableList", nsTmax);
		}
		
		
		// userClassOutputVariableList	->	outputVariableList
		List<Element> userClassOutputVariableList = JDOMUtil.getElements(document, "tmax:userClassOutputVariableList", nsTmax);
		for (Element userClassOutputVariable : userClassOutputVariableList) {
			
			Element parent = userClassOutputVariable.getParentElement();
			Element addOutputVariableList = new Element("outputVariableList", NS_NEW_TMAX);
			parent.addContent(addOutputVariableList);
			
			List<Element> variableIds = userClassOutputVariable.getChildren("variableId", nsTmax);
			for (Element variableId : variableIds) {
				Element addVariableId = new Element("variableId", NS_NEW_TMAX);
				addOutputVariableList.addContent(addVariableId);
				
				JDOMUtil.copyElement(variableId, addVariableId);
			}
			
			// 변수 리스트 엘리먼트 삭제 userClassOutputVariableList
			parent.removeChild("userClassOutputVariableList", nsTmax);
		}
		
	}

	
	/**
	 * ReqData 		-> vInReq<br>
	 * ResData 		-> vInRes<br>
	 * request_ 	-> vOutReq<br>
	 * response_	-> vOutRes<br>
	 * 
	 * @param value
	 * @return
	 */
	private static String variableChange(String value) {
		
		if( value.endsWith("ReqData") || value.endsWith("InData")) {
			value = "vInReq";
		} else if( value.endsWith("ResData") || value.endsWith("OutData")) {
			value = "vInRes";
		} else if( value.startsWith("request_") || (value.contains("ProframeWAS") && value.endsWith("_in")) ) {
			value = "vOutReq";
		} else if( value.startsWith("response_") || (value.contains("ProframeWAS") && value.endsWith("_out")) ) {
			value = "vOutRes";
		} else if( value.endsWith("pfm_header") ) {
			value = "vPfmHeader";
		} else if( value.endsWith("sys_header") ) {
			value = "vSysHeader";
		}
		
		return value;
	}

	/**
	 * Element의 Attribute 중 Id/Name 변경
	 * 
	 * @param element
	 * @return
	 */
	private static String processVariableChange(Element element) {
		
		Attribute attrId = element.getAttribute("Id");
		Attribute attrName = element.getAttribute("Name");

		String valueId = variableChange( attrId.getValue() );
		
		attrId.setValue( valueId );
		
		if( attrName != null )
			attrName.setValue( attrId.getValue() );
		
		return valueId;
	}
	
	/**
	 * @param newFlowName
	 * @param nsXpdl
	 * @param transitions
	 */
	private static void drawTransition(String newFlowName, Namespace nsXpdl, Element transitions, String from, String to) {
		
		Element eleTransition = new Element("Transition", nsXpdl);
		transitions.addContent(eleTransition);
		eleTransition.setAttribute("Id", String.format("Transition_%s_%s", from, to));
		eleTransition.setAttribute("Name", "Transition");
		eleTransition.setAttribute("priority", "0", NS_NEW_TMAX);
		eleTransition.setAttribute("From", String.format("%s_%s", newFlowName, from));
		eleTransition.setAttribute("To", String.format("%s_%s", newFlowName, to));
	
	}

	/**
	 * 기존 Transition 삭제 후 다시 그리기
	 * 
	 * @param newFlowName
	 * @param nsXpdl
	 * @param document
	 */
	private static void drawTransitions(String newFlowName, Document document, Namespace nsXpdl, String protocol) {
		
		// Transition 전부 지우기
		Element eleTransitions = JDOMUtil.getElement(document, "xpdl:Transitions", nsXpdl);
		eleTransitions.removeContent();
		
		// Start -> MAP_REQ
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.START.getName(), ACT_NAME.MAP_REQ.getName());

		// MAP_REQ -> Outbound
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.MAP_REQ.getName(), protocol);

		// Outbound -> MAP_RES
		drawTransition(newFlowName, nsXpdl, eleTransitions, protocol, ACT_NAME.MAP_RES.getName());

		// MAP_RES -> RPL
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.MAP_RES.getName(), ACT_NAME.RPL.getName());

		// RPL -> END
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.RPL.getName(), ACT_NAME.END.getName());

		// ErrorStart -> ErrorMapping
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.ERROR_START.getName(), ACT_NAME.ERROR_MAP.getName());

		// ErrorMapping -> ErrorRPL
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.ERROR_MAP.getName(), ACT_NAME.ERROR_RPL.getName());

		// ErrorRPL -> ErrorEnd
		drawTransition(newFlowName, nsXpdl, eleTransitions, ACT_NAME.ERROR_RPL.getName(), ACT_NAME.ERROR_END.getName());

	}


	/**
	 * 비정상 종료 이벤트 추가
	 * 
	 * @param newFlowName
	 * @param document
	 * @param nsXpdl
	 */
	private static void addErrorEnd(String newFlowName, Document document, Namespace nsXpdl) {
		
		// Activity
		Element actActivityErrorEnd = new Element("Activity", nsXpdl);
		actActivityErrorEnd.setAttribute("Id", newFlowName + "_" + ACT_NAME.ERROR_END.getName());
		actActivityErrorEnd.setAttribute("Name", "Terminate");
		
		// Activity - Description
		Element eleDescription = new Element("Description", nsXpdl);
		actActivityErrorEnd.addContent(eleDescription);
		eleDescription.setText("Terminate");

		// Activity - Event
		Element eleEvent = new Element("Event", nsXpdl);
		actActivityErrorEnd.addContent(eleEvent);
		// Activity - Event - EndEvent
		Element eleEndEvent = new Element("EndEvent", nsXpdl);
		eleEvent.addContent(eleEndEvent);
		eleEndEvent.setAttribute("Result", "Terminate");

		// Activity - ExtendedAttributes
		Element eleExtendedAttributes = new Element("ExtendedAttributes", nsXpdl);
		actActivityErrorEnd.addContent(eleExtendedAttributes);
		// Activity - ExtendedAttributes - ExtendedAttribute
		Element eleExtendedAttribute = new Element("ExtendedAttribute", nsXpdl);
		eleExtendedAttributes.addContent(eleExtendedAttribute);
		eleExtendedAttribute.setAttribute("Name", "EventExtendedAttribute");
		
		// Activity - ExtendedAttributes - ExtendedAttribute - EventExtendedAttribute
		Element eleEventExtendedAttribute = new Element("EventExtendedAttribute", NS_NEW_TMAX);
		eleExtendedAttribute.addContent(eleEventExtendedAttribute);
		eleEventExtendedAttribute.setAttribute("noLogging", "false");
		eleEventExtendedAttribute.setAttribute("checkPoint", "false");
		eleEventExtendedAttribute.setAttribute("savePoint", "false");
		eleEventExtendedAttribute.setAttribute("type", "internal");
		eleEventExtendedAttribute.setAttribute("autoUnregister", "false");
		
		// Activity - ExtendedAttributes - ExtendedAttribute - EventExtendedAttribute - terminateErrorCause
		Element eleTerminateErrorCause = new Element("terminateErrorCause", NS_NEW_TMAX);
		eleEventExtendedAttribute.addContent(eleTerminateErrorCause);
		eleTerminateErrorCause.setAttribute("errorCause", "LastError");
		
		// Activity - NodeGraphicsInfos
		Element eleNodeGraphicsInfos = new Element("NodeGraphicsInfos", nsXpdl);
		actActivityErrorEnd.addContent(eleNodeGraphicsInfos);
		// Activity - NodeGraphicsInfos - NodeGraphicsInfo
		Element eleNodeGraphicsInfo = new Element("NodeGraphicsInfo", nsXpdl);
		eleNodeGraphicsInfos.addContent(eleNodeGraphicsInfo);
		// Activity - NodeGraphicsInfos - NodeGraphicsInfo - Coordinates
		Element eleCoordinates = new Element("Coordinates", nsXpdl);
		eleCoordinates.setAttribute("XCoordinate", "463");
		eleCoordinates.setAttribute("YCoordinate", "194");
		eleNodeGraphicsInfo.addContent(eleCoordinates);
		
		Element eleActivities = JDOMUtil.getElement(document, "xpdl:Activities", nsXpdl);
		eleActivities.addContent(actActivityErrorEnd);
	}
	
}
