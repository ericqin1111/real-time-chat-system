package csu.web.mypetstore.domain;

import lombok.Data;

import java.io.Serializable;

public class Category  {

    private String categoryId;
    private String name;
    private String description;


    public Category() {
    }

    public Category(String categoryId, String name, String description) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
    }

    /**
     * 获取
     * @return categoryId
     */
    public String getCategoryId() {
        return categoryId;
    }

    /**
     * 设置
     * @param categoryId
     */
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    /**
     * 获取
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * 设置
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "Category{categoryId = " + categoryId + ", name = " + name + ", description = " + description + "}";
    }
}
