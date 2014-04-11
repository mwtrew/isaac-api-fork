package uk.ac.cam.cl.dtg.isaac.app;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;

@WebServlet(urlPatterns = { "/isaac/*"}, 
initParams = { 
		@WebInitParam(name = "javax.ws.rs.Application", value = "uk.ac.cam.cl.dtg.isaac.app.IsaacApplicationRegister"),
		@WebInitParam(name = "resteasy.servlet.mapping.prefix", value="/isaac/")
})
public class IsaacHttpServletDispatcher extends HttpServletDispatcher {
	private static final long serialVersionUID = 1L;

}
