package csu.web.mypetstore.persistence;

import csu.web.mypetstore.domain.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
@Mapper
public interface ItemDao {
    void updateInventoryQuantity(Map<String, Object> param);

    @Select("SELECT QTY AS value FROM INVENTORY WHERE ITEMID = #{itemId}")
    int getInventoryQuantity(String itemId);
    @Select(" SELECT I.ITEMID, LISTPRICE, UNITCOST, SUPPLIER AS supplierId, " +
            "I.PRODUCTID AS productId, NAME AS name, DESCN AS description, CATEGORY AS categoryId, STATUS, ATTR1 AS attribute1, ATTR2 AS attribute2, ATTR3 AS attribute3, ATTR4 AS attribute4, ATTR5 AS attribute5 FROM ITEM I, PRODUCT P " +
            "WHERE P.PRODUCTID = I.PRODUCTID AND I.PRODUCTID = #{productId}")
    List<Item> getItemListByProduct(String productId);

@Select("SELECT I.ITEMID, LISTPRICE, UNITCOST, SUPPLIER AS supplierId, I.PRODUCTID AS productId, NAME AS name, DESCN AS description, CATEGORY AS categoryId, STATUS, ATTR1 AS attribute1, ATTR2 AS attribute2, ATTR3 AS attribute3, ATTR4 AS attribute4, ATTR5 AS attribute5, QTY AS quantity FROM ITEM I, INVENTORY V, PRODUCT P " +
        "WHERE P.PRODUCTID = I.PRODUCTID AND I.ITEMID = V.ITEMID AND I.ITEMID =#{itemId} ")
    Item getItem(String itemId);
}
