package it.polimi.tiw.AltomareMarchesani.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private int id;
	private Boolean isTop = false;
	private Boolean isMoving = false;
	private String name;
	private String cod;
	private List<Category> subparts = new ArrayList<Category>();
	
	public Category() {
		
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Boolean getIsMoving() {
		return this.isMoving;
	}
	
	public void setIsMoving(Boolean isMoving) {
		this.isMoving = isMoving;
		for(Category cat: this.getSubparts()) {
			cat.setIsMoving(isMoving);
		}
	}
	
	public Boolean getIsTop() {
		return this.isTop;
	}
	
	public void setIsTop(Boolean isTop) {
		this.isTop = isTop;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getCod() {
		return this.cod;
	}
	
	public void setCod(String cod) {
		this.cod = cod;
	}
	
	public List<Category> getSubparts() {
		return subparts;
	}

	public void addSubpart(Category part) {
		if (this.getIsMoving()) part.setIsMoving(true);
		subparts.add(part);
	}

	public void removeSubpart(Category part) {
		subparts.remove(part);
	}

	public void setIsMovingById(int cId) {
		for(Category cat: getSubparts()) {
			if(cat.getId() == cId) {
				cat.setIsMoving(true);
				break;
			}
			else cat.setIsMovingById(cId);
		}
	}
	
	public String toString() {
		StringBuffer aBuffer = new StringBuffer("Category");
		aBuffer.append(" id: ");
		aBuffer.append(id);
		aBuffer.append(" name: ");
		aBuffer.append(name);
		aBuffer.append(" cod: ");
		aBuffer.append(cod);
		aBuffer.append(" subparts: ");
		return aBuffer.toString();
	}
	
}
