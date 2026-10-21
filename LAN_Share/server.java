import java.io.*;
import java.net.ServerSocket;
import java.net.*;

public class server {
    public static void main(String[] args) {
        // UDP Broadcast Thread
        new Thread(() -> {
            try (DatagramSocket udpsoc = new DatagramSocket()) {
                udpsoc.setBroadcast(true);
                byte[] msg = "LAN_FILE_SERVER_DISCOVERY".getBytes();
                InetAddress badd = InetAddress.getByName("255.255.255.255");
                int disport = 8888;

                DatagramPacket pkt = new DatagramPacket(
                        msg,
                        msg.length,
                        badd,
                        disport
                );

                while (true) {
                    udpsoc.send(pkt);
                    Thread.sleep(1500);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // TCP File Receiver Thread
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(5000);

                // Loop to keep the server accepting new clients
                while (true) {
                    System.out.println("\nwaiting for connection.....");
                    Socket socket = serverSocket.accept();
                    System.out.println("connected to " + socket.getRemoteSocketAddress());

                    DataInputStream dis = new DataInputStream(socket.getInputStream());

                    // Note: DataOutputStream isn't currently used by the server, but kept it as requested
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                    int quant = dis.readInt();
                    System.out.println("receiving " + quant + " files.");

                    for (int i = 0; i < quant; i++) {
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        System.out.println("name: " + fileName);
                        System.out.println("size: " + fileSize + " bytes");

                        FileOutputStream fos = new FileOutputStream("received_" + fileName);
                        byte[] buffer = new byte[4096];
                        long totalBytesRead = 0;
                        int bytesRead;

                        while (totalBytesRead < fileSize &&
                                (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;

                            int percent = (int) ((totalBytesRead * 100) / fileSize);
                            int barwidth = 30;
                            int progress = (int) ((totalBytesRead * 30) / fileSize);

                            String bar = "=".repeat(progress) + " ".repeat(barwidth - progress);

                            System.out.print("\r[" + bar + "]" + percent + "%(" + (totalBytesRead / 1024) + "KB/" + (fileSize / 1024) + " KB)");
                        }

                        // Added \n so the progress bar \r doesn't overwrite this message
                        System.out.println("\n" + fileName + " received succesfully!");
                        fos.close();
                    }

                    System.out.println("All files for this session received.");
                    dis.close();
                    socket.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}