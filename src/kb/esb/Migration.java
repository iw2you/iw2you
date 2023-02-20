package kb.esb;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

import util.JDOMUtil;

public class Migration extends MigrationUtil {

	private final static String SRC_PRJ = "D:/tmax/ProBusStudio_hsteel/workspace/LIG_ESB_LGS_CU";

	private final static String SYS_CODE = "LGS";

	private final static String PB_ADT = "AdapterRuleGroup";
	private final static String PB_COM = "CommonResources";
	private final static String PB_SG = "_SG";
	
	private final static String RULE_IN = "I";
	private final static String RULE_OUT = "O";
	

	public static void main(String[] args) {
		
		
		File adapterRuleGroup = new File(SRC_PRJ, PB_ADT);
		
		HashMap<String, File> inRuleMap = new HashMap<String, File>();
		HashMap<String, File> outRuleMap = new HashMap<String, File>();
		
		File[] ruleFileList = adapterRuleGroup.listFiles();
		for (File file : ruleFileList) {
			String fileName = file.getName();
			
			String sysSrcTgt = fileName.substring(0, fileName.lastIndexOf("_") );
			String direction = fileName.endsWith("o") ? RULE_OUT : RULE_IN;
			
			if( direction.equals(RULE_IN) )
				inRuleMap.put(sysSrcTgt, file);
			else
				outRuleMap.put(sysSrcTgt, file);
		}
		
		
		
		File commonResources = new File(SRC_PRJ, PB_COM);
		
		HashMap<String, File> dtoFileMap = new HashMap<String, File>();
		HashMap<String, File> msgFileMap = new HashMap<String, File>();
		HashMap<String, File> mapFileMap = new HashMap<String, File>();
		
		findFiles(commonResources, dtoFileMap, msgFileMap, mapFileMap);
		
		
		if( outRuleMap.size() > 0 ) {
			
			Iterator<String> itOut = outRuleMap.keySet().iterator();
			while( itOut.hasNext() ) {
				
				System.out.println("####################################################################################");
				String key = itOut.next();
				File outRule = outRuleMap.get(key);
				File inRule = inRuleMap.get(key);
				
				try {
					
					String inProtocol = null;
					String inRuleDirection = null;
					String inRuleName = null;
					String inRuleMessageType = null;
					String inRuleRequestMessage = null;
					String inRuleResponseMessage = null;
					
					if( inRule == null ) {
						
						inProtocol = PROTOCOL_PB_PFM;
						inRuleDirection = "Inbound";
					
					} else {
						
						Document inDocument = new SAXBuilder().build(inRule);
						Element inRoot = inDocument.getRootElement();
						
						Namespace nsInRule = inRoot.getNamespace("rule");
						
						inProtocol = inRoot.getNamespacePrefix();
						Namespace nsInProtocol = inRoot.getNamespace(inProtocol);
						
						inRuleDirection = inRoot.getChild("ruleType", nsInProtocol).getText();
						inRuleName = inRoot.getChild("commonRuleInfo", nsInProtocol).getChild("ruleName", nsInRule).getText();
						
						Element inflow = JDOMUtil.getChild(inRoot, "inflow", nsInProtocol);
						inRuleMessageType = JDOMUtil.getText(inflow, "inputDataType", nsInProtocol);
						
						Element inputDTO = JDOMUtil.getChild(inflow, "inputDTO", nsInProtocol);
						inRuleRequestMessage = JDOMUtil.getText(inputDTO, "physicalName", nsInRule);

						Element outflow = JDOMUtil.getChild(inRoot, "outflow", nsInProtocol);
						Element outputDTO = JDOMUtil.getChild(outflow, "outputDTO", nsInProtocol);
						inRuleResponseMessage = JDOMUtil.getText(outputDTO, "physicalName", nsInRule);

					}
					
					String outProtocol = null;
					String outRuleDirection = null;
					String outRuleName = null;
					String outRuleMessageType = null;
					String outRuleRequestMessage = null;
					String outRuleResponseMessage = null;
					if( outRule == null ) {
						
						outProtocol = PROTOCOL_PB_PFM;
						outRuleDirection = "Outbound";
					
					} else {
						Document outDocument = new SAXBuilder().build(outRule);
						Element outRoot = outDocument.getRootElement();
						
						Namespace nsOutRule = outRoot.getNamespace("rule");
	
						outProtocol = outRoot.getNamespacePrefix();
						Namespace nsOutProtocol = outRoot.getNamespace(outProtocol);
						
						outRuleDirection = outRoot.getChild("ruleType", nsOutProtocol).getText();
						outRuleName = outRoot.getChild("commonRuleInfo", nsOutProtocol).getChild("ruleName", nsOutRule).getText();
						

						Element inflow = JDOMUtil.getChild(outRoot, "inflow", nsOutProtocol);
						outRuleMessageType = JDOMUtil.getText(inflow, "inputDataType", nsOutProtocol);
						
						Element inputDTO = JDOMUtil.getChild(inflow, "inputDTO", nsOutProtocol);
						outRuleRequestMessage = JDOMUtil.getText(inputDTO, "physicalName", nsOutRule);

						Element outflow = JDOMUtil.getChild(outRoot, "outflow", nsOutProtocol);
						Element outputDTO = JDOMUtil.getChild(outflow, "outputDTO", nsOutProtocol);
						outRuleResponseMessage = JDOMUtil.getText(outputDTO, "physicalName", nsOutRule);
					}
					
					String biztxId = key.replace("_", "");
//					System.out.println("===============================================");
//					System.out.println("[" + key + "]");
//					System.out.println("[IN] " + inProtocol + "\t[IN] " + inRuleDirection + "\t[IN] " + inRuleName);
//					System.out.println("[OUT] " + outProtocol + "\t[OUT] " + outRuleDirection + "\t[OUT] " + outRuleName);
					
					
					String[] code = key.split("_");
					
					
					// ROOT 거래의 패키지 포함
					String codeProtocol = outProtocol.toUpperCase();
					String fullPackage = String.format("%s.tx%s_%s.tx%s.tx%s.tx%s", PKG_ROOT, codeProtocol, SYS_CODE, code[0], code[1], biztxId);
					
					// 거래 생성
					String flowId = biztxId + FLOW_NAME;
					
					// 거래 - 패키지 정보 없음
					Map<String, String> bizTxValueMap = new HashMap<String, String>();
					bizTxValueMap.put("sysId", 				fullPackage);
					bizTxValueMap.put("bizTxName", 			biztxId.substring(2));
					bizTxValueMap.put("flowId", 			flowId);
					
					if( inRuleMessageType != null )
						bizTxValueMap.put("messageType", 		inRuleMessageType);
					
					if( inRuleRequestMessage != null ) {
						bizTxValueMap.put("requestMessage", 	inRuleRequestMessage);
						MigrationMsg.makeMessage(dtoFileMap, inRuleRequestMessage, fullPackage, inProtocol);
					}

					if( inRuleResponseMessage != null ) {
						bizTxValueMap.put("responseMessage", 	inRuleResponseMessage);
						MigrationMsg.makeMessage(dtoFileMap, inRuleResponseMessage, fullPackage, inProtocol);
					}
					File bizTxPath = MigrationBizTx.makeBizTx(fullPackage, bizTxValueMap);
					
					// 아웃바운드 룰 - 메시지 만들기
					MigrationMsg.makeMessage(dtoFileMap, outRuleRequestMessage, fullPackage);
					MigrationMsg.makeMessage(dtoFileMap, outRuleResponseMessage, fullPackage);

					// 아웃바운드 룰 - 정보 설정
					String oruleSysId = fullPackage + ":" + outRuleName;
					Map<String, String> outRuleValueMap = new HashMap<String, String>();
					outRuleValueMap.put("sysId", 			oruleSysId);
					outRuleValueMap.put("adapterId", 		"");
					outRuleValueMap.put("endpointId", 		"");
					outRuleValueMap.put("requestMessage", 	MigrationUtil.valueChange( outRuleRequestMessage, codeProtocol ) );
					outRuleValueMap.put("responseMessage", 	MigrationUtil.valueChange( outRuleResponseMessage, codeProtocol ) );
					
					if( PROTOCOL_AL_HTTP.equals(codeProtocol) ) {
						outRuleValueMap.put("messageType", 		outRuleMessageType);
						outRuleValueMap.put("callType", 		"Sync");
						outRuleValueMap.put("httpMethod", 		"Post");
						outRuleValueMap.put("contentType", 		"text/xml");
						outRuleValueMap.put("requestTimeout", 	"7000");
					} else if( PROTOCOL_AL_PFM.equals(codeProtocol) ) {
						outRuleValueMap.put("appName", 			"appNameValue");
						outRuleValueMap.put("svcName", 			"svcNameValue");
						outRuleValueMap.put("fnName", 			"fnNameValue");
						outRuleValueMap.put("requestTimeout", 	"7000");
					}
					MigrationOutRule.makeOutRule(fullPackage, outRuleName, codeProtocol, outRuleValueMap);
					
					
					// 플로우 생성
					Map<String, String> flowValueMap = new HashMap<String, String>();
					flowValueMap.put("inProtocol", inProtocol);

					File pbFlowFile = new File(SRC_PRJ + File.separator + code[0] + PB_SG + File.separator + flowId + FILE_EX_FLOW);
					MigrationFlow.migrationFlow(pbFlowFile, flowId, fullPackage, bizTxPath, oruleSysId, flowValueMap, dtoFileMap);
					
					
				} catch (JDOMException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println("####################################################################################");
				
			}
			
		}
		
	}


	/**
	 * @param dtoFileMap
	 * @param msgFileMap
	 * @param mapFileMap
	 * @param fileList
	 */
	private static void findFiles(File path, 
			HashMap<String, File> dtoFileMap, HashMap<String, File> msgFileMap, 
			HashMap<String, File> mapFileMap) {
		
		for (File file : path.listFiles()) {
			String fileName = file.getName();
			
			if( file.isFile() ) {
			
				if( fileName.endsWith(FILE_EX_DTO) ) {
					dtoFileMap.put(fileName, file);
	
				} else if( fileName.endsWith(FILE_EX_MSG) ) {
					msgFileMap.put(fileName, file);
				
				} else if( fileName.endsWith(FILE_EX_MAP) ) {
					mapFileMap.put(fileName, file);
				
				}
				
			} else {
				
				findFiles(file, dtoFileMap, msgFileMap, mapFileMap);
				
			}
			
		}
	}
	
	
}
