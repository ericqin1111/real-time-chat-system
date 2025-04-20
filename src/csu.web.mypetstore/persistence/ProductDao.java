package csu.web.mypetstore.persistence;

import csu.web.mypetstore.domain.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface ProductDao {

    @Select("SELECT PRODUCTID,NAME,DESCN as description,CATEGORY as categoryId FROM PRODUCT WHERE CATEGORY = #{categoryId}")
    List<Product> getProductListByCategory(String categoryId);
@Select("SELECT PRODUCTID,NAME,DESCN as description,CATEGORY as categoryId FROM PRODUCT WHERE PRODUCTID =#{productId}")
    Product getProduct(String productId);
@Select("SELECT PRODUCTID,NAME,DESCN as description,CATEGORY as categoryId FROM PRODUCT WHERE lower(name) like #{keywords}")
    List<Product> searchProductList(String keywords);
}
