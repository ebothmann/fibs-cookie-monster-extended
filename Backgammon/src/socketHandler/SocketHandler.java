/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package socketHandler;

import cookieMonster.BgoFibsCookieMonster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kevin
 */
public class SocketHandler implements Runnable{
    Socket s;
    PrintWriter p;
    BufferedReader br;
    BgoFibsCookieMonster cookieMonster;
    
    public SocketHandler(BgoFibsCookieMonster cookieMonster){
        this.cookieMonster=cookieMonster;
    }
    
    @Override
    public void run() {
        init();
        listen();
        
    }
    
    private void init(){
        try {
            s= new Socket("www.fibs.com", 4321);
            p = new PrintWriter(s.getOutputStream(),true);
            br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        } catch (UnknownHostException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void listen(){
        while (true){
            try {
                //System.out.println(br.readLine());
                String message=br.readLine();
                int cookie = cookieMonster.fibsCookie(message);
                System.out.printf("%3d: %s\n", cookie, message);
            } catch (IOException ex) {
                Logger.getLogger(SocketHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
    }
    
    public void authenticate(String user, String passwd){
        p.println(user);
        p.println(passwd);
    }
    
}
