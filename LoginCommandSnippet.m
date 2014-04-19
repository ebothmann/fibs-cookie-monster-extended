/*
 * Create command string
 * Format:
 * login client_name clip_version name password
 * client_name: 	A string of a maximum of 20 characters with no whitespace that identifies your client program.
 * clip_version:  Tells FIBS which version of the CLIP interface your client understands. The latest version as of Octobre 2012 is “1009”.
 * name:          Your user ID.
 * password:      Your password.
 */

// The %@ are object placeholders in Objective-C
NSString *commandString = [NSString stringWithFormat:@"login %@_v%@ 1009 %@ %@", client, version, userName, password];
