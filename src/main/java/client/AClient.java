package client;


import java.io.IOException;

public class AClient {
    public static void main(String[] args)  {
        try {
            new ChatClient().startClient("A");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
