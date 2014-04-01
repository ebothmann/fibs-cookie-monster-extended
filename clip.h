//
//  clip.h
//  Backgammon Online
//
//  CLIP message IDs
//  Modified by Fery for use with FIBSCookieMonster
//

#pragma once

#define CLIP_VERSION 1009

#define CLIP_WELCOME 1
#define CLIP_OWN_INFO 2
#define CLIP_MOTD_BEGIN 3
#define CLIP_MOTD_END 4
#define CLIP_WHO_INFO 5
#define CLIP_WHO_END 6
#define CLIP_LOGIN 7
#define CLIP_LOGOUT 8
#define CLIP_MESSAGE 9
#define CLIP_MESSAGE_DELIVERED 10
#define CLIP_MESSAGE_SAVED 11
#define CLIP_SAYS 12
#define CLIP_SHOUTS 13
#define CLIP_WHISPERS 14
#define CLIP_KIBITZES 15
#define CLIP_YOU_SAY 16
#define CLIP_YOU_SHOUT 17
#define CLIP_YOU_WHISPER 18
#define CLIP_YOU_KIBITZ 19

/**
 The CLIP_ALERT cookie has been introduced with the clip version 1009.
 
 The CLIP_ALERT message is very similar to CLIP 12, the SAYS message. The
 difference between 12 and 20 is that 20 can only be sent by an administrator or
 administrative bot, and will be used to get a user's attention.
 
 CLIP 20 messages should be handled in such a way that the user cannot easily
 overlook them.  The user should also be able to reply via the tell command.
 
 Clients using CLIP 1008 or earlier, or telnet clients, the alert will come
 across as a regular tell.
 
 The alert functionality can be testedby talking to the "monitor" bot:
 "tell monitor alertme"
 will cause it to send an alert.
 */
#define CLIP_ALERT 20

#define CLIP_LAST_CLIP_ID 20
