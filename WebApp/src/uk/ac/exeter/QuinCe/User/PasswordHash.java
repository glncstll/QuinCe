package uk.ac.exeter.QuinCe.User;

import java.security.SecureRandom;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;


/**
 * <p>
 *   Password Hashing With PBKDF2 (<a href="http://crackstation.net/hashing-security.htm">http://crackstation.net/hashing-security.htm</a>).
 *   Copyright (c) 2013, Taylor Hornby
 *   All rights reserved.
 * </p>
 * <p>
 *   Redistribution and use in source and binary forms, with or without 
 *   modification, are permitted provided that the following conditions are met:
 * </p>
 * <ol>
 *   <li>
 *     Redistributions of source code must retain the above copyright notice, 
 *     this list of conditions and the following disclaimer.
 *   </li>
 *   <li>
 *     Redistributions in binary form must reproduce the above copyright notice,
 * 	   this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   </li>
 * </ol>
 * <p>
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 *   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 *   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 *   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 *   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 *   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 *   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 *   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *   POSSIBILITY OF SUCH DAMAGE.
 * </p>
 * 
 * @author: havoc AT defuse.ca
 * @author: Steve Jones
 */
public class PasswordHash
{
    /**
     * The algorithm to use for the hashes. CHANGING THIS WILL BREAK
     * EXISTING HASHES.
     */
	public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";

    /**
     * The size of the salt in bytes. Can be changed without breaking existing hashes. 
     */
    public static final int SALT_BYTE_SIZE = 20;
    
    /**
     * The size of the hash in bytes. Can be changed without breaking existing hashes.
     */
    public static final int HASH_BYTE_SIZE = 45;
    
    /**
     * The number of iterations to use in the hashing algorithm.
     * Can be changed without breaking existing hashes.
     */
    public static final int PBKDF2_ITERATIONS = 1000;

    /**
     * The index of the iteration count in password validation parameters.
     * @see #validatePassword(char[], String)
     */
    public static final int ITERATION_INDEX = 0;

    /**
     * The index of the salt in password validation parameters.
     * @see #validatePassword(char[], String)
     */
    public static final int SALT_INDEX = 1;

    /**
     * The index of the algorithm in password validation parameters.
     * @see #validatePassword(char[], String)
     */
    public static final int PBKDF2_INDEX = 2;
    
	/**
	 * Characters to be used in generating salts.
	 */
    private static final byte[] VALID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456879".getBytes();


    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param   password    the password to hash
     * @return              a salted PBKDF2 hash of the password
     * @throws NoSuchAlgorithmException If the chosen hashing algorithm does not exist
     * @throws InvalidKeySpecException If the key specification is invalid
     */
    public static String createHash(String password)
        throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        return createHash(password.toCharArray());
    }

    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param   password    the password to hash
     * @return              a salted PBKDF2 hash of the password
     * @throws NoSuchAlgorithmException If the chosen hashing algorithm does not exist
     * @throws InvalidKeySpecException If the key specification is invalid
     */
    public static String createHash(char[] password)
        throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        // Generate a random salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTE_SIZE];
        random.nextBytes(salt);

        // Hash the password
        byte[] hash = pbkdf2(password, salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE);
        // format iterations:salt:hash
        return PBKDF2_ITERATIONS + ":" + toHex(salt) + ":" +  toHex(hash);
    }

    /**
     * Validates a password using a hash.
     *
     * @param   password        the password to check
     * @param   correctHash     the hash of the valid password
     * @return                  true if the password is correct, false if not
     * @throws NoSuchAlgorithmException If the chosen hashing algorithm does not exist
     * @throws InvalidKeySpecException If the key specification is invalid
     */
    public static boolean validatePassword(String password, String correctHash)
        throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        return validatePassword(password.toCharArray(), correctHash);
    }

    /**
     * Validates a password using a hash.
     *
     * @param   password        the password to check
     * @param   correctHash     the hash of the valid password
     * @return                  true if the password is correct, false if not
     * @throws NoSuchAlgorithmException If the chosen hashing algorithm does not exist
     * @throws InvalidKeySpecException If the key specification is invalid
     */
    public static boolean validatePassword(char[] password, String correctHash)
        throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        // Decode the hash into its parameters
        String[] params = correctHash.split(":");
        int iterations = Integer.parseInt(params[ITERATION_INDEX]);
        byte[] salt = fromHex(params[SALT_INDEX]);
        byte[] hash = fromHex(params[PBKDF2_INDEX]);
        // Compute the hash of the provided password, using the same salt, 
        // iteration count, and hash length
        byte[] testHash = pbkdf2(password, salt, iterations, hash.length);
        // Compare the hashes in constant time. The password is correct if
        // both hashes match.
        return slowEquals(hash, testHash);
    }

    /**
     * Compares two byte arrays in length-constant time. This comparison method
     * is used so that password hashes cannot be extracted from an on-line 
     * system using a timing attack and then attacked off-line.
     * 
     * @param   a       the first byte array
     * @param   b       the second byte array 
     * @return          true if both byte arrays are the same, false if not
     */
    private static boolean slowEquals(byte[] a, byte[] b)
    {
        int diff = a.length ^ b.length;
        for(int i = 0; i < a.length && i < b.length; i++)
            diff |= a[i] ^ b[i];
        return diff == 0;
    }

    /**
     *  Computes the PBKDF2 hash of a password.
     *
     * @param   password    the password to hash.
     * @param   salt        the salt
     * @param   iterations  the iteration count (slowness factor)
     * @param   bytes       the length of the hash to compute in bytes
     * @return              the PBDKF2 hash of the password
     */
    protected static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
        throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    /**
     * Converts a string of hexadecimal characters into a byte array.
     *
     * @param   hex         the hex string
     * @return              the hex string decoded into a byte array
     */
    private static byte[] fromHex(String hex)
    {
        byte[] binary = new byte[hex.length() / 2];
        for(int i = 0; i < binary.length; i++)
        {
            binary[i] = (byte)Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
        }
        return binary;
    }

    /**
     * Converts a byte array into a hexadecimal string.
     *
     * @param   array       the byte array to convert
     * @return              a length*2 character string encoding the byte array
     */
    private static String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0) 
            return String.format("%0" + paddingLength + "d", 0) + hex;
        else
            return hex;
    }

    /**
     * Tests the basic functionality of the PasswordHash class
     *
     * @param   args        ignored
     */
    public static void main(String[] args)
    {
        try
        {
            // Print out 10 hashes
            for(int i = 0; i < 10; i++)
                System.out.println(PasswordHash.createHash("p\r\nassw0Rd!"));

            // Test password validation
            boolean failure = false;
            System.out.println("Running tests...");
            for(int i = 0; i < 100; i++)
            {
                String password = ""+i;
                String hash = createHash(password);
                String secondHash = createHash(password);
                if(hash.equals(secondHash)) {
                    System.out.println("FAILURE: TWO HASHES ARE EQUAL!");
                    failure = true;
                }
                String wrongPassword = ""+(i+1);
                if(validatePassword(wrongPassword, hash)) {
                    System.out.println("FAILURE: WRONG PASSWORD ACCEPTED!");
                    failure = true;
                }
                if(!validatePassword(password, hash)) {
                    System.out.println("FAILURE: GOOD PASSWORD NOT ACCEPTED!");
                    failure = true;
                }
            }
            if(failure)
                System.out.println("TESTS FAILED!");
            else
                System.out.println("TESTS PASSED!");
        }
        catch(Exception ex)
        {
            System.out.println("ERROR: " + ex);
        }
    }
    
    /**
     * Generate a random string.
     * <p>
     *   Taken from <a href="http://stackoverflow.com/questions/7111651/how-to-generate-a-secure-random-alphanumeric-string-in-java-efficiently">StackOverflow</a>.
     * </p>
     * <p>
     * <b>Notes:</b>
     * </p>
     * <p>
     *   Every call to {@link SecureRandom#nextInt()} uses up 32 bits of pure randomness, entropy bits,
     *   which gets produced at a very slow rate (see wiki article). You only need 6 entropy bits
     *   to get a fully random alphanumeric character {@code 2^6 = 64 > (26 + 26 + 10)}.
     * </p>
     * <p>
     *   So what you can do is seed Random with 64 bits of entropy bits. Every call to {@code Random.nextInt(62)}
     *   will consume 6 bits of entropy, so you can generate 10 alphanumeric characters that are "fully random"
     *   before you run out of entropy bits {@code (64/10 > log2(62))}.
	 * </p>
	 * <p>
	 *   This means you get 5 times the characters for the same number of entropy bits. This is the fastest way
	 *   to generate purely random alphanumeric strings.
	 * </p>
	 * 
     * @param size The length of the string
     * @return The generated string
     */
    public static byte[] generateRandomString(int size) {
    	SecureRandom srand = new SecureRandom();
        Random rand = new Random();
        byte[] randomString = new byte[size];

        for (int i = 0; i < size; ++i) {
          if ((i % 10) == 0) {
              rand.setSeed(srand.nextLong()); // 64 bits of random!
          }
          randomString[i] = VALID_CHARACTERS[rand.nextInt(VALID_CHARACTERS.length)];
        }
        
        return randomString;
    }
    
    /**
     * Generate a random salt to be used with a password.
     * @return The salt
     */
    public static byte[] generateSalt() {
    	// Copied from createHash above
    	SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_BYTE_SIZE];
        random.nextBytes(salt);
        return salt;
    }
}