  * **Is TalkMyPhone secure?**
TalkMyPhone is given to you without any warrantly. However, the smack library
used for connecting to jabber automatically uses the most secure way to
connect. With jabber.org and gmail, you should be ok. Basic check is also done
on the name of the recipient: TalkMyPhone can only receive orders from the
account it sends messages to.

  * **If I have 2 android phones and both of them are set up to connect and notify the same account, does it bug?**
Yes. And I don't plan to fix that. Use different accounts for your phones.

  * **Will there be a way to set up TalkMyPhone to notify more that one recipient?**
No, this is not planned. Though it could be usefull, I personally don't need
it. If you want to write it, there are requirements: You have to modify the
options so that fine tuning will be possible for every accounts (notifications
they receive, commands they can send).

  * **I added a feature to TalkMyPhone, but it's not supported by phones running android<1.5, can you integrate my patch?**
No. The point of TalkMyPhone is that it brings functionnalities to every
phones. If you really want to do that, then fork the project, I'm perfectly
cool with that.

  * **Why does TalkMyPhone needs my gmail identifiers? Can't it guess them?**
Your address could be guessed, but your password can't. Whereas this is a perfectly sane measure taken by google, gtalk remains one of the google services where your password is needed for connecting (e.g. Oauth is technically impossible with gtalk). That means if you want TalkMyPhone to connect **from** your gmail account, you have to enter your google password. Sorry.