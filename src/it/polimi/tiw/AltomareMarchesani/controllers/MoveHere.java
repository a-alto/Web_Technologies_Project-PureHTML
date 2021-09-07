package it.polimi.tiw.AltomareMarchesani.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import it.polimi.tiw.AltomareMarchesani.beans.Category;
import it.polimi.tiw.AltomareMarchesani.dao.CategoryDAO;

@WebServlet("/MoveHere")
public class MoveHere extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	
	public MoveHere() {
		
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
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		int idNewFather  = -1;
		int idCatToMove  = -1;
		String idNewFatherParam = request.getParameter("idNewFather");
		String idCatToMoveParam = request.getParameter("idCatToMove");
		Category treeToMove = null;
		
		boolean badRequest = false;
		if (idNewFatherParam == null || idCatToMoveParam == null) {
			badRequest = true;
		}
		
		try {
			idNewFather = Integer.parseInt(idNewFatherParam);
			idCatToMove = Integer.parseInt(idCatToMoveParam);
		} catch (NumberFormatException e) {
			badRequest = true;
		}
		
		if (badRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter id with format number is required");
			return;
		}
		
		CategoryDAO cService = new CategoryDAO(connection);
		try {
			treeToMove = cService.getCategoryById(idCatToMove);
			Category newFather = cService.getCategoryById(idNewFather);
			if(treeToMove == null || newFather == null) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Category not found");
				return;
			}
			
			cService.findSubparts(treeToMove);
			if(treeToMove.getId() == newFather.getId()) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"You cannot move a Category into itself"); 
				return;
			}
			
			if( treeToMove.getCod().length() < newFather.getCod().length() && treeToMove.getCod().equals(newFather.getCod().substring(0, treeToMove.getCod().length())) ) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST,"You cannot move a Category into one of its child");
				return;
			}
			
			Category oldFather = cService.findFather(idCatToMove);
			if(oldFather != null) cService.deleteLink(oldFather.getId(), idCatToMove);
			cService.createLink(idNewFather, idCatToMove);
			
		} catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error in retrieving categories from the database");
			return;
		}
		
		String path = getServletContext().getContextPath() + "/GoToHomePage";
		response.sendRedirect(path);
	}
}
