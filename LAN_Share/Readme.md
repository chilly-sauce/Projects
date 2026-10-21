Java LAN File Transfer Protocol

Overview

This project is a custom, automated local area network (LAN) file transfer application built entirely in Java. It allows a client machine to automatically discover a server on the same network and securely transfer multiple files, complete with real-time CLI progress bars and metadata preservation.

Project Evolution & History

This project was built iteratively, solving networking and I/O challenges step-by-step:

Version 1.0 (Proof of Concept): Started as a basic, one-way TCP socket connection. It had hardcoded IPs, hardcoded file names, and sent raw bytes using transferTo(). The server would shut down after receiving a single file.

Version 2.0 (The Robust Server): Introduced while(true) loops to keep the server alive for multiple clients, and added basic error handling (ConnectException) and file existence checks on the client side.

Version 3.0 (Automated & Dynamic): The current version. Replaced manual IP entry with UDP network discovery. Replaced raw byte dumping with a custom metadata protocol using DataOutputStream so the server dynamically knows file names, sizes, and quantities.

Technical Architecture & Key Decisions

To make this a true "application" rather than a script, several specific technical design decisions were made:

1. UDP Broadcast for Server Discovery

The Problem: Hardcoding the server's IP address (e.g., localhost or 192.168.x.x) made the tool rigid and difficult to use across different machines.

The Solution: Implemented a separate Thread on the server running a DatagramSocket. It constantly broadcasts the string "LAN_FILE_SERVER_DISCOVERY" to the broadcast address 255.255.255.255 on port 8888.

The Result: The client simply listens on port 8888. When it hears the broadcast, it automatically extracts the server's IP and Hostname, prompting the user to confirm the transfer.

2. Custom TCP Transfer Protocol (Metadata First)

The Problem: Pushing raw bytes through a socket means the receiving server has no idea what the file is called, what extension it has, or how large it is.

The Solution: Used DataOutputStream (Client) and DataInputStream (Server) to establish a predictable sequence of events:

Client sends an int representing the total number of files.

Client sends a UTF string for the file name.

Client sends a long for the file size.

Client sends the raw file bytes in 4KB chunks.

The Result: The server can dynamically recreate the exact file name (e.g., received_ideaIC.exe) and knows exactly when to stop reading the stream for one file and prepare for the next.

3. Resilient Client Array Logic

The Problem: When sending an array of files, if an early file in the array didn't exist, a flawed loop logic (i < validCount) would cause the client to prematurely stop checking the rest of the array, leaving the server hanging.

The Solution: Split the logic. First, iterate the entire array (toSend.length) to count valid files and notify the server. Then, iterate the entire array again to actually transmit the files, using continue to safely skip non-existent files without breaking the loop.

4. Memory Safety & Resource Management

Try-with-Resources: Extensively used for Socket, DatagramSocket, FileInputStream, and FileOutputStream. This ensures that even if a transfer fails or a network drop occurs, memory is freed and file locks are released.

Thread Isolation: The server separates the UDP broadcasting and the TCP listening into two distinct threads, ensuring they do not block one another.

How to Run

Configure Client Paths: Open client.java and ensure the file paths in the toSend array are correctly formatted for your OS (e.g., use double backslashes for Windows: "D:\\myFolder\\myFile.exe").

Start the Server: Run server.java. It will immediately begin broadcasting its presence on the LAN and listening for TCP connections on port 5000.

Start the Client: Run client.java.

Confirm Transfer: The client CLI will display the discovered server's name and IP. Type y to execute the batch file transfer.

Future Improvements

Replace the hardcoded toSend file array with a command-line argument parser so users can pass file paths directly in the terminal (e.g., java client "C:\file1.txt" "C:\file2.jpg").

Add checksum verification (like MD5 or SHA-256) after the transfer completes to guarantee data integrity.
