/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package backgammon;

import cookieMonster.BgoFibsCookieMonster;
import java.util.logging.Level;
import java.util.logging.Logger;
import socketHandler.SocketHandler;

/**
 *
 * @author kevin
 */
public class Backgammon {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BgoFibsCookieMonster cookieMonster = new BgoFibsCookieMonster();
        cookieMonster.resetFIBSCookieMonster();
        SocketHandler socketHandler = new SocketHandler(cookieMonster);
        Thread t = new Thread(socketHandler);
        t.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Backgammon.class.getName()).log(Level.SEVERE, null, ex);
        }
        //socketHandler.authenticate("phio", "motorrad");
        socketHandler.authenticate("guest", "");
    }
}
