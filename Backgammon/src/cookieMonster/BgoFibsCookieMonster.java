	
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
	    private static MessageState msgState= MessageState.FIBS_UNINITIALIZED_STATE;
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
	        CookieDough ptr;
	        int result;
	        switch (msgState){
	            case FIBS_UNINITIALIZED_STATE:
	                return FibsCookies.FIBS_BAD_COOKIE;
	            case FIBS_RUN_STATE:
	                if (theMessage.length()==0) return FibsCookies.FIBS_Empty;
	                result = FibsCookies.FIBS_Unknown;
	                char ch = theMessage.charAt(0);
	                if (Character.isDigit(ch)){
	                    ptr=numericBatch;
	                    // unsure whether or not ptr.next!=null or ptr !=null
	                    while(ptr!=null){
	                        Matcher matcher = ptr.p.matcher(theMessage);
	                        if (matcher.find()){
	                            result=ptr.cookie;
	                            break;
	                        }
	                        ptr=ptr.next;
	                    }
	                }
	                else if (ch== '*'){
	                    result = FibsCookies.BGOFIBSCookieStarsUnknown;
	                    ptr=starsBatch;
	                    while (ptr!=null){
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
	                    while (ptr!=null){
	                        Matcher m= ptr.p.matcher(theMessage);
	                        if (m.find()){
	                            result=ptr.cookie;
	                            break;
	                        }
	                        ptr=ptr.next;
	                    }
	                }
	                if (result== FibsCookies.FIBS_Goodbye) msgState= MessageState.FIBS_LOGOUT_STATE;
	                return result;
	                
	            case FIBS_LOGIN_STATE:
	                result= FibsCookies.FIBS_PreLogin;
	                ptr=loginBatch;
	                while (ptr!=null){
	                    Matcher matcher = ptr.p.matcher(theMessage);
                            System.out.println("Searching...");
	                    if (matcher.find()){
                                
	                        result=ptr.cookie;
	                        break;
	                    }
	                    ptr=ptr.next;
	                }
	                if (result==FibsCookies.BGOFIBSCookieGuestWelcome) msgState=MessageState.BgOfFibsStateGuestLogin;
	                if (result==Clip.CLIP_MOTD_BEGIN) msgState=MessageState.FIBS_MODT_STATE;
	                return result;
	                
	            case BgOfFibsStateGuestLogin:
	                result=FibsCookies.FIBS_Junk;
	                ptr=bgOfFibsGuestLoginBatch;
	                while(ptr!=null){
	                    Matcher matcher = ptr.p.matcher(theMessage);
	                    if (matcher.find()){
	                        result=ptr.cookie;
	                        break;
	                    }
	                    ptr=ptr.next;
	                }
	                return result;
	            case FIBS_MODT_STATE:
	                result=FibsCookies.FIBS_MOTD;
	                ptr=motdBatch;
	                while (motdBatch!=null){
	                    Matcher matcher = ptr.p.matcher(theMessage);
	                    if (matcher.find()){
	                        result=ptr.cookie;
	                        break;
	                    }
	                    ptr=ptr.next;
	                }
	                if (result==Clip.CLIP_MOTD_END) msgState=MessageState.FIBS_RUN_STATE;
	                return result;
	            case FIBS_LOGOUT_STATE:
	                return FibsCookies.FIBS_PostGoodbye;
	            
	                
	        }
	        return FibsCookies.FIBS_Unknown;
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
	            m=releaseCookieDough(m);
	        }
	    }
	    
	    private static void prepareBatches(){
	        alphaBatch= startBatch(FibsCookies.FIBS_Board,"^board:[a-zA-Z_<>]+:[a-zA-Z_<>]+:[0-9:\\-]+$");
                addDough(alphaBatch, FibsCookies.FIBS_BAD_Board, "^board:");
                addDough(alphaBatch, FibsCookies.FIBS_YouRoll, "^You roll [1-6] and [1-6]");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerRolls, "^[a-zA-Z_<>]+ rolls [1-6] and [1-6]");
                addDough(alphaBatch, FibsCookies.FIBS_RollOrDouble, "^It's your turn to roll or double\\.");
                addDough(alphaBatch, FibsCookies.FIBS_RollOrDouble, "^It's your turn\\. Please roll or double");
                addDough(alphaBatch, FibsCookies.FIBS_AcceptRejectDouble, "doubles\\. Type 'accept' or 'reject'\\.");
                addDough(alphaBatch, FibsCookies.FIBS_Doubles, "^[a-zA-Z_<>]+ doubles\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PleaseMove, "^Please move [1-4] pieces?\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerMoves, "^[a-zA-Z_<>]+ moves");
                addDough(alphaBatch, FibsCookies.FIBS_BearingOff, "^Bearing off:");
                addDough(alphaBatch, FibsCookies.FIBS_YouReject, "^You reject\\. The game continues\\.");
                addDough(alphaBatch, FibsCookies.FIBS_YouStopWatching, "You're not watching anymore\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieBannedFromWatching, "^[a-zA-Z_<>]+ bans you from watching\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieOpponentDrops, "drops connection. The game was saved\\.");
                addDough(alphaBatch, FibsCookies.FIBS_OpponentLogsOut, "logs out. The game was saved\\.");
                addDough(alphaBatch, FibsCookies.FIBS_OnlyPossibleMove, "^The only possible move is");
                addDough(alphaBatch, FibsCookies.FIBS_FirstRoll, "[a-zA-Z_<>]+ rolled [1-6].+rolled [1-6]");
                addDough(alphaBatch, FibsCookies.FIBS_MakesFirstMove, " makes the first move\\.");
                addDough(alphaBatch, FibsCookies.FIBS_YouDouble, "^You double\\. Please wait for ");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerWantsToResign, "^[a-zA-Z_<>]+ wants to resign\\. You will win [0-9]+ points?\\. Type 'accept' or 'reject'\\.");
                addDough(alphaBatch, FibsCookies.FIBS_WatchResign, "^[a-zA-Z_<>]+ wants to resign\\. ");
                addDough(alphaBatch, FibsCookies.FIBS_YouResign, "^You want to resign\\.");
                addDough(alphaBatch, FibsCookies.FIBS_ResumeMatchAck5, "^You are now playing with [a-zA-Z_<>]+\\. Your running match was loaded\\.");
                addDough(alphaBatch, FibsCookies.FIBS_JoinNextGame, "^Type 'join' if you want to play the next game, type 'leave' if you don't\\.");
                addDough(alphaBatch, FibsCookies.FIBS_NewMatchRequest, "^[a-zA-Z_<>]+ wants to play a [0-9]+ point match with you\\.");
                addDough(alphaBatch, FibsCookies.FIBS_WARNINGSavedMatch, "^WARNING: Don't accept if you want to continue");
                addDough(alphaBatch, FibsCookies.FIBS_ResignRefused, "rejects\\. The game continues\\.");
                addDough(alphaBatch, FibsCookies.FIBS_MatchLength, "^match length:");
                addDough(alphaBatch, FibsCookies.FIBS_TypeJoin, "^Type 'join [a-zA-Z_<>]+' to accept\\.");
                addDough(alphaBatch, FibsCookies.FIBS_YouAreWatching, "^You're now watching ");
                addDough(alphaBatch, FibsCookies.FIBS_YouStopWatching, "^You stop watching ");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerStartsWatching, "[a-zA-Z_<>]+ starts watching [a-zA-Z_<>]+\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerStartsWatching, "[a-zA-Z_<>]+ is watching you\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerStopsWatching, "[a-zA-Z_<>]+ stops watching [a-zA-Z_<>]+\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerIsWatching, "[a-zA-Z_<>]+ is watching ");
                addDough(alphaBatch, FibsCookies.FIBS_ResignWins, "^[a-zA-Z_<>]+ gives up\\. [a-zA-Z_<>]+ wins [0-9]+ points?\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieResignYouWinWithOldmovesScoreUpdate, "^[a-zA-Z_<>]+ gives up\\. You win [0-9]+ points?\\.Score is");
                addDough(alphaBatch, FibsCookies.FIBS_ResignYouWin, "^[a-zA-Z_<>]+ gives up\\. You win [0-9]+ points?\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieYouAcceptAndWinWithOldmovesScoreUpdate, "^You accept and win [0-9]+ points?\\.Score is");
                addDough(alphaBatch, FibsCookies.FIBS_YouAcceptAndWin, "^You accept and win [0-9]+");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieAcceptWinsWithOldmovesScoreUpdate, "^[a-zA-Z_<>]+ accepts and wins [0-9]+ points?\\.Score is");
                addDough(alphaBatch, FibsCookies.FIBS_AcceptWins, "^[a-zA-Z_<>]+ accepts and wins [0-9]+ point");
                addDough(alphaBatch, FibsCookies.FIBS_PlayersStartingMatch, "^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ start a [0-9]+ point match");
                addDough(alphaBatch, FibsCookies.FIBS_StartingNewGame, "^Starting a new game with ");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieYouGiveUpWithOldmovesScoreUpdate, "^You give up\\. [a-zA-Z_<>]+ wins [0-9]+ points?\\.Score is");
                addDough(alphaBatch, FibsCookies.FIBS_YouGiveUp, "^You give up\\. ");
                addDough(alphaBatch, FibsCookies.FIBS_YouWinMatch, "^You win the [0-9]+ point match");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerWinsMatch, "^[a-zA-Z_<>]+ wins the [0-9]+ point match");
                addDough(alphaBatch, FibsCookies.FIBS_ResumingUnlimitedMatch, "^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ are resuming their unlimited match\\.");
                addDough(alphaBatch, FibsCookies.FIBS_ResumingLimitedMatch, "^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ are resuming their [0-9]+-point match\\.");
                addDough(alphaBatch, FibsCookies.FIBS_MatchResult, "^[a-zA-Z_<>]+ wins a [0-9]+ point match against ");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerWantsToResign, "wants to resign\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieYouAcceptDoubleBad, "^You accept the double\\. The cube shows [0-9]+\\..+");
                addDough(alphaBatch, FibsCookies.FIBS_BAD_AcceptDouble, "^[a-zA-Z_<>]+ accepts? the double\\. The cube shows [0-9]+\\..+");
                addDough(alphaBatch, FibsCookies.FIBS_YouAcceptDouble, "^You accept the double\\. The cube shows");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerAcceptsDouble, "^[a-zA-Z_<>]+ accepts the double\\. The cube shows ");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieWatchedPlayerAcceptsDouble, "^[a-zA-Z_<>]+ accepts the double\\.");
                addDough(alphaBatch, FibsCookies.FIBS_ResumeMatchRequest, "^[a-zA-Z_<>]+ wants to resume a saved match with you\\.");
                addDough(alphaBatch, FibsCookies.FIBS_ResumeMatchAck0, "has joined you\\. Your running match was loaded");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieYouWinGameWithOldmovesScoreUpdate, "^You win the game and get [0-9]+ points?\\. Congratulations!Score is");
                addDough(alphaBatch, FibsCookies.FIBS_YouWinGame, "^You win the game and get");
                addDough(alphaBatch, FibsCookies.FIBS_UnlimitedInvite, "^[a-zA-Z_<>]+ wants to play an unlimited match with you\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookiePlayerWinsGameWithOldmovesScoreUpdate, "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?\\. Sorry\\.Score is");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerWinsGame, "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?. Sorry.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieWatchedPlayerWinsGame, "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?.");
                addDough(alphaBatch, FibsCookies.FIBS_WatchGameWins, "wins the game and gets");
                addDough(alphaBatch, FibsCookies.FIBS_PlayersStartingUnlimitedMatch, "start an unlimited match\\.");
                addDough(alphaBatch, FibsCookies.FIBS_ReportLimitedMatch, "^[a-zA-Z_<>]+ +- +[a-zA-Z_<>]+ .+ point match");
                addDough(alphaBatch, FibsCookies.FIBS_ReportUnlimitedMatch, "^[a-zA-Z_<>]+ +- +[a-zA-Z_<>]+ \\(unlimited");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesStart, "^[a-zA-Z_<>]+ is X - [a-zA-Z_<>]+ is O");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesRoll, "^[XO]: \\([1-6]");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesWins, "^[XO]: wins");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesDoubles, "^[XO]: doubles");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesAccepts, "^[XO]: accepts");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesRejects, "^[XO]: rejects");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieOldmovesWantsToResign, "^[XO]: wants to resign");
                addDough(alphaBatch, FibsCookies.FIBS_ShowMovesOther, "^[XO]:");
                addDough(alphaBatch, FibsCookies.FIBS_ScoreUpdate, "^score in [0-9]+ point match:");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieScoreUpdateUnlimited, "^score in unlimited match:");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieOldmovesMatchStart, "^Score is [0-9]+-[0-9]+ in a [0-9]+ point match\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieOldmovesUnlimitedMatchStart, "^Score is [0-9]+-[0-9]+ in an unlimited match\\.");
                addDough(alphaBatch, FibsCookies.FIBS_Settings, "^Settings of variables:");
                addDough(alphaBatch, FibsCookies.FIBS_Turn, "^turn:");
                addDough(alphaBatch, FibsCookies.FIBS_Boardstyle, "^boardstyle:");
                addDough(alphaBatch, FibsCookies.FIBS_Linelength, "^linelength:");
                addDough(alphaBatch, FibsCookies.FIBS_Pagelength, "^pagelength:");
                addDough(alphaBatch, FibsCookies.FIBS_Redoubles, "^redoubles:");
                addDough(alphaBatch, FibsCookies.FIBS_Sortwho, "^sortwho:");
                addDough(alphaBatch, FibsCookies.FIBS_Timezone, "^timezone:");
                addDough(alphaBatch, FibsCookies.FIBS_CantMove, "^[a-zA-Z_<>]+ can't move");
                addDough(alphaBatch, FibsCookies.FIBS_ListOfGames, "^List of games:");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerInfoStart, "^Information about");
                addDough(alphaBatch, FibsCookies.FIBS_EmailAddress, "^  Email address:");
                addDough(alphaBatch, FibsCookies.FIBS_NoEmail, "^  No email address\\.");
                addDough(alphaBatch, FibsCookies.FIBS_WavesAgain, "^[a-zA-Z_<>]+ waves goodbye again\\.");
                addDough(alphaBatch, FibsCookies.FIBS_Waves, "waves goodbye");
                addDough(alphaBatch, FibsCookies.FIBS_Waves, "^You wave goodbye\\.");
                addDough(alphaBatch, FibsCookies.FIBS_WavesAgain, "^You wave goodbye again and log out\\.");
                addDough(alphaBatch, FibsCookies.FIBS_NoSavedGames, "^no saved games\\.");
                addDough(alphaBatch, FibsCookies.FIBS_TypeBack, "^You're away\\. Please type 'back'");
                addDough(alphaBatch, FibsCookies.FIBS_SavedMatch, "^  [a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +");
                addDough(alphaBatch, FibsCookies.FIBS_SavedMatchPlaying, "^ \\*[a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSavedUnlimitedMatch, "^  [a-zA-Z_<>]+ +unlimited +[0-9]+ +- +");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSavedUnlimitedMatchPlaying, "^ \\*[a-zA-Z_<>]+ +unlimited +[0-9]+ +- +");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerIsWaitingForYou, "^[a-zA-Z_<>]+ is waiting for you to log in\\.");
                addDough(alphaBatch, FibsCookies.FIBS_IsAway, "^[a-zA-Z_<>]+ is away: ");
                addDough(alphaBatch, FibsCookies.FIBS_AllowpipTrue, "^allowpip +YES");
                addDough(alphaBatch, FibsCookies.FIBS_AllowpipFalse, "^allowpip +NO");
                addDough(alphaBatch, FibsCookies.FIBS_AutoboardTrue, "^autoboard +YES");
                addDough(alphaBatch, FibsCookies.FIBS_AutoboardFalse, "^autoboard +NO");
                addDough(alphaBatch, FibsCookies.FIBS_AutodoubleTrue, "^autodouble +YES");
                addDough(alphaBatch, FibsCookies.FIBS_AutodoubleFalse, "^autodouble +NO");
                addDough(alphaBatch, FibsCookies.FIBS_AutomoveTrue, "^automove +YES");
                addDough(alphaBatch, FibsCookies.FIBS_AutomoveFalse, "^automove +NO");
                addDough(alphaBatch, FibsCookies.FIBS_BellTrue, "^bell +YES");
                addDough(alphaBatch, FibsCookies.FIBS_BellFalse, "^bell +NO");
                addDough(alphaBatch, FibsCookies.FIBS_CrawfordTrue, "^crawford +YES");
                addDough(alphaBatch, FibsCookies.FIBS_CrawfordFalse, "^crawford +NO");
                addDough(alphaBatch, FibsCookies.FIBS_DoubleTrue, "^double +YES");
                addDough(alphaBatch, FibsCookies.FIBS_DoubleFalse, "^double +NO");
                addDough(alphaBatch, FibsCookies.FIBS_MoreboardsTrue, "^moreboards +YES");
                addDough(alphaBatch, FibsCookies.FIBS_MoreboardsFalse, "^moreboards +NO");
                addDough(alphaBatch, FibsCookies.FIBS_MovesTrue, "^moves +YES");
                addDough(alphaBatch, FibsCookies.FIBS_MovesFalse, "^moves +NO");
                addDough(alphaBatch, FibsCookies.FIBS_GreedyTrue, "^greedy +YES");
                addDough(alphaBatch, FibsCookies.FIBS_GreedyFalse, "^greedy +NO");
                addDough(alphaBatch, FibsCookies.FIBS_NotifyTrue, "^notify +YES");
                addDough(alphaBatch, FibsCookies.FIBS_NotifyFalse, "^notify +NO");
                addDough(alphaBatch, FibsCookies.FIBS_RatingsTrue, "^ratings +YES");
                addDough(alphaBatch, FibsCookies.FIBS_RatingsFalse, "^ratings +NO");
                addDough(alphaBatch, FibsCookies.FIBS_ReadyTrue, "^ready +YES");
                addDough(alphaBatch, FibsCookies.FIBS_ReadyFalse, "^ready +NO");
                addDough(alphaBatch, FibsCookies.FIBS_ReportTrue, "^report +YES");
                addDough(alphaBatch, FibsCookies.FIBS_ReportFalse, "^report +NO");
                addDough(alphaBatch, FibsCookies.FIBS_SilentTrue, "^silent +YES");
                addDough(alphaBatch, FibsCookies.FIBS_SilentFalse, "^silent +NO");
                addDough(alphaBatch, FibsCookies.FIBS_TelnetTrue, "^telnet +YES");
                addDough(alphaBatch, FibsCookies.FIBS_TelnetFalse, "^telnet +NO");
                addDough(alphaBatch, FibsCookies.FIBS_WrapTrue, "^wrap +YES");
                addDough(alphaBatch, FibsCookies.FIBS_WrapFalse, "^wrap +NO");
                addDough(alphaBatch, FibsCookies.FIBS_Junk, "^Closed old connection with user");
                addDough(alphaBatch, FibsCookies.FIBS_Done, "^Done\\.");
                addDough(alphaBatch, FibsCookies.FIBS_YourTurnToMove, "^It's your turn to move\\.");
                addDough(alphaBatch, FibsCookies.FIBS_SavedMatchesHeader, "^  opponent          matchlength   score \\(your points first\\)");
                addDough(alphaBatch, FibsCookies.FIBS_MessagesForYou, "^There are messages for you:");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieRedoublesSetToNone, "^Value of 'redoubles' set to 'none'\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieRedoublesSetToUnlimited, "^Value of 'redoubles' set to 'unlimited'\\.");
                addDough(alphaBatch, FibsCookies.FIBS_RedoublesSetTo, "^Value of 'redoubles' set to [0-9]+\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSortWhoValue, "^Value of 'sortwho' is");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSortWhoValueSetTo, "^Value of 'sortwho' set to");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSortWhoValueInvalid, "Try 'login', 'name', 'rating' or 'rrating'\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieBoardStyleValue, "^Value of 'boardstyle' is");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieBoardStyleValueSetTo, "^Value of 'boardstyle' set to");
                addDough(alphaBatch, FibsCookies.FIBS_DoublingCubeNow, "^The number on the doubling cube is now [0-9]+");
                addDough(alphaBatch, FibsCookies.FIBS_FailedLogin, "^> [0-9]+");
                addDough(alphaBatch, FibsCookies.FIBS_Average, "^Time (UTC)  average min max");
                addDough(alphaBatch, FibsCookies.FIBS_DiceTest, "^[nST]: ");
                addDough(alphaBatch, FibsCookies.FIBS_LastLogout, "^  Last logout:");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcStart, "^rating calculation:");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^Probability that underdog wins:");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "is 1-Pu if underdog wins");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^Experience: ");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^K=max\\(1");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^rating difference");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^change for");
                addDough(alphaBatch, FibsCookies.FIBS_RatingCalcInfo, "^match length  ");
                addDough(alphaBatch, FibsCookies.FIBS_WatchingHeader, "^Watching players:");
                addDough(alphaBatch, FibsCookies.FIBS_SettingsHeader, "^The current settings are:");
                addDough(alphaBatch, FibsCookies.FIBS_AwayListHeader, "^The following users are away:");
                addDough(alphaBatch, FibsCookies.FIBS_RatingExperience, "^  Rating: +[0-9]+\\.");
                addDough(alphaBatch, FibsCookies.FIBS_NotLoggedIn, "^  Not logged in right now\\.");
                addDough(alphaBatch, FibsCookies.FIBS_IsPlayingWith, "is playing with");
                addDough(alphaBatch, FibsCookies.FIBS_SavedScoreHeader, "^opponent +matchlength");
                addDough(alphaBatch, FibsCookies.FIBS_StillLoggedIn, "^  Still logged in\\.");
                addDough(alphaBatch, FibsCookies.FIBS_NoOneIsAway, "^None of the users is away\\.");
                addDough(alphaBatch, FibsCookies.FIBS_PlayerListHeader, "^No  S  username        rating  exp login    idle  from");
                addDough(alphaBatch, FibsCookies.FIBS_RatingsHeader, "^ rank name            rating    Experience");
                addDough(alphaBatch, FibsCookies.FIBS_ClearScreen, "^.\\[;H.\\[2J");
                addDough(alphaBatch, FibsCookies.FIBS_Timeout, "^Connection timed out\\.");
                addDough(alphaBatch, FibsCookies.FIBS_Goodbye, "           Goodbye\\.");
                addDough(alphaBatch, FibsCookies.FIBS_LastLogin, "^  Last login:");
                addDough(alphaBatch, FibsCookies.FIBS_NoInfo, "^No information found on user");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookiePipCount, "^Pipcounts: [0-9a-zA-Z_<>]+ ");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieBADBearingOff, "^Bearing off: .+ moves");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSavedCount, " has [0-9]+ saved game");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSavedCountZero, " has no saved games\\.");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieSayToYourself, "^You say to yourself:");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieNetworkError, "^Network error with");
                addDough(alphaBatch, FibsCookies.BGOFIBSCookieJoinedUnlimitedMatch, "^Player [a-zA-Z_<>]+ has joined you for an unlimited match\\.");
                
                numericBatch=startBatch(Clip.CLIP_WHO_INFO, "^5 [^ ]+ - - [01]");
                addDough(numericBatch, Clip.CLIP_WHO_INFO, "^5 [^ ]+ [^ ]+ - [01]");
                addDough(numericBatch, Clip.CLIP_WHO_INFO, "^5 [^ ]+ - [^ ]+ [01]");
                
                addDough(numericBatch, FibsCookies.FIBS_Average, "^[0-9][0-9]:[0-9][0-9]-");
                addDough(numericBatch, FibsCookies.FIBS_DiceTest, "^[1-6]-1 [0-9]");
                addDough(numericBatch, FibsCookies.FIBS_DiceTest, "^[1-6]: [0-9]");
                addDough(numericBatch, FibsCookies.FIBS_Stat, "^[0-9]+ bytes");
                addDough(numericBatch, FibsCookies.FIBS_Stat, "^[0-9]+ accounts");
                addDough(numericBatch, FibsCookies.FIBS_Stat, "^[0-9]+ ratings saved. reset log");
                addDough(numericBatch, FibsCookies.FIBS_Stat, "^[0-9]+ registered users.");
                addDough(numericBatch, FibsCookies.FIBS_Stat, "^[0-9]+\\([0-9]+\\) saved games check by cron");
                addDough(numericBatch, Clip.CLIP_WHO_END, "^6$");
                addDough(numericBatch, Clip.CLIP_SHOUTS, "^13 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_SAYS, "^12 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_WHISPERS, "^14 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_KIBITZES, "^15 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_YOU_SAY, "^16 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_YOU_SHOUT, "^17 ");
                addDough(numericBatch, Clip.CLIP_YOU_WHISPER, "^18 ");
                addDough(numericBatch, Clip.CLIP_YOU_KIBITZ, "^19 ");
                addDough(numericBatch, Clip.CLIP_LOGIN, "^7 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_LOGOUT, "^8 [a-zA-Z_<>]+ ");
                addDough(numericBatch, Clip.CLIP_MESSAGE, "^9 [a-zA-Z_<>]+ [0-9]+ ");
                addDough(numericBatch, Clip.CLIP_MESSAGE_DELIVERED, "^10 [a-zA-Z_<>]+$");
                addDough(numericBatch, Clip.CLIP_MESSAGE_SAVED, "^11 [a-zA-Z_<>]+$");
                addDough(numericBatch, Clip.CLIP_ALERT, "^20 [a-zA-Z_<>]+ ");
                
                starsBatch=startBatch(FibsCookies.FIBS_Username, "^\\*\\* User");
                addDough(starsBatch, FibsCookies.FIBS_Junk, "^\\*\\* You tell ");
                addDough(starsBatch, FibsCookies.FIBS_YouGag, "^\\*\\* You gag");
                addDough(starsBatch, FibsCookies.FIBS_YouUngag, "^\\*\\* You ungag");
                addDough(starsBatch, FibsCookies.FIBS_YouBlind, "^\\*\\* You blind");
                addDough(starsBatch, FibsCookies.FIBS_YouUnblind, "^\\*\\* You unblind");
                addDough(starsBatch, FibsCookies.FIBS_UseToggleReady, "^\\*\\* Use 'toggle ready' first");
                addDough(starsBatch, FibsCookies.FIBS_NewMatchAck9, "^\\*\\* You are now playing an unlimited match with ");
                addDough(starsBatch, FibsCookies.FIBS_NewMatchAck10, "^\\*\\* You are now playing a [0-9]+ point match with ");
                addDough(starsBatch, FibsCookies.FIBS_NewMatchAck2, "^\\*\\* Player [a-zA-Z_<>]+ has joined you for a");
                addDough(starsBatch, FibsCookies.FIBS_YouTerminated, "^\\*\\* You terminated the game");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieNoOneToLeave, "^\\*\\* Error: No one to leave");
                addDough(starsBatch, FibsCookies.FIBS_OpponentLeftGame, "^\\*\\* Player [a-zA-Z_<>]+ has left the game. The game was saved\\.");
                addDough(starsBatch, FibsCookies.FIBS_PlayerLeftGame, "has left the game\\.");
                addDough(starsBatch, FibsCookies.FIBS_YouInvited, "^\\*\\* You invited");
                addDough(starsBatch, FibsCookies.FIBS_YourLastLogin, "^\\*\\* Last login:");
                addDough(starsBatch, FibsCookies.FIBS_NoOne, "^\\*\\* There is no one called");
                addDough(starsBatch, FibsCookies.FIBS_AllowpipFalse, "^\\*\\* You don't allow the use of the server's 'pip' command\\.");
                addDough(starsBatch, FibsCookies.FIBS_AllowpipTrue, "^\\*\\* You allow the use the server's 'pip' command\\.");
                addDough(starsBatch, FibsCookies.FIBS_AutoboardFalse, "^\\*\\* The board won't be refreshed");
                addDough(starsBatch, FibsCookies.FIBS_AutoboardTrue, "^\\*\\* The board will be refreshed");
                addDough(starsBatch, FibsCookies.FIBS_AutodoubleTrue, "^\\*\\* You agree that doublets");
                addDough(starsBatch, FibsCookies.FIBS_AutodoubleFalse, "^\\*\\* You don't agree that doublets");
                addDough(starsBatch, FibsCookies.FIBS_AutomoveFalse, "^\\*\\* Forced moves won't");
                addDough(starsBatch, FibsCookies.FIBS_AutomoveTrue, "^\\*\\* Forced moves will");
                addDough(starsBatch, FibsCookies.FIBS_BellFalse, "^\\*\\* Your terminal won't ring");
                addDough(starsBatch, FibsCookies.FIBS_BellTrue, "^\\*\\* Your terminal will ring");
                addDough(starsBatch, FibsCookies.FIBS_CrawfordFalse, "^\\*\\* You would like to play without using the Crawford rule\\.");
                addDough(starsBatch, FibsCookies.FIBS_CrawfordTrue, "^\\*\\* You insist on playing with the Crawford rule\\.");
                addDough(starsBatch, FibsCookies.FIBS_DoubleFalse, "^\\*\\* You won't be asked if you want to double\\.");
                addDough(starsBatch, FibsCookies.FIBS_DoubleTrue, "^\\*\\* You will be asked if you want to double\\.");
                addDough(starsBatch, FibsCookies.FIBS_GreedyTrue, "^\\*\\* Will use automatic greedy bearoffs\\.");
                addDough(starsBatch, FibsCookies.FIBS_GreedyFalse, "^\\*\\* Won't use automatic greedy bearoffs\\.");
                addDough(starsBatch, FibsCookies.FIBS_MoreboardsTrue, "^\\*\\* Will send rawboards after rolling\\.");
                addDough(starsBatch, FibsCookies.FIBS_MoreboardsFalse, "^\\*\\* Won't send rawboards after rolling\\.");
                addDough(starsBatch, FibsCookies.FIBS_MovesTrue, "^\\*\\* You want a list of moves after this game\\.");
                addDough(starsBatch, FibsCookies.FIBS_MovesFalse, "^\\*\\* You won't see a list of moves after this game\\.");
                addDough(starsBatch, FibsCookies.FIBS_NotifyFalse, "^\\*\\* You won't be notified");
                addDough(starsBatch, FibsCookies.FIBS_NotifyTrue, "^\\*\\* You'll be notified");
                addDough(starsBatch, FibsCookies.FIBS_RatingsTrue, "^\\*\\* You'll see how the rating changes are calculated\\.");
                addDough(starsBatch, FibsCookies.FIBS_RatingsFalse, "^\\*\\* You won't see how the rating changes are calculated\\.");
                addDough(starsBatch, FibsCookies.FIBS_ReadyTrue, "^\\*\\* You're now ready to invite or join someone\\.");
                addDough(starsBatch, FibsCookies.FIBS_ReadyFalse, "^\\*\\* You're now refusing to play with someone\\.");
                addDough(starsBatch, FibsCookies.FIBS_ReportFalse, "^\\*\\* You won't be informed");
                addDough(starsBatch, FibsCookies.FIBS_ReportTrue, "^\\*\\* You will be informed");
                addDough(starsBatch, FibsCookies.FIBS_SilentTrue, "^\\*\\* You won't hear what other players shout\\.");
                addDough(starsBatch, FibsCookies.FIBS_SilentFalse, "^\\*\\* You will hear what other players shout\\.");
                addDough(starsBatch, FibsCookies.FIBS_TelnetFalse, "^\\*\\* You use a client program");
                addDough(starsBatch, FibsCookies.FIBS_TelnetTrue, "^\\*\\* You use telnet");
                addDough(starsBatch, FibsCookies.FIBS_WrapFalse, "^\\*\\* The server will wrap");
                addDough(starsBatch, FibsCookies.FIBS_WrapTrue, "^\\*\\* Your terminal knows how to wrap");
                addDough(starsBatch, FibsCookies.FIBS_PlayerRefusingGames, "^\\*\\* [a-zA-Z_<>]+ is refusing games\\.");
                addDough(starsBatch, FibsCookies.FIBS_NotWatching, "^\\*\\* You're not watching\\.");
                addDough(starsBatch, FibsCookies.FIBS_NotWatchingPlaying, "^\\*\\* You're not watching or playing\\.");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieNotPlayingWatching, "^\\*\\* You're not playing or watching\\.");
                addDough(starsBatch, FibsCookies.FIBS_NotPlaying, "^\\*\\* You're not playing\\.");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieNotPlayingSoYouCantGiveUp, "^\\*\\* You're not playing, so you can't give up\\.");
                addDough(starsBatch, FibsCookies.BGOFIBSCookiePlayerDidntDoubleOrResign, "^\\*\\* [a-zA-Z_<>]+ didn't double or resign\\.");
                addDough(starsBatch, FibsCookies.FIBS_NoUser, "^\\*\\* There is no user called ");
                addDough(starsBatch, FibsCookies.FIBS_AlreadyPlaying, "is already playing with");
                addDough(starsBatch, FibsCookies.FIBS_DidntInvite, "^\\*\\* [a-zA-Z_<>]+ didn't invite you.");
                addDough(starsBatch, FibsCookies.FIBS_BadMove, "^\\*\\* You can't remove this piece");
                addDough(starsBatch, FibsCookies.FIBS_CantMoveFirstMove, "^\\*\\* You can't move ");
                addDough(starsBatch, FibsCookies.FIBS_CantShout, "^\\*\\* Please type 'toggle silent' again before you shout\\.");
                addDough(starsBatch, FibsCookies.FIBS_MustMove, "^\\*\\* You must give [1-4] moves");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieDontGiveMore, "^\\*\\* Please don't give more than [1-4] moves\\.");
                addDough(starsBatch, FibsCookies.FIBS_MustComeIn, "^\\*\\* You have to remove pieces from the bar in your first move\\.");
                addDough(starsBatch, FibsCookies.FIBS_UsersHeardYou, "^\\*\\* [0-9]+ users? heard you\\.");
                addDough(starsBatch, FibsCookies.FIBS_Junk, "^\\*\\* Please wait for [a-zA-Z_<>]+ to join too\\.");
                addDough(starsBatch, FibsCookies.FIBS_SavedMatchReady, "^\\*\\*[a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +[0-9]+");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieSavedUnlimitedMatchReady, "^\\*\\*[a-zA-Z_<>]+ +unlimited +[0-9]+ +- +[0-9]+");
                addDough(starsBatch, FibsCookies.FIBS_NotYourTurnToRoll, "^\\*\\* It's not your turn to roll the dice\\.");
                addDough(starsBatch, FibsCookies.FIBS_NotYourTurnToMove, "^\\*\\* It's not your turn to move\\.");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieNotYourTurnToDouble, "^\\*\\* It's not your turn to double");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieRollDiceBeforeMove, "^\\*\\* You have to roll the dice before moving\\.");
                addDough(starsBatch, FibsCookies.BGOFIBSCookieIllegalMoveCommand, "legal words are 'bar', 'home', 'off', 'b', 'h' and 'o'\\.");
                addDough(starsBatch, FibsCookies.FIBS_YouStopWatching, "^\\*\\* You stop watching");
                addDough(starsBatch, FibsCookies.FIBS_UnknownCommand, "^\\*\\* Unknown command:");
                addDough(starsBatch,FibsCookies.FIBS_CantWatch, "^\\*\\* You can't watch another game while you're playing\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieWatchWho, "^\\*\\* Watch who?");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieMirror, "^\\*\\* Use a mirror to do that\\.");
                addDough(starsBatch,FibsCookies.FIBS_CantInviteSelf, "^\\*\\* You can't invite yourself\\.");
                addDough(starsBatch,FibsCookies.FIBS_DontKnowUser, "^\\*\\* Don't know user");
                addDough(starsBatch,FibsCookies.FIBS_MessageUsage, "^\\*\\* usage: message <user> <text>");
                addDough(starsBatch,FibsCookies.FIBS_PlayerNotPlaying, "^\\*\\* [a-zA-Z_<>]+ is not playing\\.");
                addDough(starsBatch,FibsCookies.FIBS_CantTalk, "^\\*\\* You can't talk if you won't listen\\.");
                addDough(starsBatch,FibsCookies.FIBS_WontListen, "^\\*\\* [a-zA-Z_<>]+ won't listen to you\\.");
                addDough(starsBatch,FibsCookies.FIBS_Why, "Why would you want to do that");	// (not sure about ** vs *** at front of line.)
                addDough(starsBatch,FibsCookies.FIBS_Ratings, "^\\* *[0-9]+ +[a-zA-Z_<>]+ +[0-9]+\\.[0-9]+ +[0-9]+");
                addDough(starsBatch,FibsCookies.FIBS_NoSavedMatch, "^\\*\\* There's no saved match with ");
                addDough(starsBatch,FibsCookies.FIBS_WARNINGSavedMatch, "^\\*\\* WARNING: Don't accept if you want to continue");
                addDough(starsBatch,FibsCookies.FIBS_CantGagYourself, "^\\*\\* You talk too much, don't you\\?");
                addDough(starsBatch,FibsCookies.FIBS_CantBlindYourself, "^\\*\\* You can't read this message now, can you\\?");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieShoutingDisabled, "^\\*\\* shouting has been disabled for you");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieBoardStyleValueInvalid, "^\\*\\* Valid arguments are the numbers 1 to 3\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieJoinWho, "^\\*\\* Error: Join who\\?");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieSavedMatchCorrupt, "^\\*\\* ERROR: Saved match is corrupt\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieCantPlayTwoGames, "^\\*\\* You can't play two games at once");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieInviteWho, "^\\*\\* invite who");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieWaitForLastInvitation, "^\\*\\* Please wait for your last invitation to be accepted\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieNotExperiencedEnough, "^\\*\\* You're not experienced enough to play a match of that length\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieYouAreAlreadyPlaying, "^\\*\\* You are already playing\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieYouAlreadyDidThat, "^\\*\\* You already did that\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieYouDidAlreadyRoll, "^\\*\\* You did already roll the dice\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieYouCanOnlyDouble, "^\\*\\* You can only double before you roll the dice\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieWaitUntilMoved, "^\\*\\* Please wait until user has moved\\.");
                // Pipcounts

                addDough(starsBatch,FibsCookies.BGOFIBSCookiePlayerDoesntAllowPip,"^\\*\\* [a-zA-Z_<>]+ doesn't allow this. Ask [a-zA-Z_<>]+ to 'toggle allowpip'");

                addDough(starsBatch,FibsCookies.BGOFIBSCookieTypeAllowPipFirst,"^\\*\\* Type 'toggle allowpip' first\\.");

                /*
                 * CLIP Spec notes: "Note that the allowpip toggle has no effect
                 * when watching a game. You can always get a pip count for the
                 * two players."
                 *                 
                 * But the command ref for pip
                 * http://www.fibs.com/CommandReference/pip.html states "This
                 * command can also be used while watching a game if both
                 * players have set their allowpip-toggle to YES."
                 *                 
                * Indeed we've got the following message once
                 */

                addDough(starsBatch,FibsCookies.BGOFIBSCookiePlayersDontAllowPipCounts,"^\\*\\* The players don't allow pipcounts\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieWaitUntilAcceptReject, "^\\*\\* Wait until [a-zA-Z_<>]+ accepted or rejected your resign\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieWantedToResign, "^\\*\\* [a-zA-Z_<>]+ wanted to resign. Type 'accept' or 'reject'\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieDoesntWantYouToWatch, "^\\*\\* [a-zA-Z_<>]+ doesn't want you to watch\\.");
                addDough(starsBatch,FibsCookies.BGOFIBSCookieNowAllowedToCreateNewGames,"\\*\\* You are not allowed to create new games\\.");
                
                loginBatch = startBatch(FibsCookies.FIBS_LoginPrompt, "^login:");
                addDough(loginBatch,Clip.CLIP_WELCOME, "^1 [a-zA-Z_<>]+ [0-9]+ ");
                addDough(loginBatch,Clip.CLIP_OWN_INFO, "^2 [a-zA-Z_<>]+ [01] [01]");
                addDough(loginBatch,Clip.CLIP_MOTD_BEGIN, "^3"); // MODIFIED
                addDough(loginBatch,FibsCookies.BGOFIBSCookieGuestWelcome, "^Welcome to (FIBS|TiGa). You just logged in as guest.");
                addDough(loginBatch,FibsCookies.FIBS_FailedLogin, "^> [0-9]+");	// bogus CLIP messages sent after a failed login

                bgOfFibsGuestLoginBatch = startBatch(FibsCookies.BGOFIBSCookieGuestPrompt, "^> ");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieUserNameInUse, "^\\*\\* Please use another name");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieInvalidUserName, "^\\*\\* Your name may only contain");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieTypeInNoPassword, "^Type in no password and hit");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookiePasswordPrompt, "Please give your password");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieInvalidPassword, "^Minimal password length is");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieRetypePasswordPrompt, "^Please retype your password");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookiePasswordsNotIdentical, "^\\*\\* The two passwords were not identical");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieUserNameRegistered, "^You are registered");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieWelcomeBack, "^Welcome back");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieNotAway, "^\\*\\* You're not away");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.BGOFIBSCookieUserNameTooLong,"\\*\\* Please use a name with at most 20 characters\\.");
                addDough(bgOfFibsGuestLoginBatch,FibsCookies.FIBS_Timeout,"^Connection timed out\\.");

                // Only interested in one message here, but we still use a message list for simplicity and consistency.
                motdBatch = startBatch(Clip.CLIP_MOTD_END, "^4"); // MODIFIED
                msgState = MessageState.FIBS_LOGIN_STATE;
	    }
	    
	    private static CookieDough startBatch(int cookie, String str){
	        CookieDough newCookie = addCookieDough(cookie, str);
	        return newCookie;
	    }
            
            private static void addDough(CookieDough cd, int message, String str){
                cd.next=addCookieDough(message, str);
            }
            
            private static CookieDough addCookieDough(int message,String re){
                // enough?
                CookieDough newDough= new CookieDough(message, re);
                return newDough;
            }
            
            private static CookieDough releaseCookieDough(CookieDough theDough){
                if (theDough==null) return null;
                CookieDough nextDough= theDough.next;
                theDough.p=null;
                return nextDough;
            }
	//
}
