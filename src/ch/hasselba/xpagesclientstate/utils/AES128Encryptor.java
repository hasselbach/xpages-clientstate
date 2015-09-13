package ch.hasselba.xpagesclientstate.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * based on http://stackoverflow.com/questions/15554296/simple-java-aes-encrypt-decrypt-example
 * 
 * @author Sven Hasselbach
 */
public class AES128Encryptor {
	private final static String CIPHER = "AES/CBC/PKCS5PADDING";
	private final static String UTF8 = "UTF-8";
	private final static String MODE = "AES";
	
    public static byte[] encrypt(final String key, final String ivParameter, final byte[] toEncrypt) {
        try {
            IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes( UTF8 ));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes( UTF8 ), MODE);
            Cipher cipher = Cipher.getInstance( CIPHER );
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
          return cipher.doFinal(toEncrypt);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static byte[] decrypt(final String key, final String key2, byte[] toDecrypt) {
        try {
            IvParameterSpec iv = new IvParameterSpec(key2.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), MODE);
            Cipher cipher = Cipher.getInstance( CIPHER );
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            return cipher.doFinal(toDecrypt);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

  
}