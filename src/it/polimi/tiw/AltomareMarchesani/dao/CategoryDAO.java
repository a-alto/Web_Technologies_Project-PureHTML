package it.polimi.tiw.AltomareMarchesani.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import it.polimi.tiw.AltomareMarchesani.beans.Category;

public class CategoryDAO {
	private int MAX_SUBCAT = 9;
	private Connection connection;

	public CategoryDAO() {
	}

	public CategoryDAO(Connection con) {
		this.connection = con;
	}

	public List<Category> findAllCategories() throws SQLException {
		List<Category> Categories = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM db_img_catalog.category");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category cat = new Category();
					cat.setId(result.getInt("id"));
					cat.setName(result.getString("name"));
					cat.setCod(result.getString("cod"));
					Categories.add(cat);
				}
			}
		}
		return Categories;
	}

	public List<Category> findTopCatAndSubtrees() throws SQLException {
		List<Category> Categories = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT * FROM db_img_catalog.category WHERE id NOT IN (select child FROM db_img_catalog.subparts)");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category cat = new Category();
					cat.setId(result.getInt("id"));
					cat.setName(result.getString("name"));
					cat.setCod(result.getString("cod"));
					cat.setIsTop(true);
					Categories.add(cat);
				}
				for (Category c : Categories) {
					findSubparts(c);
				}
			}
		}
		return Categories;
	}

	public void findSubparts(Category c) throws SQLException {
		Category cat = null;
		try (PreparedStatement pstatement = connection.prepareStatement(
				"SELECT C.id, C.name, C.cod FROM db_img_catalog.category C JOIN db_img_catalog.subparts S on C.id = S.child WHERE S.father = ?");) {
			pstatement.setInt(1, c.getId());
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					cat = new Category();
					cat.setId(result.getInt("id"));
					cat.setName(result.getString("name"));
					cat.setCod(result.getString("cod"));
					findSubparts(cat);
					c.addSubpart(cat);
				}
			}
		}

	}

	public void createCategory(String name) throws SQLException {
		if(!nameExists(name)) {
			String query = "INSERT into db_img_catalog.category (name, cod) VALUES(?, ?)";
			try (PreparedStatement pstatement = connection.prepareStatement(query);) {
				pstatement.setString(1, name);
				pstatement.setString(2, Integer.toString(findTopCatAndSubtrees().size()+1));
				pstatement.executeUpdate();
			}
		} else throw new SQLException("Category name already used.");
	}

	private boolean nameExists(String name) throws SQLException {
		boolean nameExists = false;
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM db_img_catalog.category WHERE name = ?;");) {
			pstatement.setString(1, name);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					nameExists = true;
				}
			}
		}
		return nameExists;
	}
	
	public void createLink(int fatherId, int childId) throws SQLException {
		int numOfChildren = findChildren(fatherId).size();
		if(numOfChildren < MAX_SUBCAT) {
			String query = "INSERT into db_img_catalog.subparts(father, child) VALUES(?, ?)";
			try (PreparedStatement pstatement = connection.prepareStatement(query);) {
				pstatement.setInt(1, fatherId);
				pstatement.setInt(2, childId);
				pstatement.executeUpdate();
				updateCod(fatherId);
			}
		} else {
			deleteCategory(childId);
			throw new SQLException("Tried to add more than " + MAX_SUBCAT + " subcategories");
		}
	}
	
	public void createLink(int fatherId, String childName) throws SQLException {
		int numOfChildren = findChildren(fatherId).size();
		if(numOfChildren < MAX_SUBCAT) {
			String query = "INSERT into db_img_catalog.subparts(father, child) VALUES(?, (SELECT id FROM db_img_catalog.category WHERE name = ?));";
			try (PreparedStatement pstatement = connection.prepareStatement(query);) {
				pstatement.setInt(1, fatherId);
				pstatement.setString(2, childName);
				pstatement.executeUpdate();
				updateCod(fatherId);
			}
		} else {
			deleteCategory(childName);
			throw new SQLException("Tried to add more than " + MAX_SUBCAT + "subcategories.");
		}
	}
	
	private String getCodById(int cId) throws SQLException {
		String cod ="-";
		String query = "SELECT cod FROM db_img_catalog.category WHERE id = ?;";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, cId);
			try (ResultSet result = pstatement.executeQuery();) {
				while(result.next()) {
					cod = result.getString("cod");
				}
			}
		}
		return cod;
	}
	
	public Category getCategoryById(int cId) throws SQLException {
		Category cat = null;
		String query = "SELECT * FROM db_img_catalog.category WHERE id = ?;";
		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, cId);
			try (ResultSet result = pstatement.executeQuery();) {
				while(result.next()) {
					cat = new Category();
					cat.setId(result.getInt("id"));
					cat.setName(result.getString("name"));
					cat.setCod(result.getString("cod"));
				}
			}
		}
		return cat;
	}

		
	private void updateCod(int fatherId) throws SQLException {
		String query = "UPDATE db_img_catalog.category SET cod = ? WHERE id = ?;";
		List<Category> children = findChildren(fatherId);
		int i = 1;
		for(Category cat: children) {
			try (PreparedStatement pstatement = connection.prepareStatement(query);) {
				String cod = getCodById(fatherId) + Integer.toString(i);
				pstatement.setString(1, cod);
				pstatement.setInt(2, cat.getId());
				pstatement.executeUpdate();
			}
			updateCod(cat.getId());
			i++;
		}
	}
	
	public boolean linkExists(int fatherId, int childId)  throws SQLException {
		boolean linkExists = false;
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM db_img_catalog.subparts where father = ? and child = ?");) {
			pstatement.setInt(1, fatherId);
			pstatement.setInt(2, childId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					linkExists = true;
				}
			}
		}
		return linkExists;
	}
	
	public void deleteLink(int fatherId, int childId) throws SQLException {
		connection.setAutoCommit(false);
		
		String query = "DELETE FROM db_img_catalog.subparts WHERE father = ? and child = ?";
		PreparedStatement pstatement = null;
		
		String query2 = "UPDATE db_img_catalog.category SET cod = ? WHERE id = ?;";
		PreparedStatement pstatement2 = null;
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, fatherId);
			pstatement.setInt(2, childId);
			pstatement.executeUpdate();
			
			pstatement2 = connection.prepareStatement(query2);
			pstatement2.setString(1, Integer.toString(findTopCatAndSubtrees().size()));
			pstatement2.setInt(2, childId);
			pstatement2.executeUpdate();
			
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
			if (pstatement != null) {
				try {
					pstatement.close();
				} catch (Exception e) {
					throw e;
				}
			}
			if (pstatement2 != null) {
				try {
					pstatement2.close();
				} catch (Exception e) {
					throw e;
				}
			}
		}
	}

	
	private void deleteCategory(int cId) throws SQLException {
		String query = "DELETE FROM db_img_catalog.category WHERE id = ?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);)  {
			pstatement.setInt(1, cId);
			pstatement.executeUpdate();
		}
	}
	
	private void deleteCategory(String name) throws SQLException{
		String query = "DELETE FROM db_img_catalog.category WHERE name = ?";
		try (PreparedStatement pstatement = connection.prepareStatement(query);)  {
			pstatement.setString(1, name);
			pstatement.executeUpdate();
		}
	}
	
	
	public List<Category> findChildren(int cId) throws SQLException {
		List<Category> children = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT C.id, C.name, C.cod FROM db_img_catalog.category C JOIN db_img_catalog.subparts S ON C.id = S.child WHERE S.father = ?");) {
			pstatement.setInt(1, cId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setCod(result.getString("cod"));
					children.add(c);
				}
			}
		}
		return children;
	}
	
	public Category findFather(int childId) throws SQLException {
		Category c = null;
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT C.id, C.name, C.cod FROM db_img_catalog.category C JOIN db_img_catalog.subparts S ON C.id = S.father WHERE S.child = ?");) {
			pstatement.setInt(1, childId);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setCod(result.getString("cod"));
				}
			}
		}
		return c;
	}

}
