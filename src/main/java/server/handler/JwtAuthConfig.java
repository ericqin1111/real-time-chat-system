package server.handler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class JwtAuthConfig {
    @Getter
    private static final List<String> SECUREFREE_PATHS = new ArrayList<>();


    //由于需要jwt的页面比不需要的多，请在下面写不需要jwt的api
    static {
        SECUREFREE_PATHS.add("/example");
        SECUREFREE_PATHS.add("/wsexample");
        SECUREFREE_PATHS.add("/login");
        SECUREFREE_PATHS.add("/register");
        SECUREFREE_PATHS.add("/file");


    }
}
