package cz.bliksoft.javautils.net.http;

import java.io.IOException;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpServer;

import cz.bliksoft.javautils.SystemMonitor;
import cz.bliksoft.javautils.freemarker.includes.BuiltinTemplateLoader;
import cz.bliksoft.javautils.logging.LogUtils;
import freemarker.cache.ClassTemplateLoader;

@SuppressWarnings("restriction")
public class SystemReportHTTPHandler extends DefaultFreemarkerHTTPHandler {

	private static final Logger log = Logger.getLogger(SystemReportHTTPHandler.class.getName());

	public SystemReportHTTPHandler() {
		addSupportedGETPOST();
		setTemplateLoader(new ClassTemplateLoader(BuiltinTemplateLoader.class, "builtin"));
		SystemMonitor.startSystemMonitor();
	}

	@Override
	public void handle(BSHttpContext context) throws IOException {

		context.requested = "systemstatus.ftlh";

		context.contextVariables = SystemMonitor.getVariables();

		Object gbc = context.request.get("collectGarbage");
		boolean collectGarbage = false;
		if (gbc != null) {
			collectGarbage = "true".equals(gbc.toString());
		}

		Object save = context.request.get("save");
		boolean saveReport = false;
		if (save != null) {
			saveReport = "true".equals(save.toString());
			if (saveReport) {
				Object name = context.request.get("reportname");
				String reportName = (name != null ? name.toString() : "fromWeb");
				SystemMonitor.logSystemReport(reportName);
			}
		}

		if (collectGarbage) {
			log.info("Forced GBC");
			System.gc();
		}
		context.contextVariables.put("GARBAGE", collectGarbage);

		super.handle(context);
	}

	public static void addEndpoint(HttpServer server) {
		server.createContext("/systeminfo", new SystemReportHTTPHandler());
	}

}
