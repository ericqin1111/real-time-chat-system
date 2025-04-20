package csu.web.mypetstore.persistence;

import csu.web.mypetstore.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface CategoryDao {

    @Select("SELECT CATID AS categoryId,NAME,DESCN AS description FROM CATEGORY")
    List<Category> getCategoryList();

    @Select("SELECT CATID AS categoryId,NAME,DESCN AS description FROM CATEGORY WHERE CATID =#{categoryId}")
    Category getCategory(String categoryId);

}
