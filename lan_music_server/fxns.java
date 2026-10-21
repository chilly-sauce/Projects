import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class fxns {

    public static void handleClient(Socket socket, File folder){

        try(socket;
            InputStream in= socket.getInputStream();
            OutputStream out= socket.getOutputStream()){

            byte[] buffer= new byte[2048];
            int bytesRead;
            try {
                bytesRead = in.read(buffer);
            }catch (java.net.SocketException e){
                System.out.println("client closed socket before req");
                return;
            }

            if(bytesRead==-1) return;

            String req= new String(buffer ,0,bytesRead);
            String[] lines=req.split("\r\n");
            String firstLine= lines[0];
            System.out.println("Req: "+firstLine);

            if(firstLine.startsWith("GET /stream")){
                String requestedSong= extractQueryParam(firstLine,"file");

                if(requestedSong==null || requestedSong.isEmpty()){
                    sendError(out,"400 bad req","missing file param");
                    return;
                }

                requestedSong = URLDecoder.decode(requestedSong, StandardCharsets.UTF_8);
                File targetFile = new File(folder,requestedSong);
                if (!targetFile.getCanonicalPath().startsWith(folder.getCanonicalPath()) || !targetFile.exists()) {
                    sendError(out, "404 Not Found", "File not found");
                    return;
                }

                String rangeHeader = null;
                for (String line : lines) {
                    if (line.toLowerCase().startsWith("range:")) {
                        rangeHeader = line.substring(6).trim();
                        break;
                    }
                }

                serveAudio(out, targetFile, rangeHeader);

            }
            else if(firstLine.startsWith("GET / ")){
                serveHtml(out, folder);
            }
            else{
                sendError(out,"404 Not Found" ,"Endpoint not found");
            }
        }catch (java.net.SocketException e){
            System.out.println("Connection dropped by client: " + e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void serveHtml(OutputStream out , File folder) throws Exception{
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        StringBuilder listHtml = new StringBuilder();
        if (files == null || files.length == 0) {
            listHtml.append("<p>No MP3 files found in folder.</p>");
        } else {
            for (File file : files) {
                String fileName = file.getName();
                listHtml.append("<div style='margin-bottom: 25px; padding: 15px; background: #2a2a2a; border-radius: 8px;'>")
                        .append("<h3>").append(fileName).append("</h3>")
                        .append("<audio controls preload='metadata' src='/stream?file=")
                        .append(java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8))
                        .append("' style='width: 100%; max-width: 400px;'></audio>")
                        .append("</div>");
            }
        }


        String htmlBody ="<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <title>RetroAudio // Deck</title>\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <style>\n" +
                "    body { background-color: #111; background-image: radial-gradient(#2a2a2a 1px, transparent 0); background-size: 20px 20px; color: #c0c0c0; font-family: 'Courier New', Courier, monospace; margin: 0; padding: 20px; display: flex; flex-direction: column; align-items: center; }\n" +
                "    h1 { color: #ffaa00; text-shadow: 2px 2px #000, 0 0 10px #ffaa00; letter-spacing: 2px; border-bottom: 2px solid #ffaa00; padding-bottom: 10px; margin-bottom: 30px; text-transform: uppercase; }\n" +
                "    .player-deck { background: linear-gradient(180deg, #333 0%, #1a1a1a 100%); border: 3px outset #555; border-radius: 5px; padding: 15px; width: 100%; max-width: 500px; margin-bottom: 25px; box-shadow: 5px 5px 15px rgba(0,0,0,0.8), inset 1px 1px 2px #777; }\n" +
                "    .lcd-screen { background: #0a2e0a; border: 3px inset #222; padding: 12px; margin-bottom: 15px; box-shadow: inset 0 0 10px #000; }\n" +
                "    .track-title { color: #33ff33; font-size: 1.1em; font-weight: bold; text-shadow: 0 0 5px #33ff33; margin: 0 0 5px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; display: flex; align-items: center; }\n" +
                "    .track-title::before { content: '▶'; font-size: 0.8em; margin-right: 8px; animation: blink 1s step-end infinite; }\n" +
                "    .meta-info { color: #1a991a; font-size: 0.85em; margin: 0; text-transform: uppercase; }\n" +
                "    /* Filter hacks the native player to look green and industrial */\n" +
                "    audio { width: 100%; height: 35px; border-radius: 0; outline: none; filter: sepia(100%) hue-rotate(90deg) saturate(400%) contrast(150%) drop-shadow(2px 2px 2px #000); border: 2px solid #222; }\n" +
                "    @keyframes blink { 50% { opacity: 0; } }\n" +
                "    ::-webkit-scrollbar { width: 12px; } \n" +
                "    ::-webkit-scrollbar-track { background: #111; } \n" +
                "    ::-webkit-scrollbar-thumb { background: #444; border: 1px solid #777; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <h1>WinAudio Y2K // Deck</h1>\n" +
                listHtml.toString() +
                "  <script>\n" +
                "    document.addEventListener('play', function(e) {\n" +
                "      var players = document.getElementsByTagName('audio');\n" +
                "      for (var i = 0; i < players.length; i++) {\n" +
                "        if (players[i] !== e.target) { players[i].pause(); players[i].currentTime = 0; }\n" +
                "      }\n" +
                "    }, true);\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + htmlBody.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                "Connection: close\r\n\r\n" +
                htmlBody;

        out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
        out.flush();

    }

    static void serveAudio(OutputStream out , File audioFile ,String rangeHeader) throws IOException {
        long fileSize= audioFile.length();

        System.out.println("--> serveAudio started. Total file size: " + fileSize);

        if(rangeHeader==null || !rangeHeader.startsWith("bytes=")){
            String header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: audio/mpeg\r\n" +
                    "Content-Length: " + fileSize + "\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(header.getBytes());
            out.flush();

            try(FileInputStream fis= new FileInputStream(audioFile)){
                pipeBytes(fis,out,fileSize);
            }
            return;
        }

        String rangeValue= rangeHeader.substring(6).trim();
        String[] parts=rangeValue.split("-");

        long start=Long.parseLong(parts[0]);
        long end= ((parts.length)>1 && !parts[1].isEmpty())
                ?Long.parseLong(parts[1]):fileSize-1;

        if (start >= fileSize || end >= fileSize || start > end) {
            String notSatisfiable = "HTTP/1.1 416 Range Not Satisfiable\r\n" +
                    "Content-Range: bytes */" + fileSize + "\r\n" +
                    "Content-Length: 0\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(notSatisfiable.getBytes());
            out.flush();
            return;
        }

        long bytesToSend = end - start + 1;

        // 3. Send 206 Partial Content
        String header = "HTTP/1.1 206 Partial Content\r\n" +
                "Content-Type: audio/mpeg\r\n" +
                "Accept-Ranges: bytes\r\n" +
                "Content-Range: bytes " + start + "-" + end + "/" + fileSize + "\r\n" +
                "Content-Length: " + bytesToSend + "\r\n" +
                "Connection: close\r\n\r\n";
        out.write(header.getBytes());
        out.flush();

        // 4. Seek and stream only the requested byte slice
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            long skipped = fis.skip(start);
            while (skipped < start) {
                skipped += fis.skip(start - skipped);
            }
            pipeBytes(fis, out, bytesToSend);
        } catch (IOException e) {
            // Client scrubbed/disconnected
            System.out.println("Client scrubbed or aborted range: " + e.getMessage());
        }

    }

    private static void pipeBytes(InputStream in, OutputStream out,long totalBytes) throws IOException{
        byte[] buffer = new byte[64 * 1024];
        long remaining= totalBytes;

        try {
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);
                if (read == -1) break;

                out.write(buffer, 0, read);
                out.flush();
                remaining -= read;

            }
        }catch (IOException e){
            System.out.println("prev song aborted!");
        }

    }

    private static String extractQueryParam(String requestLine, String key){
        int questionMark = requestLine.indexOf('?');
        int httpIndex = requestLine.lastIndexOf(" HTTP/");
        if (questionMark == -1 || httpIndex == -1 || questionMark >= httpIndex) return null;

        String queryString = requestLine.substring(questionMark + 1, httpIndex);
        for (String param : queryString.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return null;
    }

    private static void sendError(OutputStream out, String status, String message) throws IOException {
        String body = "<h1>" + status + "</h1><p>" + message + "</p>";
        String response = "HTTP/1.1 " + status + "\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "Connection: close\r\n\r\n" +
                body;
        out.write(response.getBytes());
        out.flush();
    }

}
