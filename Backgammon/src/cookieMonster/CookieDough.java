/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cookieMonster;

import java.util.regex.Pattern;

/**
 *
 * @author kevin
 */
public class CookieDough {
    protected Pattern p;
    protected int cookie;
    protected CookieDough next;
    
    public CookieDough(int cookie, String str){
        this.cookie=cookie;
        p=Pattern.compile(str);
    }
}
