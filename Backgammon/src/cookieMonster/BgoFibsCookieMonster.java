/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cookieMonster;

import java.util.regex.Matcher;

/**
 *
 * @author kevin
 */

public class BgoFibsCookieMonster {
    private static final int TEST_FIBSCOOKIEMONSTER=0;
    private MessageState msgState= MessageState.FIBS_UNINITIALIZED_STATE;
    private static enum MessageState{
        FIBS_UNINITIALIZED_STATE,FIBS_LOGIN_STATE,FIBS_MODT_STATE,FIBS_RUN_STATE,FIBS_LOGOUT_STATE,BgOfFibsStateGuestLogin
    } 
    private static CookieDough loginBatch=null;
    private static CookieDough bgOfFibsGuestLoginBatch=null;
    private static CookieDough motdBatch=null;
    private static CookieDough alphaBatch=null;
    private static CookieDough numericBatch=null;
    private static CookieDough starsBatch=null;
    
    public void bgOSetFibsCookieMonsterToRunState(){
        if (msgState==MessageState.FIBS_UNINITIALIZED_STATE) prepareBatches();
        // unsure about right intendation
        msgState= MessageState.FIBS_RUN_STATE;
    }
    
    public int fibsCookie(String theMessage){
        if (msgState==MessageState.FIBS_UNINITIALIZED_STATE) prepareBatches();
        CookieDough ptr = null;
        switch (msgState){
            case FIBS_UNINITIALIZED_STATE:
                return FIBS_BAD_COOKIE;
                break;
            case FIBS_RUN_STATE:
                if (theMessage.length()==0) return FIBS_Empty;
                int result = FIBS_Unknown;
                char ch = theMessage.charAt(0);
                if (Character.isDigit(ch)){
                    ptr=numericBatch;
                    // unsure whether or not ptr.next!=null or ptr !=null
                    while(ptr.next!=null){
                        Matcher matcher = ptr.p.matcher(theMessage);
                        if (matcher.find()){
                            result=ptr.cookie;
                            break;
                        }
                        ptr=ptr.next;
                    }
                }
                else if (ch== '*'){
                    result = bgoFibsCookieStarsUnknown;
                    ptr=starsBatch;
                    while (ptr.next!=null){
                        Matcher m= ptr.p.matcher(theMessage);
                        if (m.find()){
                            result=ptr.cookie;
                            break;
                        }
                        ptr=ptr.next;
                    }
                }
                else {
                    ptr=alphaBatch;
                    while (ptr.next!=null){
                        Matcher m= ptr.p.matcher(theMessage);
                        if (m.find()){
                            result=ptr.cookie;
                            break;
                        }
                        ptr=ptr.next;
                    }
                }
                if (result== fibsGoodbye) msgState= MessageState.FIBS_LOGOUT_STATE;
                return result;
                break;
                
            case FIBS_LOGIN_STATE:
                int result= FIBS_PreLogin;
                ptr=loginBatch;
                while (ptr.next!=null){
                    Matcher matcher = ptr.p.matcher(theMessage);
                    if (matcher.find()){
                        result=ptr.cookie;
                        break;
                    }
                    ptr=ptr.next;
                }
                if (result==BGOFIBSCookieGuestWelcome) msgState=MessageState.BgOfFibsStateGuestLogin;
                if (result==CLIP_MOTD_BEGIN) msgState=MessageState.FIBS_MODT_STATE;
                return result;
                break;
                
            case BgOfFibsStateGuestLogin:
                int result=FIBS_Junk;
                ptr=bgOfFibsGuestLoginBatch;
                while(ptr.next!=null){
                    Matcher matcher = ptr.p.matcher(theMessage);
                    if (matcher.find()){
                        result=ptr.cookie;
                        break;
                    }
                    ptr=ptr.next;
                }
                return result;
                break;
            case FIBS_MODT_STATE:
                int result=FIBS_MOTD;
                ptr=motdBatch;
                while (motdBatch.next!=null){
                    Matcher matcher = ptr.p.matcher(theMessage);
                    if (matcher.find()){
                        result=ptr.cookie;
                        break;
                    }
                    ptr=ptr.next;
                }
                if (result==CLIP_MOTD_END) msgState=MessageState.FIBS_RUN_STATE;
                return result;
                break;
            case FIBS_LOGOUT_STATE:
                return FIBS_PostGoodbye;
                break;
            
                
        }
        return FIBS_Unknown;
    }
    
    public void resetFIBSCookieMonster(){
        if (msgState==MessageState.FIBS_UNINITIALIZED_STATE) prepareBatches();
        else msgState=MessageState.FIBS_LOGIN_STATE;
    }
    
    public void releaseFIBSCookieMonster(){
        // dont know whether or not i got this function right. lets see
        if (msgState==MessageState.FIBS_UNINITIALIZED_STATE) return;
        trashBatch(alphaBatch);
        trashBatch(starsBatch);
        trashBatch(numericBatch);
        trashBatch(loginBatch);
        trashBatch(bgOfFibsGuestLoginBatch);
        trashBatch(motdBatch);
        msgState= MessageState.FIBS_UNINITIALIZED_STATE;
    }
    
    private void trashBatch(CookieDough cd){
        CookieDough m=cd;
        while (m!=null){
            // enough? here lies the understanding problem. Examples as always would be nice :)
            releaseCookieDough(m);
        }
    }
    
    private static void prepareBatches(){
        CookieDough current;
        String str;
        str="^board:[a-zA-Z_<>]+:[a-zA-Z_<>]+:[0-9:\\-]+$";
        alphaBatch= startBatch(FIBS_BOARD,str);
    }
    
    private CookieDough startBatch(int cookie, String str){
        
        
    }
    
}
