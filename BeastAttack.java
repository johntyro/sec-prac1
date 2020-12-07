import java.io.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class BeastAttack
{

    public static byte[] subArray(byte[] array, int beg, int end) {
        return Arrays.copyOfRange(array, beg, end);
    }

    static void printCT(byte[] ct){
		for(int i = 0; i < 8; i++)
		{
		    System.out.print(String.format("%02x ", ct[i]));
		}
		
		for(int j = 8; j < ct.length; j++)
		{
		    System.out.print(String.format("%02x", ct[j]));
		}
		
		System.out.println("");    
    }

    static byte[] nextIV(byte[] ct){
    	byte[] expIV = subArray(ct, 0, 8);
    	// next IV guess, based on cycles of 13 ticks
		expIV[7] +=  13;
		if(expIV[7] <= -128 + 13)
			expIV[6] += 1;
		return expIV;
    }

    static byte[] merge(int pos, byte[] msg, byte x){
    	byte[] prefix = new byte[8];
		int i = 0;
		for(; i < 7 - pos; i++) prefix[i] = 0;
		for(; i < 7; i++) prefix[i] = msg[pos + i - 7];
		prefix[7] = x;

		return prefix;
    }

    static void guessIV(byte[] ciphertext, byte[] prefix){
    	int cnt = 0;
    	boolean found = false;

		while(!found && cnt < 300){
			cnt += 1;
			
			expIV = nextIV(ciphertext);
			callEncrypt(prefix, prefix.length, ciphertext);

			found = Arrays.equals(expIV, subArray(ciphertext, 0, 8));
		}

		if(found) return;
		else return -1;
    }

    public static void main(String[] args) throws Exception
    {

		Scanner sc = new Scanner(System.in);
	        
		byte[] ciphertext = new byte[1024]; 
		byte[] prevCipher = new byte[1024];

		byte[] msg = new byte[8];

		callEncrypt(null, 0, ciphertext);    
		
		// the ciphertext is 64 bytes long, hence the padded plaintext is 56 bytes long
		// the IV is approximately 5 times the timestamp in milliseconds
		// when run through ssh the code below gives approximately 5000 difference in the IV
		

		for(int pos = 0; pos < 8; pos++){

			// for each position find the target ciphertext
			// need 7 - pos zeroes before the text
			byte[] prefix = new byte[7 - pos];
			Arrays.fill(prefix, (byte) 0);

			guessIV(pos, ciphertext, prefix);

			// E(msg[pos] xor iv_8) is at ciphertext 15
			byte tar = ciphertext[15];
			byte iv_8 = ciphertext[7];

			System.out.println("Sucess, target is " + tar + " iv_8 is " + iv_8);


			boolean match = false;
			boolean end = false;

			for(byte candidate = -128; !end; candidate++){
				if(candidate == 127) end = true;
				
				byte[] prefix = merge(pos, msg, candidate);
				System.out.print("Prefix is :" )
				for(byte b : prefix) System.out.print(" " + b)

				guessIV(ciphertext, prefix);	

				if (ciphertext[15] == tar){
					System.out.println("Candidate " + candidate + " is a match");
					msg[pos] = (byte) (candidate ^ iv_8);
					match = true;
					break;
				}
				else{
					System.out.println("Candidate " + candidate +" is rejected");
					continue;
				}
			}
			
			if(!match){ 
				System.out.println("\nxXx No match found xXx\n");
				return;
			}
			System.out.println("\nMessage is " + (char) msg[pos]);
		}


		for(byte b : msg){
			System.out.print( (char) b);
		}    
		System.out.println("");
	    
    }
    


    // a helper method to call the external programme "encrypt" in the current directory
    // the parameters are the plaintext, length of plaintext, and ciphertext; returns length of ciphertext
    static int callEncrypt(byte[] prefix, int prefix_len, byte[] ciphertext) throws IOException
    {
	HexBinaryAdapter adapter = new HexBinaryAdapter();
	Process process;
	
	// run the external process (don't bother to catch exceptions)
	if(prefix != null)
	{
	    // turn prefix byte array into hex string
	    byte[] p=Arrays.copyOfRange(prefix, 0, prefix_len);
	    String PString=adapter.marshal(p);
	    process = Runtime.getRuntime().exec("./encrypt "+PString);
	}
	else
	{
	    process = Runtime.getRuntime().exec("./encrypt");
	}

	// process the resulting hex string
	String CString = (new BufferedReader(new InputStreamReader(process.getInputStream()))).readLine();
	byte[] c=adapter.unmarshal(CString);
	System.arraycopy(c, 0, ciphertext, 0, c.length); 
	return(c.length);
    }
}
