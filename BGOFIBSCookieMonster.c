// Original notes for the FIBS cookie monster source:

/*
 *---  FIBSCookieMonster.c --------------------------------------------------
 *
 *  Created by Paul Ferguson on Tue Dec 24 2002.
 *  Copyright (c) 2003 Paul Ferguson. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * The name of Paul D. Ferguson may not be used to endorse or promote
 *   products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *---------------------------------------------------------------------------
 */

#include "BGOFIBSCookieMonster.h"

#include <ctype.h>
#include <sys/types.h>
#include <stddef.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <regex.h>

#define TEST_FIBSCOOKIEMONSTER 0		// see main(), below

// A simple state model
static enum
{
  FIBS_UNINITIALIZED_STATE,
  FIBS_LOGIN_STATE,
  FIBS_MOTD_STATE,
  FIBS_RUN_STATE,
  FIBS_LOGOUT_STATE,
  BGOFIBSStateGuestLogin
} MessageState = FIBS_UNINITIALIZED_STATE;


// Principle data structure. Used internally--clients never see the dough,
// just the finished cookie.
typedef struct CookieDough
{
  regex_t 				regex;
  int						cookie;
  struct CookieDough * 	next;
} CookieDough;

static CookieDough * LoginBatch	= NULL;		// for LOGIN_STATE
static CookieDough * BGOFIBSGuestLoginBatch = NULL;

static CookieDough * MOTDBatch		= NULL;		// for MOTD_STATE

static CookieDough * AlphaBatch	= NULL;		// for RUN_STATE

static CookieDough * NumericBatch	= NULL;
static CookieDough * StarsBatch	= NULL;

// Private functions
static void PrepareBatches();
static CookieDough * AddCookieDough(int message, const char * re);
static CookieDough * ReleaseCookieDough(CookieDough * theDough);


#pragma mark --- Public functions ---

// Returns a message ID (see FIBSCookieMonster.h and clip.h), or -1 if
// the FCM failed to initialize.
//
// NOTE: The incoming FIBS message should NOT include line terminators.


void BGOSetFIBSCookieMonsterToRunState()
{
  // If this is first use, initialize everything (can also use ResetFIBSCookieMonster to do this).
  if (MessageState == FIBS_UNINITIALIZED_STATE)
    PrepareBatches();
  
  MessageState = FIBS_RUN_STATE;
}

int FIBSCookie(const char * theMessage)
{
  // If this is first use, initialize everything (can also use ResetFIBSCookieMonster to do this).
  if (MessageState == FIBS_UNINITIALIZED_STATE)
    PrepareBatches();
  
  CookieDough * ptr;
  switch (MessageState)
  {
    case FIBS_UNINITIALIZED_STATE:
      return FIBS_BAD_COOKIE;		// We failed to initialize for some reason?
      break;
      
    case FIBS_RUN_STATE:
    {
      if (strlen(theMessage) == 0)
        return FIBS_Empty;

      int result = FIBS_Unknown;
      register const char ch = theMessage[0];

      if (isdigit(ch))		// CLIP messages and miscellaneous numeric messages
      {
        for (ptr = NumericBatch; (ptr); ptr = ptr->next)
        {
          if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
          {
            result = ptr->cookie;
            break;
          }
        }
      }
      else if (ch == '*')	// '** ' messages
      {
        result = BGOFIBSCookieStarsUnknown;

        for (ptr = StarsBatch; (ptr); ptr = ptr->next)
        {
          if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
          {
            result = ptr->cookie;
            break;
          }
        }
      }
      else	// all other messages
      {
        for (ptr = AlphaBatch; (ptr); ptr = ptr->next)
        {
          if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
          {
            result = ptr->cookie;
            break;
          }
        }
      }
      if (result == FIBS_Goodbye)
        MessageState = FIBS_LOGOUT_STATE;

      return result;
    }
      break;
      
    case FIBS_LOGIN_STATE:
    {
      int result = FIBS_PreLogin;
      for (ptr = LoginBatch; (ptr); ptr = ptr->next)
      {
        if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
        {
          result = ptr->cookie;
          break;
        }
      }
      
      if (result == BGOFIBSCookieGuestWelcome)
        MessageState = BGOFIBSStateGuestLogin;
      
      if (result == CLIP_MOTD_BEGIN)
        MessageState = FIBS_MOTD_STATE;
      
      return result;
    }
      break;
      
    case BGOFIBSStateGuestLogin:
    {
      int result = FIBS_Junk;
      for (ptr = BGOFIBSGuestLoginBatch; (ptr); ptr = ptr->next)
      {
        if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
        {
          result = ptr->cookie;
          break;
        }
      }
      
      return result;
    }
      break;
      
    case FIBS_MOTD_STATE:
    {
      int result = FIBS_MOTD;
      for (ptr = MOTDBatch; (ptr); ptr = ptr->next)
      {
        if (regexec(&(ptr->regex), theMessage, 0, NULL, 0) == 0)
        {
          result = ptr->cookie;
          break;
        }
      }
      if (result == CLIP_MOTD_END)
        MessageState = FIBS_RUN_STATE;
      
      return result;
    }
      break;
      
    case FIBS_LOGOUT_STATE:
    {
      return FIBS_PostGoodbye;
    }
      break;
  }
  return FIBS_Unknown;
}


// Call this function to reset before reconnecting to FIBS.
// If the batches have already been initialized, just reset the
// message state, else do the initialization.
//
// Note that it is not necessary to call this function before calling
// FIBSCookie(), which calls PrepareBatches() on first use if needed.
// You can call this function first however, to make sure things
// are ready to go when you connect.

void ResetFIBSCookieMonster()
{
  if (MessageState == FIBS_UNINITIALIZED_STATE)
    PrepareBatches();
  else
    MessageState = FIBS_LOGIN_STATE;
}


// Call this to release the memory used by FIBSCookieMonster.
// You normally don't need to use this function, since everything
// will be cleaned up when your application terminates.

void ReleaseFIBSCookieMonster()
{
  if (MessageState == FIBS_UNINITIALIZED_STATE)
    return;
  
  // NOTE: The for() loop's body is empty, all work done inside the for() statement.
#define TRASH_BATCH(startingPoint) { CookieDough * m; for (m = startingPoint; (m); m = ReleaseCookieDough(m)); startingPoint = NULL; }
  
  TRASH_BATCH(AlphaBatch)
  TRASH_BATCH(StarsBatch)
  TRASH_BATCH(NumericBatch)
  TRASH_BATCH(LoginBatch)
  TRASH_BATCH(BGOFIBSGuestLoginBatch)
  TRASH_BATCH(MOTDBatch)
  
  MessageState = FIBS_UNINITIALIZED_STATE;
  
#undef TRASH_BATCH
}

#pragma mark --- Private functions ---


// Initialize stuff, ready to start pumping out cookies by the thousands.
// Note that the order of items in this function is important, in some cases
// messages are very similar and are differentiated by depending on the
// order the batch is processed.

static void PrepareBatches()
{
  CookieDough * current;
  
#define START_BATCH(map, message, re) current = map = AddCookieDough(message, re); if (current == NULL) return;
#define ADD_DOUGH(message, re) current = current->next = AddCookieDough(message, re); if (current == NULL) return;
  
  START_BATCH(AlphaBatch, FIBS_Board, 	"^board:[a-zA-Z_<>]+:[a-zA-Z_<>]+:[0-9:\\-]+$");
  ADD_DOUGH(FIBS_BAD_Board,				"^board:");
  ADD_DOUGH(FIBS_YouRoll,					"^You roll [1-6] and [1-6]");
  ADD_DOUGH(FIBS_PlayerRolls,				"^[a-zA-Z_<>]+ rolls [1-6] and [1-6]");
  ADD_DOUGH(FIBS_RollOrDouble,			"^It's your turn to roll or double\\.");
  ADD_DOUGH(FIBS_RollOrDouble,			"^It's your turn\\. Please roll or double");
  ADD_DOUGH(FIBS_AcceptRejectDouble,		"doubles\\. Type 'accept' or 'reject'\\.");
  ADD_DOUGH(FIBS_Doubles,					"^[a-zA-Z_<>]+ doubles\\.");
  ADD_DOUGH(FIBS_PleaseMove,				"^Please move [1-4] pieces?\\.");
  ADD_DOUGH(FIBS_PlayerMoves,				"^[a-zA-Z_<>]+ moves");
  ADD_DOUGH(FIBS_BearingOff,				"^Bearing off:");
  ADD_DOUGH(FIBS_YouReject,				"^You reject\\. The game continues\\.");
  ADD_DOUGH(FIBS_YouStopWatching,			"You're not watching anymore\\.");		// overloaded	//PLAYER logs out.. You're not watching anymore.
  ADD_DOUGH(BGOFIBSCookieBannedFromWatching,	"^[a-zA-Z_<>]+ bans you from watching\\.");
  ADD_DOUGH(BGOFIBSCookieOpponentDrops,	"drops connection. The game was saved\\.");
  ADD_DOUGH(FIBS_OpponentLogsOut,			"logs out. The game was saved\\.");					// PLAYER logs out. The game was saved. ||  PLAYER drops connection. The game was saved. MODIFIED
  ADD_DOUGH(FIBS_OnlyPossibleMove,		"^The only possible move is");
  ADD_DOUGH(FIBS_FirstRoll,				"[a-zA-Z_<>]+ rolled [1-6].+rolled [1-6]");
  ADD_DOUGH(FIBS_MakesFirstMove,			" makes the first move\\.");
  ADD_DOUGH(FIBS_YouDouble,				"^You double\\. Please wait for ");	// You double. Please wait for PLAYER to accept or reject.
  
  ADD_DOUGH(
            FIBS_PlayerWantsToResign,
            "^[a-zA-Z_<>]+ wants to resign\\. You will win [0-9]+ points?\\. Type 'accept' or 'reject'\\."
            );
  
  ADD_DOUGH(FIBS_WatchResign,				"^[a-zA-Z_<>]+ wants to resign\\. ");	// PLAYER wants to resign. PLAYER2 will win 2 points.  (ORDER MATTERS HERE)
  ADD_DOUGH(FIBS_YouResign,				"^You want to resign\\.");	// You want to resign. PLAYER will win 1 .
  ADD_DOUGH(FIBS_ResumeMatchAck5,			"^You are now playing with [a-zA-Z_<>]+\\. Your running match was loaded\\.");
  ADD_DOUGH(FIBS_JoinNextGame,			"^Type 'join' if you want to play the next game, type 'leave' if you don't\\.");
  
  
  
  ADD_DOUGH(FIBS_NewMatchRequest,			"^[a-zA-Z_<>]+ wants to play a [0-9]+ point match with you\\.");
  ADD_DOUGH(FIBS_WARNINGSavedMatch,		"^WARNING: Don't accept if you want to continue");
  ADD_DOUGH(FIBS_ResignRefused,			"rejects\\. The game continues\\.");
  ADD_DOUGH(FIBS_MatchLength,				"^match length:");
  ADD_DOUGH(FIBS_TypeJoin,				"^Type 'join [a-zA-Z_<>]+' to accept\\.");
  ADD_DOUGH(FIBS_YouAreWatching,			"^You're now watching ");
  ADD_DOUGH(FIBS_YouStopWatching,			"^You stop watching ");		// overloaded
  ADD_DOUGH(FIBS_PlayerStartsWatching,	"[a-zA-Z_<>]+ starts watching [a-zA-Z_<>]+\\.");
  ADD_DOUGH(FIBS_PlayerStartsWatching,	"[a-zA-Z_<>]+ is watching you\\.");
  ADD_DOUGH(FIBS_PlayerStopsWatching,		"[a-zA-Z_<>]+ stops watching [a-zA-Z_<>]+\\.");
  ADD_DOUGH(FIBS_PlayerIsWatching,		"[a-zA-Z_<>]+ is watching ");
  ADD_DOUGH(FIBS_ResignWins,				"^[a-zA-Z_<>]+ gives up\\. [a-zA-Z_<>]+ wins [0-9]+ points?\\.");	// PLAYER1 gives up. PLAYER2 wins 1 point.
  
  ADD_DOUGH(
            BGOFIBSCookieResignYouWinWithOldmovesScoreUpdate,
            "^[a-zA-Z_<>]+ gives up\\. You win [0-9]+ points?\\.Score is"
            );
  
  ADD_DOUGH(FIBS_ResignYouWin,			"^[a-zA-Z_<>]+ gives up\\. You win [0-9]+ points?\\.");
  
  ADD_DOUGH(
            BGOFIBSCookieYouAcceptAndWinWithOldmovesScoreUpdate,
            "^You accept and win [0-9]+ points?\\.Score is"
            );
  
  ADD_DOUGH(FIBS_YouAcceptAndWin,			"^You accept and win [0-9]+");
  
  
  ADD_DOUGH(
            BGOFIBSCookieAcceptWinsWithOldmovesScoreUpdate,
            "^[a-zA-Z_<>]+ accepts and wins [0-9]+ points?\\.Score is"
            );
  
  ADD_DOUGH(FIBS_AcceptWins,				"^[a-zA-Z_<>]+ accepts and wins [0-9]+ point");
  
  ADD_DOUGH(FIBS_PlayersStartingMatch,	"^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ start a [0-9]+ point match");	// PLAYER and PLAYER start a <n> point match.
  ADD_DOUGH(FIBS_StartingNewGame,			"^Starting a new game with ");
  
  ADD_DOUGH(
            BGOFIBSCookieYouGiveUpWithOldmovesScoreUpdate,
            "^You give up\\. [a-zA-Z_<>]+ wins [0-9]+ points?\\.Score is"
            );
  
  ADD_DOUGH(FIBS_YouGiveUp,				"^You give up\\. ");
  
  ADD_DOUGH(FIBS_YouWinMatch, "^You win the [0-9]+ point match");
  ADD_DOUGH(FIBS_PlayerWinsMatch,			"^[a-zA-Z_<>]+ wins the [0-9]+ point match");	//PLAYER wins the 3 point match 3-0 .
  ADD_DOUGH(FIBS_ResumingUnlimitedMatch,	"^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ are resuming their unlimited match\\.");
  ADD_DOUGH(FIBS_ResumingLimitedMatch,	"^[a-zA-Z_<>]+ and [a-zA-Z_<>]+ are resuming their [0-9]+-point match\\.");
  ADD_DOUGH(FIBS_MatchResult,				"^[a-zA-Z_<>]+ wins a [0-9]+ point match against ");	//PLAYER wins a 9 point match against PLAYER  11-6 .
  ADD_DOUGH(FIBS_PlayerWantsToResign,		"wants to resign\\.");		//  Same as a longline in an actual game  This is just for watching.
  
  ADD_DOUGH(BGOFIBSCookieYouAcceptDoubleBad,           "^You accept the double\\. The cube shows [0-9]+\\..+");
  ADD_DOUGH(FIBS_BAD_AcceptDouble,                    "^[a-zA-Z_<>]+ accepts? the double\\. The cube shows [0-9]+\\..+");
  ADD_DOUGH(FIBS_YouAcceptDouble,                     "^You accept the double\\. The cube shows");
  ADD_DOUGH(FIBS_PlayerAcceptsDouble,                 "^[a-zA-Z_<>]+ accepts the double\\. The cube shows ");
  ADD_DOUGH(BGOFIBSCookieWatchedPlayerAcceptsDouble,   "^[a-zA-Z_<>]+ accepts the double\\.");
  
  ADD_DOUGH(FIBS_ResumeMatchRequest,                  "^[a-zA-Z_<>]+ wants to resume a saved match with you\\.");
  ADD_DOUGH(FIBS_ResumeMatchAck0,                     "has joined you\\. Your running match was loaded");
  
  ADD_DOUGH(
            BGOFIBSCookieYouWinGameWithOldmovesScoreUpdate,
            "^You win the game and get [0-9]+ points?\\. Congratulations!Score is"
            );
  
  ADD_DOUGH(FIBS_YouWinGame,				"^You win the game and get");
  
  ADD_DOUGH(FIBS_UnlimitedInvite,			"^[a-zA-Z_<>]+ wants to play an unlimited match with you\\.");
  
  ADD_DOUGH(
            BGOFIBSCookiePlayerWinsGameWithOldmovesScoreUpdate,
            "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?\\. Sorry\\.Score is"
            );
  
  ADD_DOUGH(FIBS_PlayerWinsGame,                  "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?. Sorry.");
  ADD_DOUGH(BGOFIBSCookieWatchedPlayerWinsGame,    "^[a-zA-Z_<>]+ wins the game and gets [0-9]+ points?.");	// (when watching)
  ADD_DOUGH(FIBS_WatchGameWins,			"wins the game and gets");
  ADD_DOUGH(FIBS_PlayersStartingUnlimitedMatch,	"start an unlimited match\\.");	// PLAYER_A and PLAYER_B start an unlimited match.
  ADD_DOUGH(FIBS_ReportLimitedMatch,		"^[a-zA-Z_<>]+ +- +[a-zA-Z_<>]+ .+ point match");	// PLAYER_A        -       PLAYER_B (5 point match 2-2)
  ADD_DOUGH(FIBS_ReportUnlimitedMatch,	"^[a-zA-Z_<>]+ +- +[a-zA-Z_<>]+ \\(unlimited");
  ADD_DOUGH(FIBS_ShowMovesStart,			"^[a-zA-Z_<>]+ is X - [a-zA-Z_<>]+ is O");
  ADD_DOUGH(FIBS_ShowMovesRoll,			"^[XO]: \\([1-6]");	// ORDER MATTERS HERE
  ADD_DOUGH(FIBS_ShowMovesWins,			"^[XO]: wins");
  ADD_DOUGH(FIBS_ShowMovesDoubles,		"^[XO]: doubles");
  ADD_DOUGH(FIBS_ShowMovesAccepts,		"^[XO]: accepts");
  ADD_DOUGH(FIBS_ShowMovesRejects,		"^[XO]: rejects");
  
  ADD_DOUGH(
            BGOFIBSCookieOldmovesWantsToResign,
            "^[XO]: wants to resign"
            );
  
  ADD_DOUGH(FIBS_ShowMovesOther,			"^[XO]:");
  ADD_DOUGH(FIBS_ScoreUpdate,				"^score in [0-9]+ point match:");
  ADD_DOUGH(BGOFIBSCookieScoreUpdateUnlimited,  "^score in unlimited match:"); // score in unlimited match: user1-n user2-n
  
  ADD_DOUGH(
            BGOFIBSCookieOldmovesMatchStart,
            "^Score is [0-9]+-[0-9]+ in a [0-9]+ point match\\."
            );
  
  ADD_DOUGH(
            BGOFIBSCookieOldmovesUnlimitedMatchStart,
            "^Score is [0-9]+-[0-9]+ in an unlimited match\\."
            );
  
  ADD_DOUGH(FIBS_Settings,				"^Settings of variables:");
  ADD_DOUGH(FIBS_Turn,					"^turn:");
  ADD_DOUGH(FIBS_Boardstyle,				"^boardstyle:");
  ADD_DOUGH(FIBS_Linelength,				"^linelength:");
  ADD_DOUGH(FIBS_Pagelength,				"^pagelength:");
  ADD_DOUGH(FIBS_Redoubles,				"^redoubles:");
  ADD_DOUGH(FIBS_Sortwho,					"^sortwho:");
  ADD_DOUGH(FIBS_Timezone,				"^timezone:");
  ADD_DOUGH(FIBS_CantMove,				"^[a-zA-Z_<>]+ can't move");	// PLAYER can't move || You can't move
  ADD_DOUGH(FIBS_ListOfGames,				"^List of games:");
  ADD_DOUGH(FIBS_PlayerInfoStart,			"^Information about");
  ADD_DOUGH(FIBS_EmailAddress,			"^  Email address:");
  ADD_DOUGH(FIBS_NoEmail,					"^  No email address\\.");
  ADD_DOUGH(FIBS_WavesAgain,				"^[a-zA-Z_<>]+ waves goodbye again\\.");
  ADD_DOUGH(FIBS_Waves,					"waves goodbye");
  ADD_DOUGH(FIBS_Waves,					"^You wave goodbye\\.");
  ADD_DOUGH(FIBS_WavesAgain,				"^You wave goodbye again and log out\\.");
  ADD_DOUGH(FIBS_NoSavedGames,			"^no saved games\\.");
  ADD_DOUGH(FIBS_TypeBack,				"^You're away\\. Please type 'back'");
  ADD_DOUGH(FIBS_SavedMatch,				"^  [a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +");
  ADD_DOUGH(FIBS_SavedMatchPlaying,		"^ \\*[a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +");
  // NOTE: for FIBS_SavedMatchReady, see the Stars message, because it will appear to be one of those (has asterisk at index 0).
  
  ADD_DOUGH(
            BGOFIBSCookieSavedUnlimitedMatch,
            "^  [a-zA-Z_<>]+ +unlimited +[0-9]+ +- +"
            );
  
  ADD_DOUGH(
            BGOFIBSCookieSavedUnlimitedMatchPlaying,
            "^ \\*[a-zA-Z_<>]+ +unlimited +[0-9]+ +- +"
            );
  
  ADD_DOUGH(FIBS_PlayerIsWaitingForYou,	"^[a-zA-Z_<>]+ is waiting for you to log in\\.");
  ADD_DOUGH(FIBS_IsAway,					"^[a-zA-Z_<>]+ is away: ");
  
  // Boolean settings
  ADD_DOUGH(FIBS_AllowpipTrue,			"^allowpip +YES");
  ADD_DOUGH(FIBS_AllowpipFalse,			"^allowpip +NO");
  ADD_DOUGH(FIBS_AutoboardTrue,			"^autoboard +YES");
  ADD_DOUGH(FIBS_AutoboardFalse,			"^autoboard +NO");
  ADD_DOUGH(FIBS_AutodoubleTrue,			"^autodouble +YES");
  ADD_DOUGH(FIBS_AutodoubleFalse,			"^autodouble +NO");
  ADD_DOUGH(FIBS_AutomoveTrue,			"^automove +YES");
  ADD_DOUGH(FIBS_AutomoveFalse,			"^automove +NO");
  ADD_DOUGH(FIBS_BellTrue,				"^bell +YES");
  ADD_DOUGH(FIBS_BellFalse,				"^bell +NO");
  ADD_DOUGH(FIBS_CrawfordTrue,			"^crawford +YES");
  ADD_DOUGH(FIBS_CrawfordFalse,			"^crawford +NO");
  ADD_DOUGH(FIBS_DoubleTrue,				"^double +YES");
  ADD_DOUGH(FIBS_DoubleFalse,				"^double +NO");
  ADD_DOUGH(FIBS_MoreboardsTrue,			"^moreboards +YES");
  ADD_DOUGH(FIBS_MoreboardsFalse,			"^moreboards +NO");
  ADD_DOUGH(FIBS_MovesTrue,				"^moves +YES");
  ADD_DOUGH(FIBS_MovesFalse,				"^moves +NO");
  ADD_DOUGH(FIBS_GreedyTrue,				"^greedy +YES");
  ADD_DOUGH(FIBS_GreedyFalse,				"^greedy +NO");
  ADD_DOUGH(FIBS_NotifyTrue,				"^notify +YES");
  ADD_DOUGH(FIBS_NotifyFalse,				"^notify +NO");
  ADD_DOUGH(FIBS_RatingsTrue,				"^ratings +YES");
  ADD_DOUGH(FIBS_RatingsFalse,			"^ratings +NO");
  ADD_DOUGH(FIBS_ReadyTrue,				"^ready +YES");
  ADD_DOUGH(FIBS_ReadyFalse,				"^ready +NO");
  ADD_DOUGH(FIBS_ReportTrue,				"^report +YES");
  ADD_DOUGH(FIBS_ReportFalse,				"^report +NO");
  ADD_DOUGH(FIBS_SilentTrue,				"^silent +YES");
  ADD_DOUGH(FIBS_SilentFalse,				"^silent +NO");
  ADD_DOUGH(FIBS_TelnetTrue,				"^telnet +YES");
  ADD_DOUGH(FIBS_TelnetFalse,				"^telnet +NO");
  ADD_DOUGH(FIBS_WrapTrue,				"^wrap +YES");
  ADD_DOUGH(FIBS_WrapFalse,				"^wrap +NO");
  
  ADD_DOUGH(FIBS_Junk,					"^Closed old connection with user");
  ADD_DOUGH(FIBS_Done,					"^Done\\.");
  ADD_DOUGH(FIBS_YourTurnToMove,			"^It's your turn to move\\.");
  ADD_DOUGH(FIBS_SavedMatchesHeader,		"^  opponent          matchlength   score \\(your points first\\)");
  ADD_DOUGH(FIBS_MessagesForYou,			"^There are messages for you:");
  ADD_DOUGH(BGOFIBSCookieRedoublesSetToNone,		"^Value of 'redoubles' set to 'none'\\.");
  ADD_DOUGH(BGOFIBSCookieRedoublesSetToUnlimited,	"^Value of 'redoubles' set to 'unlimited'\\.");
  ADD_DOUGH(FIBS_RedoublesSetTo,			"^Value of 'redoubles' set to [0-9]+\\.");
  ADD_DOUGH(BGOFIBSCookieSortWhoValue,			"^Value of 'sortwho' is");
  ADD_DOUGH(BGOFIBSCookieSortWhoValueSetTo,		"^Value of 'sortwho' set to");
  ADD_DOUGH(BGOFIBSCookieSortWhoValueInvalid,	"Try 'login', 'name', 'rating' or 'rrating'\\.");
  ADD_DOUGH(BGOFIBSCookieBoardStyleValue,		"^Value of 'boardstyle' is");
  ADD_DOUGH(BGOFIBSCookieBoardStyleValueSetTo,	"^Value of 'boardstyle' set to");
  ADD_DOUGH(FIBS_DoublingCubeNow,			"^The number on the doubling cube is now [0-9]+");
  ADD_DOUGH(FIBS_FailedLogin,				"^> [0-9]+");							// bogus CLIP messages sent after a failed login
  ADD_DOUGH(FIBS_Average,					"^Time (UTC)  average min max");
  ADD_DOUGH(FIBS_DiceTest,				"^[nST]: ");
  ADD_DOUGH(FIBS_LastLogout,				"^  Last logout:");
  ADD_DOUGH(FIBS_RatingCalcStart,			"^rating calculation:");
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^Probability that underdog wins:");
  ADD_DOUGH(FIBS_RatingCalcInfo,			"is 1-Pu if underdog wins");	// P=0.505861 is 1-Pu if underdog wins and Pu if favorite wins
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^Experience: ");					// Experience: fergy 500 - jfk 5832
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^K=max\\(1");						// K=max(1 ,		-Experience/100+5) for fergy: 1.000000
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^rating difference");
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^change for");					// change for fergy: 4*K*sqrt(N)*P=2.023443
  ADD_DOUGH(FIBS_RatingCalcInfo,			"^match length  ");
  ADD_DOUGH(FIBS_WatchingHeader,			"^Watching players:");
  ADD_DOUGH(FIBS_SettingsHeader,			"^The current settings are:");
  ADD_DOUGH(FIBS_AwayListHeader,			"^The following users are away:");
  ADD_DOUGH(FIBS_RatingExperience,		"^  Rating: +[0-9]+\\.");				// Rating: 1693.11 Experience: 5781
  ADD_DOUGH(FIBS_NotLoggedIn,				"^  Not logged in right now\\.");
  ADD_DOUGH(FIBS_IsPlayingWith,			"is playing with");
  ADD_DOUGH(FIBS_SavedScoreHeader,		"^opponent +matchlength");		//	opponent          matchlength   score (your points first)
  ADD_DOUGH(FIBS_StillLoggedIn,			"^  Still logged in\\.");			//  Still logged in. 2:12 minutes idle.
  ADD_DOUGH(FIBS_NoOneIsAway,				"^None of the users is away\\.");
  ADD_DOUGH(FIBS_PlayerListHeader,		"^No  S  username        rating  exp login    idle  from");
  ADD_DOUGH(FIBS_RatingsHeader,			"^ rank name            rating    Experience");
  ADD_DOUGH(FIBS_ClearScreen,				"^.\\[;H.\\[2J");				// ANSI clear screen sequence
  ADD_DOUGH(FIBS_Timeout,					"^Connection timed out\\.");
  ADD_DOUGH(FIBS_Goodbye,					"           Goodbye\\.");
  ADD_DOUGH(FIBS_LastLogin,				"^  Last login:");
  ADD_DOUGH(FIBS_NoInfo,					"^No information found on user");
  
  ADD_DOUGH(BGOFIBSCookiePipCount,				"^Pipcounts: [0-9a-zA-Z_<>]+ ");
  
  ADD_DOUGH(BGOFIBSCookieBADBearingOff,			"^Bearing off: .+ moves"); // Bearing off: 1 o PLAYER moves 1-off
  
  ADD_DOUGH(BGOFIBSCookieSavedCount,			" has [0-9]+ saved game"); // user has 12 saved games.
  ADD_DOUGH(BGOFIBSCookieSavedCountZero,		" has no saved games\\.");
  
  ADD_DOUGH(BGOFIBSCookieSayToYourself,			"^You say to yourself:");
  ADD_DOUGH(BGOFIBSCookieNetworkError,			"^Network error with"); // Example: Network error with bestmind. The game was saved.
  
  ADD_DOUGH(
            BGOFIBSCookieJoinedUnlimitedMatch,
            "^Player [a-zA-Z_<>]+ has joined you for an unlimited match\\."
            );
  
  
  
  //--- Numeric messages ---------------------------------------------------
  START_BATCH(NumericBatch, CLIP_WHO_INFO, "^5 [^ ]+ - - [01]");
  ADD_DOUGH(CLIP_WHO_INFO,				"^5 [^ ]+ [^ ]+ - [01]");
  ADD_DOUGH(CLIP_WHO_INFO,				"^5 [^ ]+ - [^ ]+ [01]");
  
  ADD_DOUGH(FIBS_Average,					"^[0-9][0-9]:[0-9][0-9]-");			// output of average command
  ADD_DOUGH(FIBS_DiceTest,				"^[1-6]-1 [0-9]");					// output of dicetest command
  ADD_DOUGH(FIBS_DiceTest,				"^[1-6]: [0-9]");
  ADD_DOUGH(FIBS_Stat,					"^[0-9]+ bytes");					// output from stat command
  ADD_DOUGH(FIBS_Stat,					"^[0-9]+ accounts");
  ADD_DOUGH(FIBS_Stat,					"^[0-9]+ ratings saved. reset log");
  ADD_DOUGH(FIBS_Stat,					"^[0-9]+ registered users.");
  ADD_DOUGH(FIBS_Stat,					"^[0-9]+\\([0-9]+\\) saved games check by cron");
  
  ADD_DOUGH(CLIP_WHO_END,					"^6$");
  ADD_DOUGH(CLIP_SHOUTS,					"^13 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_SAYS,					"^12 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_WHISPERS,				"^14 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_KIBITZES,				"^15 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_YOU_SAY,					"^16 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_YOU_SHOUT,				"^17 ");
  ADD_DOUGH(CLIP_YOU_WHISPER,				"^18 ");
  ADD_DOUGH(CLIP_YOU_KIBITZ,				"^19 ");
  ADD_DOUGH(CLIP_LOGIN,					"^7 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_LOGOUT,					"^8 [a-zA-Z_<>]+ ");
  ADD_DOUGH(CLIP_MESSAGE,					"^9 [a-zA-Z_<>]+ [0-9]+ ");
  ADD_DOUGH(CLIP_MESSAGE_DELIVERED,		"^10 [a-zA-Z_<>]+$");
  ADD_DOUGH(CLIP_MESSAGE_SAVED,			"^11 [a-zA-Z_<>]+$");
  
  ADD_DOUGH(
            CLIP_ALERT,
            "^20 [a-zA-Z_<>]+ "
            );
  
  //--- '**' messages ------------------------------------------------------
  START_BATCH(StarsBatch, FIBS_Username, "^\\*\\* User");
  ADD_DOUGH(FIBS_Junk,					"^\\*\\* You tell ");				// "** You tell PLAYER: xxxxx"
  ADD_DOUGH(FIBS_YouGag,					"^\\*\\* You gag");
  ADD_DOUGH(FIBS_YouUngag,				"^\\*\\* You ungag");
  ADD_DOUGH(FIBS_YouBlind,				"^\\*\\* You blind");
  ADD_DOUGH(FIBS_YouUnblind,				"^\\*\\* You unblind");
  ADD_DOUGH(FIBS_UseToggleReady,			"^\\*\\* Use 'toggle ready' first");
  ADD_DOUGH(FIBS_NewMatchAck9,			"^\\*\\* You are now playing an unlimited match with ");
  ADD_DOUGH(FIBS_NewMatchAck10,			"^\\*\\* You are now playing a [0-9]+ point match with ");	// ** You are now playing a 5 point match with PLAYER
  ADD_DOUGH(FIBS_NewMatchAck2,			"^\\*\\* Player [a-zA-Z_<>]+ has joined you for a");	// ** Player PLAYER has joined you for a 2 point match.
  ADD_DOUGH(FIBS_YouTerminated,			"^\\*\\* You terminated the game");
  ADD_DOUGH(BGOFIBSCookieNoOneToLeave,          "^\\*\\* Error: No one to leave");
  ADD_DOUGH(FIBS_OpponentLeftGame,		"^\\*\\* Player [a-zA-Z_<>]+ has left the game. The game was saved\\.");
  ADD_DOUGH(FIBS_PlayerLeftGame,			"has left the game\\.");		// overloaded
  ADD_DOUGH(FIBS_YouInvited,				"^\\*\\* You invited");
  ADD_DOUGH(FIBS_YourLastLogin,			"^\\*\\* Last login:");
  ADD_DOUGH(FIBS_NoOne,					"^\\*\\* There is no one called");
  ADD_DOUGH(FIBS_AllowpipFalse,			"^\\*\\* You don't allow the use of the server's 'pip' command\\.");
  ADD_DOUGH(FIBS_AllowpipTrue,			"^\\*\\* You allow the use the server's 'pip' command\\.");
  ADD_DOUGH(FIBS_AutoboardFalse,			"^\\*\\* The board won't be refreshed");
  ADD_DOUGH(FIBS_AutoboardTrue,			"^\\*\\* The board will be refreshed");
  ADD_DOUGH(FIBS_AutodoubleTrue,			"^\\*\\* You agree that doublets");
  ADD_DOUGH(FIBS_AutodoubleFalse,			"^\\*\\* You don't agree that doublets");
  ADD_DOUGH(FIBS_AutomoveFalse,			"^\\*\\* Forced moves won't");
  ADD_DOUGH(FIBS_AutomoveTrue,			"^\\*\\* Forced moves will");
  ADD_DOUGH(FIBS_BellFalse,				"^\\*\\* Your terminal won't ring");
  ADD_DOUGH(FIBS_BellTrue,				"^\\*\\* Your terminal will ring");
  ADD_DOUGH(FIBS_CrawfordFalse,			"^\\*\\* You would like to play without using the Crawford rule\\.");
  ADD_DOUGH(FIBS_CrawfordTrue,			"^\\*\\* You insist on playing with the Crawford rule\\.");
  ADD_DOUGH(FIBS_DoubleFalse,				"^\\*\\* You won't be asked if you want to double\\.");
  ADD_DOUGH(FIBS_DoubleTrue,				"^\\*\\* You will be asked if you want to double\\.");
  ADD_DOUGH(FIBS_GreedyTrue,				"^\\*\\* Will use automatic greedy bearoffs\\.");
  ADD_DOUGH(FIBS_GreedyFalse,				"^\\*\\* Won't use automatic greedy bearoffs\\.");
  ADD_DOUGH(FIBS_MoreboardsTrue,			"^\\*\\* Will send rawboards after rolling\\.");
  ADD_DOUGH(FIBS_MoreboardsFalse,			"^\\*\\* Won't send rawboards after rolling\\.");
  ADD_DOUGH(FIBS_MovesTrue,				"^\\*\\* You want a list of moves after this game\\.");
  ADD_DOUGH(FIBS_MovesFalse,				"^\\*\\* You won't see a list of moves after this game\\.");
  ADD_DOUGH(FIBS_NotifyFalse,				"^\\*\\* You won't be notified");
  ADD_DOUGH(FIBS_NotifyTrue,				"^\\*\\* You'll be notified");
  ADD_DOUGH(FIBS_RatingsTrue,				"^\\*\\* You'll see how the rating changes are calculated\\.");
  ADD_DOUGH(FIBS_RatingsFalse,			"^\\*\\* You won't see how the rating changes are calculated\\.");
  ADD_DOUGH(FIBS_ReadyTrue,				"^\\*\\* You're now ready to invite or join someone\\.");
  ADD_DOUGH(FIBS_ReadyFalse,				"^\\*\\* You're now refusing to play with someone\\.");
  ADD_DOUGH(FIBS_ReportFalse,				"^\\*\\* You won't be informed");
  ADD_DOUGH(FIBS_ReportTrue,				"^\\*\\* You will be informed");
  ADD_DOUGH(FIBS_SilentTrue,				"^\\*\\* You won't hear what other players shout\\.");
  ADD_DOUGH(FIBS_SilentFalse,				"^\\*\\* You will hear what other players shout\\.");
  ADD_DOUGH(FIBS_TelnetFalse,				"^\\*\\* You use a client program");
  ADD_DOUGH(FIBS_TelnetTrue,				"^\\*\\* You use telnet");
  ADD_DOUGH(FIBS_WrapFalse,				"^\\*\\* The server will wrap");
  ADD_DOUGH(FIBS_WrapTrue,				"^\\*\\* Your terminal knows how to wrap");
  ADD_DOUGH(FIBS_PlayerRefusingGames,		"^\\*\\* [a-zA-Z_<>]+ is refusing games\\.");
  ADD_DOUGH(FIBS_NotWatching,				"^\\*\\* You're not watching\\.");
  ADD_DOUGH(FIBS_NotWatchingPlaying,		"^\\*\\* You're not watching or playing\\.");
  ADD_DOUGH(BGOFIBSCookieNotPlayingWatching,	"^\\*\\* You're not playing or watching\\.");
  ADD_DOUGH(FIBS_NotPlaying,				"^\\*\\* You're not playing\\.");
  ADD_DOUGH(BGOFIBSCookieNotPlayingSoYouCantGiveUp,	"^\\*\\* You're not playing, so you can't give up\\.");
  ADD_DOUGH(BGOFIBSCookiePlayerDidntDoubleOrResign,	"^\\*\\* [a-zA-Z_<>]+ didn't double or resign\\.");
  ADD_DOUGH(FIBS_NoUser,					"^\\*\\* There is no user called ");
  ADD_DOUGH(FIBS_AlreadyPlaying,			"is already playing with");
  ADD_DOUGH(FIBS_DidntInvite,				"^\\*\\* [a-zA-Z_<>]+ didn't invite you.");
  ADD_DOUGH(FIBS_BadMove,					"^\\*\\* You can't remove this piece");
  ADD_DOUGH(FIBS_CantMoveFirstMove,		"^\\*\\* You can't move ");			// ** You can't move 3 points in your first move
  ADD_DOUGH(FIBS_CantShout,				"^\\*\\* Please type 'toggle silent' again before you shout\\.");
  ADD_DOUGH(FIBS_MustMove,				"^\\*\\* You must give [1-4] moves");
  ADD_DOUGH(BGOFIBSCookieDontGiveMore,			"^\\*\\* Please don't give more than [1-4] moves\\.");
  ADD_DOUGH(FIBS_MustComeIn,				"^\\*\\* You have to remove pieces from the bar in your first move\\.");
  ADD_DOUGH(FIBS_UsersHeardYou,			"^\\*\\* [0-9]+ users? heard you\\.");
  ADD_DOUGH(FIBS_Junk,					"^\\*\\* Please wait for [a-zA-Z_<>]+ to join too\\.");
  ADD_DOUGH(FIBS_SavedMatchReady,			"^\\*\\*[a-zA-Z_<>]+ +[0-9]+ +[0-9]+ +- +[0-9]+");		// double star before a name indicates you have a saved game with this player
  
  ADD_DOUGH(
            BGOFIBSCookieSavedUnlimitedMatchReady,
            "^\\*\\*[a-zA-Z_<>]+ +unlimited +[0-9]+ +- +[0-9]+"
            );
  
  ADD_DOUGH(FIBS_NotYourTurnToRoll,		"^\\*\\* It's not your turn to roll the dice\\.");
  ADD_DOUGH(FIBS_NotYourTurnToMove,		"^\\*\\* It's not your turn to move\\.");
  ADD_DOUGH(BGOFIBSCookieNotYourTurnToDouble,	"^\\*\\* It's not your turn to double");
  
  ADD_DOUGH(BGOFIBSCookieRollDiceBeforeMove,	"^\\*\\* You have to roll the dice before moving\\.");
  ADD_DOUGH(BGOFIBSCookieIllegalMoveCommand,	"legal words are 'bar', 'home', 'off', 'b', 'h' and 'o'\\.");
  
  ADD_DOUGH(FIBS_YouStopWatching,			"^\\*\\* You stop watching");
  ADD_DOUGH(FIBS_UnknownCommand,			"^\\*\\* Unknown command:");
  ADD_DOUGH(FIBS_CantWatch,				"^\\*\\* You can't watch another game while you're playing\\.");
  ADD_DOUGH(BGOFIBSCookieWatchWho,				"^\\*\\* Watch who?");
  ADD_DOUGH(BGOFIBSCookieMirror,				"^\\*\\* Use a mirror to do that\\.");
  ADD_DOUGH(FIBS_CantInviteSelf,			"^\\*\\* You can't invite yourself\\.");
  ADD_DOUGH(FIBS_DontKnowUser,			"^\\*\\* Don't know user");
  ADD_DOUGH(FIBS_MessageUsage,			"^\\*\\* usage: message <user> <text>");
  ADD_DOUGH(FIBS_PlayerNotPlaying,		"^\\*\\* [a-zA-Z_<>]+ is not playing\\.");
  ADD_DOUGH(FIBS_CantTalk,				"^\\*\\* You can't talk if you won't listen\\.");
  ADD_DOUGH(FIBS_WontListen,				"^\\*\\* [a-zA-Z_<>]+ won't listen to you\\.");
  ADD_DOUGH(FIBS_Why,						"Why would you want to do that");		// (not sure about ** vs *** at front of line.)
  ADD_DOUGH(FIBS_Ratings,					"^\\* *[0-9]+ +[a-zA-Z_<>]+ +[0-9]+\\.[0-9]+ +[0-9]+");
  ADD_DOUGH(FIBS_NoSavedMatch,			"^\\*\\* There's no saved match with ");
  ADD_DOUGH(FIBS_WARNINGSavedMatch,		"^\\*\\* WARNING: Don't accept if you want to continue");
  ADD_DOUGH(FIBS_CantGagYourself,			"^\\*\\* You talk too much, don't you\\?");
  ADD_DOUGH(FIBS_CantBlindYourself,		"^\\*\\* You can't read this message now, can you\\?");
  ADD_DOUGH(BGOFIBSCookieShoutingDisabled,		"^\\*\\* shouting has been disabled for you");
  ADD_DOUGH(BGOFIBSCookieBoardStyleValueInvalid,"^\\*\\* Valid arguments are the numbers 1 to 3\\.");
  ADD_DOUGH(BGOFIBSCookieJoinWho,				"^\\*\\* Error: Join who\\?");
  ADD_DOUGH(BGOFIBSCookieSavedMatchCorrupt,		"^\\*\\* ERROR: Saved match is corrupt\\.");
  ADD_DOUGH(BGOFIBSCookieCantPlayTwoGames,		"^\\*\\* You can't play two games at once");
  ADD_DOUGH(BGOFIBSCookieInviteWho,				"^\\*\\* invite who");
  ADD_DOUGH(BGOFIBSCookieWaitForLastInvitation,	"^\\*\\* Please wait for your last invitation to be accepted\\.");
  ADD_DOUGH(BGOFIBSCookieNotExperiencedEnough,	"^\\*\\* You're not experienced enough to play a match of that length\\.");
  ADD_DOUGH(BGOFIBSCookieYouAreAlreadyPlaying,	"^\\*\\* You are already playing\\.");
  ADD_DOUGH(BGOFIBSCookieYouAlreadyDidThat,		"^\\*\\* You already did that\\.");
  ADD_DOUGH(BGOFIBSCookieYouDidAlreadyRoll,		"^\\*\\* You did already roll the dice\\.");
  ADD_DOUGH(BGOFIBSCookieYouCanOnlyDouble,		"^\\*\\* You can only double before you roll the dice\\.");
  ADD_DOUGH(BGOFIBSCookieWaitUntilMoved,		"^\\*\\* Please wait until user has moved\\.");
  
  
  // Pipcounts
  
  ADD_DOUGH(BGOFIBSCookiePlayerDoesntAllowPip,
            "^\\*\\* [a-zA-Z_<>]+ doesn't allow this. Ask [a-zA-Z_<>]+ to 'toggle allowpip'");
  
  ADD_DOUGH(BGOFIBSCookieTypeAllowPipFirst,
            "^\\*\\* Type 'toggle allowpip' first\\.");
  
  /*
   * CLIP Spec notes:
   * "Note that the allowpip toggle has no effect when watching a game.
   * You can always get a pip count for the two players."
   *
   * But the command ref for pip http://www.fibs.com/CommandReference/pip.html
   * states "This command can also be used while watching a game
   * if both players have set their allowpip-toggle to YES."
   *
   * Indeed we've got the following message once
   */
  
  ADD_DOUGH(BGOFIBSCookiePlayersDontAllowPipCounts,
            "^\\*\\* The players don't allow pipcounts\\.");
  
  
  ADD_DOUGH(BGOFIBSCookieWaitUntilAcceptReject,	"^\\*\\* Wait until [a-zA-Z_<>]+ accepted or rejected your resign\\.");
  ADD_DOUGH(BGOFIBSCookieWantedToResign,		"^\\*\\* [a-zA-Z_<>]+ wanted to resign. Type 'accept' or 'reject'\\.");
  
  ADD_DOUGH(BGOFIBSCookieDoesntWantYouToWatch,	"^\\*\\* [a-zA-Z_<>]+ doesn't want you to watch\\.");

  ADD_DOUGH(
            BGOFIBSCookieNowAllowedToCreateNewGames,
            "\\*\\* You are not allowed to create new games\\."
            );
  
  START_BATCH(LoginBatch, FIBS_LoginPrompt, "^login:");
  ADD_DOUGH(CLIP_WELCOME,					"^1 [a-zA-Z_<>]+ [0-9]+ ");
  ADD_DOUGH(CLIP_OWN_INFO,				"^2 [a-zA-Z_<>]+ [01] [01]");
  ADD_DOUGH(CLIP_MOTD_BEGIN,				"^3"); // MODIFIED
  ADD_DOUGH(BGOFIBSCookieGuestWelcome,			"^Welcome to (FIBS|TiGa). You just logged in as guest.");
  ADD_DOUGH(FIBS_FailedLogin,				"^> [0-9]+");		// bogus CLIP messages sent after a failed login
  
  START_BATCH(BGOFIBSGuestLoginBatch, BGOFIBSCookieGuestPrompt, "^> ");
  ADD_DOUGH(BGOFIBSCookieUserNameInUse,			"^\\*\\* Please use another name");
  ADD_DOUGH(BGOFIBSCookieInvalidUserName,			"^\\*\\* Your name may only contain");
  ADD_DOUGH(BGOFIBSCookieTypeInNoPassword,		"^Type in no password and hit");
  ADD_DOUGH(BGOFIBSCookiePasswordPrompt,			"Please give your password");
  ADD_DOUGH(BGOFIBSCookieInvalidPassword,			"^Minimal password length is");
  ADD_DOUGH(BGOFIBSCookieRetypePasswordPrompt,	"^Please retype your password");
  ADD_DOUGH(BGOFIBSCookiePasswordsNotIdentical,	"^\\*\\* The two passwords were not identical");
  ADD_DOUGH(BGOFIBSCookieUserNameRegistered,		"^You are registered");
  ADD_DOUGH(BGOFIBSCookieWelcomeBack,				"^Welcome back");
  ADD_DOUGH(BGOFIBSCookieNotAway,					"^\\*\\* You're not away");
  ADD_DOUGH(
            BGOFIBSCookieUserNameTooLong,
            "\\*\\* Please use a name with at most 20 characters\\."
            );
  ADD_DOUGH(
            FIBS_Timeout,
            "^Connection timed out\\."
            );
  
  
  
  // Only interested in one message here, but we still use a message list for simplicity and consistency.
  START_BATCH(MOTDBatch, CLIP_MOTD_END, "^4"); // MODIFIED
  
  MessageState = FIBS_LOGIN_STATE;
  
#undef START_BATCH
#undef ADD_DOUGH
  
}

// Allocates memory for a new CookieDough struct, initializes it, and returns pointer.
static CookieDough * AddCookieDough(int message, const char * re)
{
  CookieDough * newDough = malloc(sizeof(CookieDough));
  if (newDough == NULL)
    return NULL;
  
  int result = regcomp(&(newDough->regex), re, REG_EXTENDED | REG_NOSUB);
  if (result)			// we discard the result code, so...
  {
    free(newDough);	// ...set a breakpoint here, if you're having initialization problems.
    return NULL;
  }
  newDough->cookie = message;
  newDough->next = NULL;
  return newDough;
}

// Releases the CookieDough struct passed in the parameter, returning
// that struct's next pointer. Also calls regfree() to release the memory
// allocated by regcomp().
static CookieDough * ReleaseCookieDough(CookieDough * theDough)
{
  if (theDough == NULL)
    return NULL;
  
  CookieDough * nextDough = theDough->next;
  regfree(&(theDough->regex));
  free(theDough);
  return nextDough;
}

#pragma mark --- Test harness ---

#if TEST_FIBSCOOKIEMONSTER

// This test program just prepends the cookie to the front of each message.
//
// The easiest way to run this is  to capture an online session to a text file.
// Remember to log in as a CLIP client, not a plain old telnet user, for example:
//
// % telnet fibs.com 4321 | tee ~/Documents/fibs_log.txt
// ...
// login: login TESTAPP 1008 fergy xxxxxxxxxxxx
// ... (etc.)
//
// and then using that file as input to this program:
//
// % ThisTestApp < ~/Documents/fibs_log.txt
//
// I have found that piping telnet directly to this test program doesn't work
// reliably (at least on OS X), but you may try that:
//
// % telnet fibs.com 4321 |  ThisTestApp
//
// NOTE: The incoming FIBS lines should NOT include a line terminator.

int main(int argc, const char * argv[])
{
  
  int numCookies[500];
  int i;
  
  for (i=0; i < 500; ++i)
    numCookies[i] = 0;
  
  char message[4096];
  
  ResetFIBSCookieMonster();
  
  while (gets(message))
  {
    int cookie = FIBSCookie(message);
    printf("%3d: %s\n", cookie, message);
    numCookies[cookie] += 1;
  }
  
  printf("--------------\n");
  for (i = 0; i < 500; ++i)
    if (numCookies[i] > 0)
      printf("%3d %4d\n", i, numCookies[i]);
  
  ReleaseFIBSCookieMonster();
  
  return 0;
}

#endif

