
import java.io.*;
import java.net.*;
import java.security.*;

import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        Scanner scanner= new Scanner(System.in);
        String serverIP = null;
        String serverName=null;

        System.out.println("searching for server on local network....");
        try (DatagramSocket udpsoc = new DatagramSocket(8888)) {
            byte[] buffer = new byte[1024];
            DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);

            udpsoc.receive(pkt);

            String msg = new String(pkt.getData(), 0, pkt.getLength());

            if ("LAN_FILE_SERVER_DISCOVERY".equals(msg)) {
                serverIP = pkt.getAddress().getHostAddress();
                serverName = pkt.getAddress().getHostName();
                System.out.println("Server add found "+serverIP);
            }
        } catch (Exception e) {
            System.out.println("Error finding server " + e.getMessage());
            return;
        }

        if (serverIP != null) {
            System.out.println("\n--- SERVER FOUND ---");
            System.out.println("Device Name: " + serverName);
            System.out.println("IP Address:  " + serverIP);

            System.out.print("\nDo you want to send the file to this device? (y/n): ");

            String userInput = scanner.nextLine().trim().toLowerCase();

            if (!userInput.equals("y")) {
                System.out.println("Transfer cancelled by user. Exiting...");
                return; // Stops the program if they type anything other than 'y'
            }

            File[] toSend={
                    new File("D:ideaIC-2025.2.6.2.exe"),
                    new File("D:tor-browser-windows-x86_64-portable-15.0.20.exe")
            };

            try {
                Socket socket = new Socket(serverIP, 5000);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());



                int validCount=0;
                for(int i=0;i< toSend.length;i++){
                    if(toSend[i].exists()){
                        validCount++;
                    }
                }

                dos.writeInt(validCount);
                dos.flush();

                for(int i=0;i< toSend.length;i++){
                    File currentFile=toSend[i];

                    if(!currentFile.exists()){
                        System.out.println(currentFile.getName() +"dne");
                        continue;
                    }
                    System.out.println("\nsending "+currentFile.getName());
                    // Send metadata
                    dos.writeUTF(currentFile.getName());
                    dos.writeLong(currentFile.length());
                    dos.flush();
                    // Send file bytes
                    try (FileInputStream fis = new FileInputStream(currentFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalBytesSent = 0;
                        long fileSize = currentFile.length();

                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                            totalBytesSent += bytesRead;

                            int percent = (int) ((totalBytesSent * 100) / fileSize);
                            System.out.print("\rSending... " + percent + "%");
                        }
                    }
                    dos.flush();
                }

                System.out.println("\n\nAll files sent successfully!");



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}