
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;

import java.time.Instant;

import javax.crypto.Mac;
import java.security.InvalidKeyException;
import javax.crypto.spec.SecretKeySpec;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.File;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class fxns {

    public static byte[] getKey(){
        byte[] key=new byte[20];
        try {
            SecureRandom strongRandom=SecureRandom.getInstanceStrong();
            strongRandom.nextBytes(key);
        }catch (NoSuchAlgorithmException e){
            SecureRandom fallback= new SecureRandom();
            fallback.nextBytes(key);
        }
        return key;
    }

    public static long window(){
        long uTime=Instant.now().getEpochSecond();
        return uTime/30;
    }

    public static byte[] hash(byte[] key , byte[] time){
        try{
            SecretKeySpec signingKey= new SecretKeySpec(key , "HmacSHA1");
            Mac mac= Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            return mac.doFinal(time);
        }catch (NoSuchAlgorithmException| InvalidKeyException e){
            throw new IllegalStateException("Failed to calculate hmac SHA1",e);
        }
    }

    public static String getOTP(byte[] hashvalue){
        int offset =hashvalue[hashvalue.length -1] & 0x0F;
        int code =((hashvalue[offset] & 0x7F)<<24)
                  |((hashvalue[offset+1] & 0xFF)<<16)
                  |((hashvalue[offset+2] & 0xFF)<<8)
                  |((hashvalue[offset+3] & 0xFF));

        int otp=code % 1_000_000;
        String op= String.format("%06d",otp);
        return op;

    }

    public static void qr(String text ,int ht , int wd , String filepath){
        try{
            BitMatrix bM= new MultiFormatWriter().encode(text,BarcodeFormat.QR_CODE,wd,ht);
            Path pt= new File(filepath).toPath();
            MatrixToImageWriter.writeToPath(bM, "PNG",pt);
        } catch (Exception e) {
            System.out.println("qr not generated :"+e.getMessage());
        }
    }

    public static long lastUsedTimeStep = -1;

    public static boolean check(String otpEntered, byte[] secret) {
        long fTime = fxns.window();

        // 3 time step values
        long currentStep = fTime;
        long prevStep = fTime - 1;
        long nextStep = fTime + 1;

        // Byte buffers
        byte[] currentWindow = ByteBuffer.allocate(8).putLong(currentStep).array();
        byte[] prevWindow = ByteBuffer.allocate(8).putLong(prevStep).array();
        byte[] nextWindow = ByteBuffer.allocate(8).putLong(nextStep).array();

        // Generate OTPs
        String otpCurrent = fxns.getOTP(fxns.hash(secret, currentWindow));
        String otpPrev = fxns.getOTP(fxns.hash(secret, prevWindow));
        String otpNext = fxns.getOTP(fxns.hash(secret, nextWindow));

        byte[] inputBytes = otpEntered.getBytes(StandardCharsets.UTF_8);

        // Constant-time comparisons
        boolean matchCurrent = MessageDigest.isEqual(otpCurrent.getBytes(StandardCharsets.UTF_8), inputBytes);
        boolean matchPrev = MessageDigest.isEqual(otpPrev.getBytes(StandardCharsets.UTF_8), inputBytes);
        boolean matchNext = MessageDigest.isEqual(otpNext.getBytes(StandardCharsets.UTF_8), inputBytes);

        // Verify match AND ensure the matched window has not been used yet
        if (matchCurrent && currentStep > lastUsedTimeStep) {
            lastUsedTimeStep = currentStep;
            return true;
        } else if (matchPrev && prevStep > lastUsedTimeStep) {
            lastUsedTimeStep = prevStep;
            return true;
        } else if (matchNext && nextStep > lastUsedTimeStep) {
            lastUsedTimeStep = nextStep;
            return true;
        }

        return false;
    }


}
