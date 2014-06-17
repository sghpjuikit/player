
package utilities;

/**
 *
 * @author uranium
 */
public final class MathUtil {
    
    /**
     * Logarithm
     * @param base of the log
     * @param i number to calculate log for
     * @return base specified logarithm of the number
     */
    static int log(int base, int i) {
        short p = 0;
        while(Math.pow(base, p) <= i)
            p++;
        return p;
    }
    
    /**
     * @param number
     * @return number of digits of a number
     */
    public static int length(int number) {
        int x = number;
        int cifres = 0;
        while (x > 0) {
            x /= 10;
            cifres++;
        }
        return cifres;
    }
    
    /**
     * Creates zeropadded string - string of a number with '0' added in to
     * maintain consistency in number of length.
     * @param a - to turn onto zeropadded string
     * @param b - number to zeropad into
     * @return 
     */
    public static String zeroPad(int a, int b) {
        int diff = length(b) - length(a);
        String out = "";
        for (int i=1; i<=diff; i++)
            out += "0";
        return out + String.valueOf(a);
    }
    
/******************************* PECULIAR *************************************/
    
    /**
     * @return highest possible number of the same decadic length as specified
     * number.
     * Examples:  9 for 1-10, 99 for 10-99, 999 for nubmers 100-999, etc...
     */
    public static int DecMin1(int number) {
        return (int) (Math.pow(10, 1+length(number))-1);
    }
}
