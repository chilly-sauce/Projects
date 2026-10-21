Java Multithreaded Port Scanner with Banner Grabbing

Overview

This project is a high-performance, multithreaded TCP port scanner built in Java. It scans all 65,535 valid ports on a target IP address or hostname. Beyond simply checking if a port is open, it features a dual-stage "banner grabbing" engine that attempts to identify the specific service or application running on the open port.

Technical Architecture & Key Decisions

Scanning 65,535 ports sequentially with a standard socket connection takes an impractical amount of time. To solve this and make the scanner genuinely useful, several critical design decisions were implemented:

1. High-Concurrency Multithreading

The Problem: A single-threaded scanner waiting for timeouts on closed or filtered ports would take hours to scan a single machine.

The Solution: Implemented Java's ExecutorService with a FixedThreadPool of 200 concurrent threads.

The Result: The workload of 65,535 ports is distributed across 200 active threads. The scanner can blast through a local target in seconds rather than hours, using executor.awaitTermination() to gracefully wait for all threads to finish before calculating the final scan duration.

2. Fast-Fail Connection Timeouts

The Problem: Default socket connections in Java will hang for a long time (often 60 seconds) if a firewall is silently dropping packets (filtered ports).

The Solution: Used soc.connect(new InetSocketAddress(host, port), timeout) with a strict timeout of 200 milliseconds. If the port doesn't complete the TCP handshake within 200ms, the scanner immediately marks it as closed and moves on.

3. Dual-Stage Banner Grabbing

Identifying what is running on a port is tricky because different protocols behave differently. This scanner uses a smart, two-stage approach:

Stage 1: Passive Listening (The Polite Approach)

Some services (like SSH, FTP, or SMTP) send a welcome message immediately upon connection.

The scanner sets an ultra-short read timeout (50ms) and listens. If data arrives, it reads the first line, saves it as the banner, and closes the connection.

Stage 2: Active Probing (The Aggressive Approach)

Web servers (HTTP/HTTPS) and databases (Redis) stay completely silent until the client sends a request. If Stage 1 times out, the scanner catches the IOException and shifts to active probing.

A custom getProbePayload() method checks the port number and fires a protocol-specific payload:

Port 6379: Sends PING\r\n to wake up Redis.

Port 11211: Sends version\r\n for Memcached.

Port 25/587: Sends EHLO for SMTP servers that might have been too slow for Stage 1.

Fallback: Sends a generic HTTP/1.0 HEAD request (HEAD / HTTP/1.0\r\n...). This acts as an excellent catch-all for web servers, REST APIs, and microservices, forcing them to reveal their server software (e.g., Server: nginx).

4. Clean Output Formatting

Raw banners often contain massive amounts of HTML or ugly carriage returns. The scanner uses String.split("\\r?\\n")[0] to extract strictly the very first line of the response, keeping the terminal output clean and readable.

How to Run

Compile the code: javac Main.java

Run the application: java Main

When prompted Enter target:, type an IP address (e.g., 127.0.0.1 or 192.168.1.5) or a domain name (e.g., scanme.nmap.org).

The scanner will output open ports and their banners in real-time, followed by the total execution time.

Note: Port scanning external networks without permission can trigger intrusion detection systems or violate ISP terms of service. Use on local/authorized infrastructure.

Future Improvements

Command Line Arguments: Replace the Scanner input with args[] parsing so the tool can be run via one-liners (e.g., java Main 192.168.1.1 -t 300).

Custom Port Ranges: Allow the user to specify a range (e.g., 1-1024) instead of always scanning all 65,535.

Output Export: Add the ability to write the final scan results to a .csv or .txt file for documentation.
