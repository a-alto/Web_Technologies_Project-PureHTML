package it.polimi.tiw.AltomareMarchesani.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.AltomareMarchesani.beans.Category;
import it.polimi.tiw.AltomareMarchesani.dao.CategoryDAO;

@WebServlet("/MoveCategory")
public class MoveCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;


	public MoveCategory() {
		super();
	}

	public void init() throws ServletException {
		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new UnavailableException("Couldn't get db connection");
		}

		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		List<Category> allcategories = null;
		List<Category> topcategories = null;
		int cId  = -1;
		String cIdParam = request.getParameter("idCatToMove");
		
		boolean badRequest = false;
		if (cIdParam == null) {
			badRequest = true;
		}
		
		try {
			cId = Integer.parseInt(cIdParam);
		} catch (NumberFormatException e) {
			badRequest = true;
		}
		
		if (badRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter id with format number is required");
			return;
		}
		
		CategoryDAO cService = new CategoryDAO(connection);
		try {
			allcategories = cService.findAllCategories();
			topcategories = cService.findTopCatAndSubtrees();
			Category catToMove = cService.getCategoryById(cId);
			
			if(catToMove == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Category to move not found");
				return;
			}
			
			for(Category cat: topcategories) {
				if(cat.getId() == cId) {
					cat.setIsMoving(true);
					break;
				}
				else cat.setIsMovingById(cId);
			}
			
				
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error in retrieving categories from the database");
			return;
		}
		// Redirect to the Home page and add variables
		String path = "/WEB-INF/Home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("allcategories", allcategories);
		ctx.setVariable("topcategories", topcategories);
		ctx.setVariable("idCatToMove", cId);
		ctx.setVariable("movingMode", true);
		templateEngine.process(path, ctx, response.getWriter());
	}
	
	
	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e){
				
			}
		}
	}

}
