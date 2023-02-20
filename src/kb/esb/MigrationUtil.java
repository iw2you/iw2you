package kb.esb;

import java.io.File;

import org.jdom2.Namespace;

public class MigrationUtil {

	public final static File READ_BIZTX_FILE = new File("D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/com/test/test.biztx");
	
	public static final String BIZTX_WORK_PATH	= "D:/tmax/AnyLink7_Studio/workspace/PROJECT/BIZTX/";
	
	public static final String PKG_ROOT			= "kb.esb";

	public static final String FILE_EX_BIZTX		= ".biztx";
	public static final String FILE_EX_FLOW			= ".sfdl";
	public static final String FILE_EX_UMSG			= ".umsg";
	public static final String FILE_EX_DTO			= ".dto";
	public static final String FILE_EX_MSG			= ".msg";
	public static final String FILE_EX_MAP			= ".map";

	public static final String PROTOCOL_PB_HTTP 	= "HTTPADAPTER"; 	// HTTPADAPTER
	public static final String PROTOCOL_AL_HTTP 	= "HTTP"; 			// HTTP

	public static final String PROTOCOL_PB_SAP 		= "SAPADAPTER";		// SAPADAPTER
	public static final String PROTOCOL_AL_SAP 		= "SAP";			// SAP
	
	public static final String PROTOCOL_PB_WS 		= "WEBSERVICE";		// WEBSERVICE
	public static final String PROTOCOL_AL_WS 		= "WS";				// WEBSERVICE
	
	public static final String PROTOCOL_PB_PFM 		= "PROFRAME";		// PROFRAME
	public static final String PROTOCOL_AL_PFM 		= "PFM";			// PROFRAME
	
	public static final String FLOW_NAME					= "_SvcFlow";
	public static final String DEFAULT_TYPE					= "Default Type";
	public static final String RESOURCE_OUT_RULE		= "OutboundRule";

	public static final String BIZTX_ROOT 		= "Root";
	public static final String BIZTX_MIDDLE 	= "Middle";
	public static final String BIZTX_LEAF 		= "Leaf";
	
	public static final Namespace NS_XS 		= Namespace.getNamespace("xs", "http://www.w3.org/2001/XMLSchema");
	public static final Namespace NS_NEW_XSI 	= Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	
	public static final Namespace NS_NEW_TMAX 	= Namespace.getNamespace("tmax", "http://www.tmaxsoft.com/anylink/XPDL20/");
	public static final Namespace NS_MES 		= Namespace.getNamespace("mes", "http://www.tmaxsoft.com/promapper/message");
	public static final Namespace NS_STR 		= Namespace.getNamespace("str", "http://www.tmaxsoft.com/promapper/structure");
	
	
	
	public enum ACT_NAME {
		START("Start"),
		MAP_REQ("MAP_REQ"),
		MAP_RES("MAP_RES"),
		RPL("RPL"),
		END("End"),
		ERROR_START("ErrorStart"),
		ERROR_MAP("ErrorMapping"),
		ERROR_RPL("ErrorRPL"),
		ERROR_END("ErrorEnd");
		
		private final String name;
		
		public String getName() { 
			return name; 
		}
		
		private ACT_NAME(String name){ 
			this.name = name; 
		} 
	};
	
	public enum ACT_OLD_NAME {
		START("_Message"),
		RPL("_ReplyMessage"),
		END("_None"),
		ERROR_START("Error"),
		ERROR_MAP("ErrorMapping");
		
		final private String name;
		public String getName() { 
			return name; 
		}
		
		private ACT_OLD_NAME(String name){ 
			this.name = name; 
		} 
	};

	
	/**
	 * Str 문자열 제거
	 * 
	 * @param value
	 * @return
	 */
	public static String valueChange(String value, String protocol) {
		
		if( PROTOCOL_PB_PFM.equals( protocol ) || PROTOCOL_AL_PFM.equals( protocol ) )
			return value;
		else
			return value.replace("Str", "");
		
	}

	/**
	 * 패키지 구분자를 경로 구분자로 변경
	 * . -> /로 변경
	 * 
	 * @param value
	 * @return
	 */
	public static String pakcageChange(String value) {
		return value.replace(".", "/");
	}
	
}
