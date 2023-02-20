package kb.esb;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class MigrationOutRule extends MigrationUtil {


	@SuppressWarnings("unused")
	public static void makeOutRule(String fullPackage, String ruleName, String ruleProtocol, Map<String, String> value) throws IOException {
		
		File saveFile = new File(BIZTX_WORK_PATH + File.separator + MigrationUtil.pakcageChange(fullPackage) + File.separator + ruleName + ".orule");
		
		Document document = new Document();
		
		Namespace ns0 = Namespace.getNamespace("ns0", "http://www.tmaxsoft.com/schemas/anylink/");
		Namespace ns1 = Namespace.getNamespace("ns1", "http://www.tmaxsoft.com/schemas/anylink/common/");
		Namespace ns2 = Namespace.getNamespace("ns2", "http://www.tmaxsoft.com/schemas/anylink/reliableQueue/");
		Namespace ns3 = Namespace.getNamespace("ns3", "http://www.tmaxsoft.com/schemas/anylink/serverPlugin/");
		
		Element root = new Element("outboundRuleInfo", ns0);
		root.addNamespaceDeclaration(ns0);
		root.addNamespaceDeclaration(ns1);
		root.addNamespaceDeclaration(ns2);
		root.addNamespaceDeclaration(ns3);
		
		Element sysId = JDOMUtil.createElement(root, "sysId", ns0, value.get("sysId"));
		Element id = JDOMUtil.createElement(root, "id", ns0, ruleName);
		Element name = JDOMUtil.createElement(root, "name", ns0, ruleName);
		Element rulePackage = JDOMUtil.createElement(root, "package", ns0, fullPackage);
		Element version = JDOMUtil.createElement(root, "version", ns0, "0");
		Element resourceType = JDOMUtil.createElement(root, "resourceType", ns0, RESOURCE_OUT_RULE);
		
		
		/**
		 * 요청/응답 메시지 설정
		 */
		setMessage(root, "requestMessage", ns0, fullPackage, value);
		setMessage(root, "responseMessage", ns0, fullPackage, value);
		Element useResponseHeaderMessage = JDOMUtil.createElement(root, "useResponseHeaderMessage", ns0, "false");
		
		
		Element adapterId = JDOMUtil.createElement(root, "adapterId", ns0, value.get("adapterId"));
		Element endpointId = JDOMUtil.createElement(root, "endpointId", ns0, value.get("endpointId"));
		
		Element requestTimeout = JDOMUtil.createElement(root, "requestTimeout", ns0, value.get("requestTimeout"));
		Element protocol = JDOMUtil.createElement(root, "protocol", ns0, ruleProtocol);
		
		if( PROTOCOL_AL_HTTP.equals(ruleProtocol) ) {
			Element httpOutboundRuleInfo = JDOMUtil.createElement(root, "httpOutboundRuleInfo", ns0);
			Element callType = JDOMUtil.createElement(httpOutboundRuleInfo, "callType", ns0, value.get("callType"));
			Element httpMethod = JDOMUtil.createElement(httpOutboundRuleInfo, "httpMethod", ns0, value.get("httpMethod"));
			Element contentType = JDOMUtil.createElement(httpOutboundRuleInfo, "contentType", ns0, value.get("contentType"));
		} else if( PROTOCOL_AL_PFM.equals(ruleProtocol) ) {
			Element proFrameOutboundRuleInfo = JDOMUtil.createElement(root, "proFrameOutboundRuleInfo", ns0);
			Element appName = JDOMUtil.createElement(proFrameOutboundRuleInfo, "appName", ns0, value.get("appName"));
			Element svcName = JDOMUtil.createElement(proFrameOutboundRuleInfo, "svcName", ns0, value.get("svcName"));
			Element fnName = JDOMUtil.createElement(proFrameOutboundRuleInfo, "fnName", ns0, value.get("fnName"));
		}
		document.addContent(root);
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());
//			xmlOutput.output(document, System.out);   
		
		if( !saveFile.getParentFile().exists() )
			saveFile.getParentFile().mkdirs();
		
		xmlOutput.output(document, new FileWriter(saveFile));
		System.out.println(saveFile);
	}


	/**
	 * @param newPackage
	 * @param value
	 * @param ns
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unused")
	private static void setMessage(Element root, String cname, Namespace ns, String newPackage, Map<String, String> value) {
		
		Element element = JDOMUtil.createElement(root, cname, ns);
		
		String requestMessageValue = value.get(cname);
		Element requestMessageId = JDOMUtil.createElement(element, "messageId", ns, newPackage + ":" + requestMessageValue + FILE_EX_UMSG);
		
		
		String messageType = value.get("messageType");
		/**
		 * 메시지 타입이 없을 경우엔, "Default Type" 설정
		 */
		String reqUumsgFile = null;
		if( messageType == null ) {
			reqUumsgFile = DEFAULT_TYPE;
		} else {
			reqUumsgFile = requestMessageValue + messageType + FILE_EX_MSG;
		}
		Element requestTypeId = JDOMUtil.createElement(element, "typeId", ns, newPackage + ":" + reqUumsgFile);
		Element requestIsArray = JDOMUtil.createElement(element, "isArray", ns, "false");
		
	}
	
}
