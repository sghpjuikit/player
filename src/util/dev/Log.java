/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.dev;

/**
 * Prints messages.
 */
public class Log {
  
    /**
     * Log message.
     * @param message 
     */
    public static void info(String message) {
        System.out.println(message);
    }
    
    /**
     * Log debugging message.
     * @param message 
     */
    public static void deb(String message) {
        //System.out.println("Debug: " + message);
    }
    
    public static void deb(String message, Object o) {
        String os = o.toString();
        System.out.println("Debug: " + message.replaceAll("\\{\\}", os));
    }
    
    public static void deb(String message, Object o1, Object o2) {
        String o1s = o1.toString();
        String o2s = o2.toString();
        String m = message.replaceFirst("\\{\\}", o1s).replaceFirst("\\{\\}", o2s);
        System.out.println("Debug: " + m);
    }
    
    /**
     * Log error.
     * @param message 
     */
    public static void err(String message) {
        System.err.println("Error: " + message);
    }
    
    /**
     * Log warning message.
     * @param message 
     */
    public static void warn(String message) {
        System.err.println("Warn: " + message);
    }
    
    /**
     * Notifies user of the message. This method is supposed to deliver the message
     * specifically in GUI.
     * @param message 
     */
    public static void unsupported(String message) {
        System.out.println("Unsupported operation on this platform: " + message);   
    }

}
