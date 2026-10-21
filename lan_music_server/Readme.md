Java HTTP Audio Streaming Server (RetroAudio Y2K)

Overview

This project is a custom-built, multithreaded HTTP server written entirely in plain Java (no external frameworks like Spring Boot or Tomcat). It is designed specifically to serve a directory of .mp3 files over a local network. It dynamically generates a retro, Y2K-themed web interface and supports advanced HTTP streaming features like audio scrubbing (seeking) via byte-range requests.

Technical Architecture & Key Decisions

Building an HTTP media server from scratch requires handling specific network protocols that standard web frameworks usually hide from you. Here are the core technical decisions made during development:

1. Multithreaded Client Handling

The Problem: A single-threaded server would freeze completely if one user was streaming a 5-minute song, preventing anyone else from loading the web page or listening to music.

The Solution: Implemented an ExecutorService with a FixedThreadPool of 8 threads.

The Result: The server can seamlessly handle multiple concurrent connections. It can serve the HTML page to a new visitor while simultaneously streaming audio chunks to active listeners on other threads.

2. HTTP "Range" Requests (206 Partial Content)

The Problem: If you just send an MP3 file with a 200 OK status, the browser has to download the entire file from start to finish. The user cannot click ahead (scrub) in the audio player because the browser doesn't know how to request specific parts of the file.

The Solution: Implemented partial content delivery.

The server reads the Range: bytes=X-Y header sent by the browser.

It calculates the exact byte slice requested.

It responds with a 206 Partial Content HTTP header, telling the browser exactly which byte range is being sent (Content-Range: bytes X-Y/Total).

It uses fis.skip(start) to jump to the exact byte in the .mp3 file and only pipes the requested chunk.

The Result: Users can seamlessly click anywhere on the audio timeline, and the server will instantly stream from that exact timestamp without downloading the whole file.

3. Graceful Broken Pipe Handling

The Problem: When a user skips a track or clicks to a new timestamp, the browser abruptly closes the current socket connection to open a new one for the new timestamp. In Java, writing to a closed socket throws nasty SocketException stack traces.

The Solution: Wrapped the in.read() and out.write() byte-piping loops in specific try-catch blocks that silently catch SocketException and print friendly messages like "prev song aborted!" or "client closed socket before req". This keeps the server logs clean and prevents crashes.

4. Security: Preventing Directory Traversal

The Problem: A malicious user could manually craft an HTTP request like GET /stream?file=../../../../Windows/System32/secret.txt to break out of the D:/server folder and read sensitive files on the host machine.

The Solution: Used Java's getCanonicalPath(). Before serving any file, the server resolves the absolute path of the requested file and checks: targetFile.getCanonicalPath().startsWith(folder.getCanonicalPath()). If it tries to escape the root music folder, the server intercepts it and throws a 404 Not Found.

5. Dynamic UI Generation & Frontend Hacks

Backend-Driven UI: Instead of serving a static HTML file, the server uses folder.listFiles() to scan the directory for .mp3s and dynamically builds the HTML string using a StringBuilder. If you add a new song to the folder, it instantly appears on the website upon refresh.

Y2K Aesthetic: Inline CSS with heavy text-shadows, gradients, and a specific filter: sepia(...) hue-rotate(...) hack on the native <audio> element forces modern browser media players to look like a retro, green-tinted industrial dashboard.

JavaScript Quality of Life: Injected a tiny JS script that listens for the play event. If a user clicks play on a track, it loops through all other <audio> elements on the page and pauses them, ensuring multiple songs don't overlap.

How to Run

Create a folder at D:/server (or modify the path in Main.java) and place some .mp3 files inside.

Compile the files: javac Main.java fxns.java

Run the server: java Main

Open a web browser on the same network and navigate to http://localhost:8080 (or the host machine's local IP address).

Future Improvements

Refactor HTML: The serveHtml string is currently very large and lives inside fxns.java. Move this logic into the currently empty html.java file to cleanly separate the frontend templates from the backend network logic.

ID3 Tag Parsing: Instead of just displaying the raw file name (e.g., track_01.mp3), implement a basic byte-reader to extract ID3 tags (Artist, Title) from the MP3 metadata to display in the UI.
