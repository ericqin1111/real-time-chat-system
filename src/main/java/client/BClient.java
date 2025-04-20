package client;


import java.io.IOException;

public class BClient {
    public static void main(String[] args)  {
        try {
            new ChatClient().startClient("B");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
