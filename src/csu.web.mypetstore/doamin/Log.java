package csu.web.mypetstore.domain;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
@Data
public class Log implements Serializable {
    private Timestamp time;
    String type;
    String document;

    public Log() {

    }


}
