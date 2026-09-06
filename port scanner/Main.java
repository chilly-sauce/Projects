import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter target:");
        String host=scanner.nextLine();
        int timeout=200;
        int threadCount=200;

        ExecutorService executor= Executors.newFixedThreadPool(threadCount);

        System.out.println("Starting scanning on "+host);
        long startTime= System.currentTimeMillis();

        for(int i=1;i<=65535;i++){
            final int port=i;
            executor.submit(()-> {
                ScanRes res = scan(port, host, timeout);
                if (res.isOpen ==true ) {
                    System.out.println("[+] Port " + port + " is OPEN | Banner: " + res.banner);
                }
            });
        }
        executor.shutdown();
        scanner.close();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.err.println("scan interrupted: "+ e.getMessage());
        }

        long localtime=(System.currentTimeMillis()-startTime)/1000 ;
        System.out.println("scan completed in "+localtime+ " sec.");

    }
    public static ScanRes scan(int port , String host , int timeout){
        try (Socket soc = new Socket()) {
            soc.connect(new InetSocketAddress(host, port), timeout);

            String banner="No banner / silent";

            OutputStream out = soc.getOutputStream();
            InputStream in= soc.getInputStream();

            try{
                soc.setSoTimeout(50);
                byte[] buffer= new byte[1024];
                int bytesRead=in.read(buffer);

                if(bytesRead!=-1){
                    banner= new String(buffer,0,bytesRead).trim().split("\\r?\\n")[0];

                }

            }catch ( IOException s) {

                try {
                    soc.setSoTimeout(timeout);

                    out.write(getProbePayload(port, host));
                    out.flush();
                    byte[] buffer = new byte[1024];
                    int bytesRead = in.read(buffer);

                    if (bytesRead != -1) {
                        banner = new String(buffer, 0, bytesRead).trim().split("\\r?\\n")[0];

                    }
                } catch (SocketTimeoutException e) {
                    banner = "silent service";
                } catch (IOException e) {
                    banner = "banner read error";
                }
            }

            return new ScanRes(port, true, banner);

        } catch (IOException e) {
            return new ScanRes(port,false,"closed");
        }
    }

    public static class ScanRes{
        public final int port;
        public final boolean isOpen;
        public final String banner;

        public ScanRes(int port, boolean isOpen , String banner) {
            this.port = port;
            this.isOpen=isOpen;
            this.banner=banner;
        }
    }

    private static byte[] getProbePayload(int port, String host) {
        switch (port) {
            case 6379: // Redis
                return "PING\r\n".getBytes();
            case 11211: // Memcached
                return "version\r\n".getBytes();
            case 25:   // SMTP (if passive listen missed it)
            case 587:
                return ("EHLO " + host + "\r\n").getBytes();
            default:   // Universal web fallback (catches dev servers, REST APIs, microservices)
                return ("HEAD / HTTP/1.0\r\nHost: " + host + "\r\n\r\n").getBytes();
        }
    }

}
