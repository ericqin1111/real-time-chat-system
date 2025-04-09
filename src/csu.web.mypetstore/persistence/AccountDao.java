package csu.web.mypetstore.persistence;

import csu.web.mypetstore.domain.Account;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountDao {
    @Select("SELECT SIGNON.USERNAME, SIGNON.PASSWORD, ACCOUNT.EMAIL, ACCOUNT.FIRSTNAME, ACCOUNT.LASTNAME, ACCOUNT.STATUS, ACCOUNT.ADDR1 AS address1, ACCOUNT.ADDR2 AS address2, ACCOUNT.CITY, ACCOUNT.STATE, ACCOUNT.ZIP, ACCOUNT.COUNTRY, ACCOUNT.PHONE, PROFILE.FAVCATEGORY AS favouriteCategoryId, PROFILE.MYLISTOPT AS listOption, PROFILE.BANNEROPT AS bannerOption, BANNERDATA.BANNERNAME FROM ACCOUNT JOIN SIGNON ON SIGNON.USERNAME = ACCOUNT.USERID JOIN PROFILE ON PROFILE.USERID = ACCOUNT.USERID JOIN BANNERDATA ON PROFILE.FAVCATEGORY = BANNERDATA.FAVCATEGORY WHERE ACCOUNT.USERID = #{username}")
    Account getAccountByUsername(String username);

    @Select("SELECT SIGNON.USERNAME, SIGNON.PASSWORD, ACCOUNT.EMAIL, ACCOUNT.FIRSTNAME, ACCOUNT.LASTNAME, ACCOUNT.STATUS, ACCOUNT.ADDR1 AS address1, ACCOUNT.ADDR2 AS address2, ACCOUNT.CITY, ACCOUNT.STATE, ACCOUNT.ZIP, ACCOUNT.COUNTRY, ACCOUNT.PHONE, PROFILE.FAVCATEGORY AS favouriteCategoryId, PROFILE.MYLISTOPT AS listOption, PROFILE.BANNEROPT AS bannerOption, BANNERDATA.BANNERNAME FROM ACCOUNT JOIN SIGNON ON SIGNON.USERNAME = ACCOUNT.USERID JOIN PROFILE ON PROFILE.USERID = ACCOUNT.USERID JOIN BANNERDATA ON PROFILE.FAVCATEGORY = BANNERDATA.FAVCATEGORY WHERE ACCOUNT.USERID = #{username} AND SIGNON.PASSWORD = #{password}")
    Account getAccountByUsernameAndPassword(Account account);

    @Insert("INSERT INTO SIGNON  (username, password) VALUES (#{username},#{password})")
    void insertAccount(Account account);

    @Insert("INSERT INTO account  (userid, email, firstname, lastname, status, addr1, addr2, city, state, zip, country, phone) VALUES (#{userid } ,#{email } ,#{ firstname} ,#{ lastname} ,#{status } ,#{addr1 } ,#{addr2 } ,#{ city} ,#{ state} ,#{ zip},#{ country},#{ phone}  )")
    void insertAccountWith(Account account);

    @Update("UPDATE SIGNON SET PASSWORD = #{password} WHERE USERNAME =#{username} ")
    void updateAccount(String username, String password);


}
