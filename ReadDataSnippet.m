#import "BGOFIBSCookieMonster.h"

static NSStringEncoding const BGOFIBSStringEncoding = NSASCIIStringEncoding;
static NSString * const EBNewLineCharacter = @"\n";
static NSString * const EBCarriageReturnCharacter = @"\r";

- (void)onSocket:(AsyncSocket *)socket
     didReadData:(NSData *)data
         withTag:(long)tag
{
  // Check the data argument
  NSParameterAssert(data != nil);
  NSParameterAssert([data length] > 0);

  // Get string from stream data
  NSString *message = [[NSString alloc] initWithData:data
                                            encoding:BGOFIBSStringEncoding];

  if (message == nil) {
    // The data does not represent a valid string yet, wait for more
    [socket readDataWithTimeout:-1 tag:0];
    return;
  }

  // Process the string
  NSArray *lines = [message componentsSeparatedByString:EBNewLineCharacter];

  // Process lines
  NSUInteger numberOfLines = [lines count];
  for (NSUInteger i = 0; i < numberOfLines; i++)
  {
    // Obtain a line
    NSString *line = (NSString *)lines[i];

    // Remove carriage returns
    NSCharacterSet *carriageReturnCharacterSet =
    [NSCharacterSet characterSetWithCharactersInString:EBCarriageReturnCharacter];
    line = [line stringByTrimmingCharactersInSet:carriageReturnCharacterSet];

    // Process previously unfinished lines
    if ((i == 0) && self.bufferedUnfinishedLine)
    {
      // Append the unfinished line of the previous data block
      // to the first line of this one
      line = [self.bufferedUnfinishedLine stringByAppendingString:line];

      // Clean up
      self.bufferedUnfinishedLine = nil;
    }

    // Obtain cookie
    int cookie = [self cookieForMessage:line];

    // Remember unfinished lines for completing them later
    if ((i + 1) == numberOfLines)
    {
      // This is the string after the last newLineChar
      // if all lines were complete, it should be empty
      // if it isn't, buffer it for later

      // NOTE: there are exceptions, messages which do NOT
      // end on \n\r, which is fine, examples:
      // login:
      // >

      // Handle exceptions
      if ((cookie != FIBS_LoginPrompt) &&
          (cookie != FIBS_FailedLogin) &&
          (cookie != BGOFIBSCookieGuestPrompt) &&
          (cookie != BGOFIBSCookiePasswordPrompt) &&
          (cookie != BGOFIBSCookieRetypePasswordPrompt) &&
          (cookie != BGOFIBSCookiePasswordsNotIdentical) &&
          (cookie != FIBS_Empty) &&
          ([line length] > 0))
      {
        // Remember unfinished line and return
        self.bufferedUnfinishedLine = line;
        [socket readDataWithTimeout:-1 tag:0];
        return;
      }
    }

    // Handle cookie
    [self handleFIBSCookie:cookie withMessage:line];
  }

  [socket readDataWithTimeout:-1 tag:0];
}

- (int)cookieForMessage:(NSString *)message
{
  // Create C string
  NSUInteger aBufferSize = [message length] + 1;
  char aBuffer[aBufferSize];

  // Get message
  [message getCString:aBuffer
            maxLength:aBufferSize
             encoding:[NSString defaultCStringEncoding]];

  // Return FIBS cookie
  return FIBSCookie(aBuffer);
}

- (void)handleFIBSCookie:(int)cookie withMessage:(NSString *)message
{
  // Process cookie (a very long switch)
  switch (cookie)
  {
    case BGOFIBSCookieGuestPrompt:
      ...
  }
}
