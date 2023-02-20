package kb.esb;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import util.JDOMUtil;

public class MigrationBizTx extends MigrationUtil {


	/**
	 * 
	 * @param fullPackage
	 * @param bizTxValueMap
	 * @return
	 */
	public static File makeBizTx(String fullPackage, Map<String, String> bizTxValueMap) {
		
		File parentPath = new File(BIZTX_WORK_PATH + File.separator + MigrationUtil.pakcageChange(PKG_ROOT));
		if( !parentPath.exists() )
			parentPath.mkdirs();
		
//		System.out.println( "########" + parentPath );
//		System.out.println( "########" + fullPackage );
		String[] bizTxPath = fullPackage.split("\\.");
		for (int i = 2; i < bizTxPath.length; i++) {
			
			File file = new File(parentPath + File.separator + bizTxPath[i]);
			if( !file.exists() )
				file.mkdirs();
			
			createBizTx(file, bizTxValueMap);
			
			parentPath = file;
		}
		
		return parentPath;
	}
	
	
	/**
	 * Leaf 없는 거래 만들기
	 * 
	 * @param fullPackage
	 * @return
	 */
	public static File makeBizTx(String fullPackage) {
		
		String[] bizTxPath = fullPackage.split("\\.");
		File parentPath = new File(BIZTX_WORK_PATH + File.separator + bizTxPath[0]);
		if( !parentPath.exists() )
			parentPath.mkdirs();
		
		StringBuffer strBuf = new StringBuffer(bizTxPath[0]);
		for (int i = 1; i < bizTxPath.length; i++) {
			 
			File file = new File(parentPath + File.separator + bizTxPath[i]);
			if( !file.exists() )
				file.mkdirs();
			
			createBizTx(file, null, strBuf.toString(), bizTxPath[i]);
			
			strBuf.append(".").append(bizTxPath[i]);
			parentPath = file;
		}
		
		return parentPath;
		
	}
	
	
	/**
	 * 
	 * 
	 * @param path
	 * @param bizTxValueMap
	 */

	@SuppressWarnings("unused")
	private static void createBizTx(File path, Map<String, String> bizTxValueMap, String ... str) {
		
		/**
			D:\tmax\AnyLink7_Studio\workspace\PROJECT\BIZTX\kb\esb\txPFM
			D:\tmax\AnyLink7_Studio\workspace\PROJECT\BIZTX\kb\esb\txPFM\txCU
			D:\tmax\AnyLink7_Studio\workspace\PROJECT\BIZTX\kb\esb\txPFM\txCU\txEC
			D:\tmax\AnyLink7_Studio\workspace\PROJECT\BIZTX\kb\esb\txPFM\txCU\txEC\txCUECR001
		 */
		String fullPath = path.getAbsolutePath();
		String biztxId = path.getName();
		
		File saveFile = new File(fullPath + File.separator + biztxId + FILE_EX_BIZTX);
		if( saveFile.exists() ) {
//			System.out.println("[있음] " + saveFile);
//			return;
		}
			
		try {
			
			/**
			 * 패키지 정보 없음
			 * 
			 * bizTxValueMap.get("bizTxName");
			 * bizTxValueMap.get("flowId");
			 * bizTxValueMap.get("messageType");
			 * bizTxValueMap.get("requestMessage");
			 * bizTxValueMap.get("responseMessage");
			 */
			String rootPackage = null;
			String nodeTypeValue = null;
			String bizTxName = null;
			if( bizTxValueMap == null ) {
				System.out.println(">>>>>>>> 프로프레임 메시지 거래||" + path);
				rootPackage = str[0];
				bizTxName = str[1];
				
//				System.out.println(rootPackage + "||" + rootPackage.split("\\.").length);
				if( rootPackage.split("\\.").length == 1 )
					nodeTypeValue = BIZTX_ROOT;
				else
					nodeTypeValue = BIZTX_MIDDLE;

			} else {
				rootPackage = PKG_ROOT;
				System.out.println(">>>>>>>> 일반거래||" + path);
				bizTxName = "[" + biztxId.substring(2) + "] " + bizTxValueMap.get("bizTxName");
				
				if( path.getParentFile().getName().indexOf("tx") < 0 ) {
					nodeTypeValue = BIZTX_ROOT;
				} else if( biztxId.matches("tx.*[0-9][0-9][0-9]") ) {
					nodeTypeValue = BIZTX_LEAF;
				} else {
					nodeTypeValue = BIZTX_MIDDLE;
				}
			}
			
			
			Document document = new SAXBuilder().build(READ_BIZTX_FILE);
			Element rootEle = document.getRootElement();
			
			Namespace ns0 = rootEle.getNamespace("ns0");
			
			Element sysId = JDOMUtil.getElement(document, "ns0:sysId", ns0);
			Element id = JDOMUtil.getElement(document, "ns0:id", ns0);
			Element name = JDOMUtil.getElement(document, "ns0:name", ns0);
			Element bizTxPackage = JDOMUtil.getElement(document, "ns0:package", ns0);
			Element nodeType = JDOMUtil.getElement(document, "ns0:nodeType", ns0);
			nodeType.setText(nodeTypeValue);
			
			String parentIdValue = null;
			String sysIdValue = null;
			if( nodeTypeValue.equals(BIZTX_ROOT) ) {
				
				sysIdValue = rootPackage + "." + biztxId;
				parentIdValue = rootPackage;
				
			} else {
				
				/**
				 * package == parentId
				 * sysId = package + id
				 */
				parentIdValue = path.getParent();
				parentIdValue = parentIdValue.substring(parentIdValue.indexOf("BIZTX") + 6).replace("\\", ".");
				sysIdValue = parentIdValue + "." + biztxId;
				
				Element parentId = new Element("parentId", ns0);
				rootEle.addContent(parentId);
				parentId.setText(parentIdValue);

				/**
				 * Leaf는 callService/serviceId 필수
				 */
				if( nodeTypeValue.equals(BIZTX_LEAF) ) {
//				   <ns0:requestMessage>
//				      <ns0:messageId>com.test:CuEcR002ReqData.umsg</ns0:messageId>
//				      <ns0:typeId>com.test:CuEcR002ReqData.umsg</ns0:typeId>
//				      <ns0:isArray>false</ns0:isArray>
//				   </ns0:requestMessage>
//				   <ns0:responseMessage>
//				      <ns0:messageId>com.test:CuEcR002ResData.umsg</ns0:messageId>
//				      <ns0:typeId>com.test:CuEcR002ResData.umsg</ns0:typeId>
//				      <ns0:isArray>false</ns0:isArray>
//				   </ns0:responseMessage>
					
					String messageType = bizTxValueMap.get("messageType");
					String requestMessageValue =  bizTxValueMap.get("requestMessage");
					String responseMessageValue = bizTxValueMap.get("responseMessage");
					
					if( requestMessageValue != null ) {
						Element requestMessage = JDOMUtil.createElement(rootEle, "requestMessage", ns0);
						Element requestMessageId = JDOMUtil.createElement(requestMessage, "messageId", ns0, String.format("%s:%s%s", sysIdValue, requestMessageValue, FILE_EX_UMSG));
						Element requestTypeId = JDOMUtil.createElement(requestMessage, "typeId", ns0, String.format("%s:%s%s%s", sysIdValue, requestMessageValue, messageType, FILE_EX_UMSG));
						Element requestIsArray = JDOMUtil.createElement(requestMessage, "isArray", ns0, "false");
					}
					
					if( responseMessageValue != null ) {
						Element responseMessage = JDOMUtil.createElement(rootEle, "responseMessage", ns0);
						Element responseMessageId = JDOMUtil.createElement(responseMessage, "messageId", ns0, String.format("%s:%s%s", sysIdValue, responseMessageValue, FILE_EX_UMSG));
						Element responseTypeId = JDOMUtil.createElement(responseMessage, "typeId", ns0, String.format("%s:%s%s%s", sysIdValue, responseMessageValue, messageType, FILE_EX_UMSG));
						Element responseIsArray = JDOMUtil.createElement(responseMessage, "isArray", ns0, "false");
					}
					
					
//					<ns0:callService>
//						<ns0:serviceId>kb.esb.txPFM.txCU.txEC.txCUECR001:CUECR001_SvcFlow:CUECR001_SvcFlow_Start</ns0:serviceId>
//						<ns0:asyncResponse>false</ns0:asyncResponse>
//					</ns0:callService>
					String flowId = bizTxValueMap.get("flowId");
					
					Element callService = JDOMUtil.createElement(rootEle, "callService", ns0);
					
					Element serviceId = JDOMUtil.createElement(callService, "serviceId", ns0, 
							String.format("%s:%s:%s", sysIdValue, flowId, flowId + "_" + ACT_NAME.START.getName()));
					
					Element asyncResponse = JDOMUtil.createElement(callService, "asyncResponse", ns0, "false");

				}
				
				/**
				 * 거래 타임아웃 삭제(상위 따름)
				 */
				rootEle.removeChild("businessTimeout", ns0);
			}
			
			sysId.setText(sysIdValue);
			id.setText(biztxId);
			name.setText(bizTxName);
			bizTxPackage.setText(parentIdValue);
			
			
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
//				xmlOutput.output(document, System.out);   
			
			xmlOutput.output(document, new FileWriter(saveFile));
//			System.out.println(saveFile);
		
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
}
