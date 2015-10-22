## Announcement ##

TalkMyPhone is moving to [GtalkSMS](https://code.google.com/p/gtalksms/) (see [the mailing list](https://groups.google.com/group/talkmyphone-users/browse_thread/thread/c52bb1514c0fcf3c))

## Description ##

This android application aims at enabling you to control your phone through gtalk. It can be useful for those who prefer typing sms on a real keyboard.

&lt;wiki:gadget url="http://www.ohloh.net/p/485105/widgets/project\_thin\_badge.xml" height="36" border="0"/&gt;

## Features ##

This application opens a gtalk conversation with you and:
  * forwards you the incoming sms
  * notifies you about incoming calls
  * notifies you about battery state

With simple commands, you get the ability to
  * reply to the incoming sms (using "reply:`<message>`")
  * send sms (using "sms:`<contact>`:`<message>`" - contact can be a name or a phone number)
  * read last 5 sms from a contact (using "sms:`<contact>`" with no argument)

Additionnal useful commands are provided, and you can:
  * make you phone ring in case you loose it (using "ring")
  * geolocalize your phone - it will send you google maps links (using "where")
  * copy text to the keyboard (using "copy:`<text>`")
  * get information about a contact (using "contact:`<contact>`")
  * open any url (just paste it in the conversation)
  * get help, using '?'

The changelog is [here](http://talkmyphone.googlecode.com/hg/Changelog). Use the [bug-tracker](https://code.google.com/p/talkmyphone/issues/list) for feature requests/bugs. **Use [the mailing list](http://groups.google.com/group/talkmyphone-users) for asking for help**.

## Setup ##

### Method 1 : For the lazy, using your existing gmail address ###

  * **Address to notify**: enter you gmail address
  * **Use a different account**: leave unchecked
  * **Password**: enter your gmail password

TalkMyphone will connect to Jabber using your gmail account and it will appear as if you're receiving messages **from yourself**. This method has a drawback: since gtalk doesn't let you have conversations with yourself, you will have to **do it from a chat client** like [Pidgin](http://pidgin.im/) (just add yourself to your contacts).

### Method 2 : Using a jabber account specially created for your phone ###

  * Go to https://register.jabber.org/ and create an account.
  * Using a chat client , set the jabber account to be friend with your gtalk account and check that the accounts can talk to each other **in a two-way conversation** (tip #1: There are [web-based clients](http://jwchat.org/), use them at your own risk - tip #2: with Pidgin, you can modify the jabber nickname so that it will appear in a nice way to your gmail account).
  * Then, you can configure the phone:
    * **Address to notify** enter you gmail address
    * **Use a different account** check the option
    * **Login** enter the jabber login
    * **Password** enter the jabber password
    * In **Additional settings**:
      * **Server host**: jabber.org
      * **Server port**: 5222
      * **Service name**: jabber.org

TalkMyphone will connect to Jabber using its own account and you will be able to reach it from the web-based gtalk like a regular contact.

## Download on the market ##

![http://talkmyphone.googlecode.com/files/talkmyphone_market_link.png](http://talkmyphone.googlecode.com/files/talkmyphone_market_link.png)

## Author ##

TalkMyPhone was initially written by [Christophe-Marie Duquesne](http://www.google.com/profiles/chm.duquesne) (joined by [other developers](http://talkmyphone.googlecode.com/hg/AUTHORS)).

## Donate ##

If you like this app, you can thank with a donation. Please specify the way you want to appear in the [donors list](http://talkmyphone.googlecode.com/hg/Donors). Market users will appear as "anonymous donor".

[![](https://www.paypal.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=X6JDMQ6VWCH8J&lc=FR&item_name=TalkMyPhone&item_number=talkmyphone&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted)