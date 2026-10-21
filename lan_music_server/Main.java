import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main{
    public static void main(String[] args) {

        File folder= new File("D:/server");

        System.out.println("Path checked: " + folder.getAbsolutePath());
        System.out.println("Exists: " + folder.exists());
        System.out.println("File size (bytes): " + folder.length());

        if (!folder.exists() ) {
            System.err.println("STOP: The file cannot be found at this path!");
            return;
        }

        int port = 8080;
        ExecutorService threadPool= Executors.newFixedThreadPool(8);
        try(ServerSocket serverSocket= new ServerSocket(port)){
            System.out.println("Server is listening on port: "+port);

            while(true){
                Socket clientSocket = serverSocket.accept();
                System.out.println("connection established");
                threadPool.execute(()->fxns.handleClient(clientSocket,folder));
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
                threadPool.shutdown();
        }
        }
}
