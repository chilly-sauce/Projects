import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base32;

public class Main {
    public static void main(String[] args) {
        // 1. Generate core security data
        Base32 base32 = new Base32();
        byte[] secret = fxns.getKey();

        // 2. Format URI for Authenticator
        String ecb32 = base32.encodeToString(secret).replace("=", "").trim();
        String issuer = "MyApp";
        String ecIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String account = "user@example.com";
        String ecaccount = URLEncoder.encode(account, StandardCharsets.UTF_8);

        String qrdata = "otpauth://totp/" + ecIssuer + ":" + ecaccount
                + "?secret=" + ecb32
                + "&issuer=" + ecIssuer
                + "&algorithm=SHA1&digits=6&period=30";

        // 3. Generate QR Code image file locally
        fxns.qr(qrdata, 300, 300, "totp_qr.png");

        // 4. Pass secret to the UI memory state and launch
        ModernAuthApp.setSecret(secret);
        ModernAuthApp.launchApp(args);
    }
}