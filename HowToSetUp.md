# How does this work? #

To notify you for events happening on your phone, TalkMyPhone **connects** to
jabber and talks to your **gtalk user**.

## Connecting TalkMyPhone ##

To connect to jabber, TalkMyPhone needs **identifiers**. That means, a login and a password. This can be the identifiers for any jabber account, including your gmail account. You can either connect from your gmail account, or from any jabber compatible address.

## Notified account ##

Once TalkMyPhone is connected to jabber, it has to know which jabber user
it should send notifications to and receive commands from. Most people
will want to notify their gmail account, but you can actually choose to
notify any jabber account.

# The possibilities #

So, TalkMyPhone **connects** from an account and **notifies** another account.
These accounts can be the same, which leads to 2 typical ways to configure
the application.

## Using your gmail address for controls and notifications ##

If you choose this method, the phone will connect to jabber using your
gmail address. Thus, when you will receive a message from the phone, it
will appear as a message sent by... you. While this can be confusing, this
has been chosen as the default method since most users don't like having
to create a different jabber address for there device. To control the
phone, you'll just have to respond to the notifications sent by
TalkMyPhone in the same chat window.

There is an issue coming from this method: gmail does not allow opening a
conversation with yourself, so in the end, you'll be more or less obliged
to use a chat program. Pidgin is the recommanded one. Just add yourself
in your own contacts and talk to yourself to command the phone.

## Using an account for connecting and another one for receiving notifications ##

The other way to go is to use a different address for the phone. If you
already own another gmail address, you can use this one. Otherwise, you
should prefer creating an account from jabber.org since the registration
process is particularly **not** painful (fill a captcha, choose a login and
a password and you can use the account right away - no mail address to
provide nor agreement to sign).

If you use a jabber account, you'll need to modify the advanced settings
and modify the parameters for jabber. These parameters are easy to find on
the internet, but just in case you don't feel like searching:
  * server name: jabber.org
  * server port: 5222
  * service name: jabber.org
If you messed with these parameters and you want to go back to connecting
from a gmail account:
  * server name: talk.google.com
  * server port: 5222
  * service name: gmail.com

## Common mistakes, settings advices ##

  * Verify the extension, that is, double check the "@domain.tld". It is **@gmail.com** or **@jabber.org**. It is very common to make a mistake on this. Verify the domain **and** the extension: Don't put **.org** instead of **.com**.
  * Login/Logout gtalk won't help. The only thing that can help is start/stop the application when you have modified the options.
  * If you think your problem is hardware related, then you should check the HardwareIssues wiki page