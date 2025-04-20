package csu.web.mypetstore.domain;

import lombok.Data;

import java.io.Serializable;

public class Product  {
    private String productId;
    private String categoryId;
    private String name;
    private String description;

    private String pic;

    public Product() {
    }

    public Product(String productId, String categoryId, String name, String description, String pic) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.pic = pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public String getPic() {
        return pic;
    }


    /**
     * 获取
     * @return productId
     */
    public String getProductId() {
        return productId;
    }

    /**
     * 设置
     * @param productId
     */
    public void setProductId(String productId) {
        this.productId = productId;
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
        return "Product{productId = " + productId + ", categoryId = " + categoryId + ", name = " + name + ", description = " + description + ", pic = " + pic + "}";
    }
}
