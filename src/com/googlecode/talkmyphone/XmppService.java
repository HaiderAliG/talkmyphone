package com.googlecode.talkmyphone;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.talkmyphone.contacts.Contact;
import com.googlecode.talkmyphone.contacts.ContactsManager;
import com.googlecode.talkmyphone.contacts.Phone;
import com.googlecode.talkmyphone.geo.GeoManager;
import com.googlecode.talkmyphone.phone.PhoneManager;
import com.googlecode.talkmyphone.sms.Sms;
import com.googlecode.talkmyphone.sms.SmsMmsManager;

public class XmppService extends Service {
    private BroadcastsAndCommandsHandler mBroadcastsAndCommandsHandler;

    private static final int DISCONNECTED = 0;
    private static final int CONNECTING = 1;
    private static final int CONNECTED = 2;

    // Indicates the current state of the service (disconnected/connecting/connected)
    private int mStatus = DISCONNECTED;

    // Service instance
    private static XmppService instance = null;

    // XMPP connection
    private String mLogin;
    private String mPassword;
    private String mTo;
    private ConnectionConfiguration mConnectionConfiguration = null;
    private XMPPConnection mConnection = null;
    private PacketListener mPacketListener = null;
    private boolean notifyApplicationConnection;
    private static boolean formatChatResponses;

    // last person who sent sms/who we sent an sms to
    private String lastRecipient = null;

    // sms
    private int smsNumber;
    private boolean displaySentSms;

    // notification stuff
    @SuppressWarnings("unchecked")
    private static final Class[] mStartForegroundSignature = new Class[] {
        int.class, Notification.class};
    @SuppressWarnings("unchecked")
    private static final Class[] mStopForegroundSignature = new Class[] {
        boolean.class};
    private NotificationManager mNM;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];
    private PendingIntent contentIntent = null;

    // Our current retry attempt, plus a runnable and handler to implement retry
    private int mCurrentRetryCount = 0;
    Runnable mReconnectRunnable = null;
    Handler mReconnectHandler = new Handler();

    public final static String LOG_TAG = "talkmyphone";
    /** Updates the status about the service state (and the statusbar)*/
    private void updateStatus(int status) {
        if (status != mStatus) {
            Notification notification = new Notification();
            switch(status) {
                case CONNECTED:
                    notification = new Notification(
                            R.drawable.status_green,
                            "Connected",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "TalkMyPhone",
                            "Connected",
                            contentIntent);
                    break;
                case CONNECTING:
                    notification = new Notification(
                            R.drawable.status_orange,
                            "Connecting...",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "TalkMyPhone",
                            "Connecting...",
                            contentIntent);
                    break;
                case DISCONNECTED:
                    notification = new Notification(
                            R.drawable.status_red,
                            "Disconnected",
                            System.currentTimeMillis());
                    notification.setLatestEventInfo(
                            getApplicationContext(),
                            "TalkMyPhone",
                            "Disconnected",
                            contentIntent);
                    break;
                default:
                    break;
            }
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            notification.flags |= Notification.FLAG_NO_CLEAR;
            stopForegroundCompat(mStatus);
            startForegroundCompat(status, notification);
            mStatus = status;
        }
    }
    /**
     * This is a wrapper around the startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke startForeground", e);
            }
            return;
        }
        // Fall back on the old API.
        setForeground(true);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
                Log.w("ApiDemos", "Unable to invoke stopForeground", e);
            }
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        setForeground(false);
    }

    /**
     * This makes the 2 previous wrappers possible
     */
    private void initNotificationStuff() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        try {
            mStartForeground = getClass().getMethod("startForeground",
                    mStartForegroundSignature);
            mStopForeground = getClass().getMethod("stopForeground",
                    mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        contentIntent =
            PendingIntent.getActivity(
                    this, 0, new Intent(this, MainScreen.class), 0);
    }

    /** imports the preferences */
    private void importPreferences() {
        SharedPreferences prefs = getSharedPreferences("TalkMyPhone", 0);
        String serverHost = prefs.getString("serverHost", "");
        int serverPort = prefs.getInt("serverPort", 0);
        String serviceName = prefs.getString("serviceName", "");
        mConnectionConfiguration = new ConnectionConfiguration(serverHost, serverPort, serviceName);
        mTo = prefs.getString("notifiedAddress", "");
        mPassword =  prefs.getString("password", "");
        boolean useDifferentAccount = prefs.getBoolean("useDifferentAccount", false);
        if (useDifferentAccount) {
            mLogin = prefs.getString("login", "");
        } else{
            mLogin = mTo;
        }
        notifyApplicationConnection = prefs.getBoolean("notifyApplicationConnection", true);
        displaySentSms = prefs.getBoolean("showSentSms", false);
        smsNumber = prefs.getInt("smsNumber", 5);
        formatChatResponses = prefs.getBoolean("formatResponses", false);
    }


    /** clears the XMPP connection */
    public void clearConnection() {
        if (mReconnectRunnable != null)
            mReconnectHandler.removeCallbacks(mReconnectRunnable);

        if (mConnection != null) {
            if (mPacketListener != null) {
                mConnection.removePacketListener(mPacketListener);
            }
            // don't try to disconnect if already disconnected
            if (isConnected()) {
                mConnection.disconnect();
            }
        }
        mConnection = null;
        mPacketListener = null;
        mConnectionConfiguration = null;
        updateStatus(DISCONNECTED);
    }

    private void maybeStartReconnect() {
        if (mCurrentRetryCount > 5) {
            // we failed after all the retries - just die.
            Log.v(LOG_TAG, "maybeStartReconnect ran out of retrys");
            updateStatus(DISCONNECTED);
            Toast.makeText(this, "Failed to connect.", Toast.LENGTH_SHORT).show();
            onDestroy();
            return;
        } else {
            mCurrentRetryCount += 1;
            // a simple linear-backoff strategy.
            int timeout = 5000 * mCurrentRetryCount;
            Log.e(LOG_TAG, "maybeStartReconnect scheduling retry in " + timeout);
            mReconnectHandler.postDelayed(mReconnectRunnable, timeout);
        }
    }

    /** init the XMPP connection */
    public void initConnection() {
        updateStatus(CONNECTING);
        NetworkInfo active = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (active==null || !active.isAvailable()) {
            Log.e(LOG_TAG, "connection request, but no network available");
            Toast.makeText(this, "Waiting for network to become available.", Toast.LENGTH_SHORT).show();
            // we don't destroy the service here - our network receiver will notify us when
            // the network comes up and we try again then.
            updateStatus(DISCONNECTED);
            return;
        }
        if (mConnectionConfiguration == null) {
            importPreferences();
        }
        XMPPConnection connection = new XMPPConnection(mConnectionConfiguration);
        try {
            connection.connect();
        } catch (XMPPException e) {
            Log.e(LOG_TAG, "xmpp connection failed: " + e);
            Toast.makeText(this, "Connection failed.", Toast.LENGTH_SHORT).show();
            maybeStartReconnect();
            return;
        }
        try {
            connection.login(mLogin, mPassword);
        } catch (XMPPException e) {
            connection.disconnect();
            Log.e(LOG_TAG, "xmpp login failed: " + e);
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiator is the message itself which starts with this
            // hard-coded string.
            if (e.getMessage().indexOf("SASL authentication")==-1) {
                // doesn't look like a bad username/password, so retry
                Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show();
                maybeStartReconnect();
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                onDestroy();
            }
            return;
        }
        mConnection = connection;
        onConnectionComplete();
    }

    private void onConnectionComplete() {
        Log.v(LOG_TAG, "connection established");
        mCurrentRetryCount = 0;
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        mPacketListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message message = (Message) packet;

                if (    message.getFrom().toLowerCase().startsWith(mTo.toLowerCase() + "/")
                    && !message.getFrom().equals(mConnection.getUser()) // filters self-messages
                ) {
                    if (message.getBody() != null) {
                        onCommandReceived(message.getBody());

                        String commandLine = message.getBody();
                        String command;
                        String args;

                        if (-1 != commandLine.indexOf(":")) {
                            command = commandLine.substring(0, commandLine.indexOf(":"));
                            args = commandLine.substring(commandLine.indexOf(":") + 1);
                        } else {
                            command = commandLine;
                            args = "";
                        }
                        command = command.toLowerCase();
                        Intent intent = new Intent("ACTION_TALKMYPHONE_USER_COMMAND_RECEIVED");
                        intent.putExtra("command", command);
                        intent.putExtra("args", args);
                        sendBroadcast(intent);
                    }
                }
            }
        };
        mConnection.addPacketListener(mPacketListener, filter);
        updateStatus(CONNECTED);
        // Send welcome message
        if (notifyApplicationConnection) {
            send("Welcome to TalkMyPhone. Send \"?\" for getting help");
        }
    }

    /** returns true if the service is correctly connected */
    public boolean isConnected() {
        return    (mConnection != null
                && mConnection.isConnected()
                && mConnection.isAuthenticated());
    }

    private void _onStart() {
        // Get configuration
        if (instance == null)
        {
            instance = this;

            mBroadcastsAndCommandsHandler = new BroadcastsAndCommandsHandler(getApplicationContext());

            initNotificationStuff();

            updateStatus(DISCONNECTED);

            // first, clean everything
            clearConnection();

            // then, re-import preferences
            importPreferences();

            mCurrentRetryCount = 0;
            mReconnectRunnable = new Runnable() {
                public void run() {
                    Log.v(LOG_TAG, "attempting reconnection");
                    Toast.makeText(XmppService.this, "Reconnecting", Toast.LENGTH_SHORT).show();
                    initConnection();
                }
            };
            initConnection();
        }
    }

    public static XmppService getInstance() {
        return instance;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        _onStart();
    };

    @Override
    public void onDestroy() {
        GeoManager.stopLocatingPhone();

        clearConnection();

        stopForegroundCompat(mStatus);

        instance = null;

        Toast.makeText(this, "TalkMyPhone stopped", Toast.LENGTH_SHORT).show();
    }

    /** sends a message to the user */
    public void send(String message) {
        if (isConnected()) {
            Message msg = new Message(mTo, Message.Type.chat);
            msg.setBody(message);
            mConnection.sendPacket(msg);
        }
    }


    public void setLastRecipient(String phoneNumber) {
        if (lastRecipient == null || !phoneNumber.equals(lastRecipient)) {
            lastRecipient = phoneNumber;
            displayLastRecipient(phoneNumber);
        }
    }

    public static String makeBold(String in) {
        if (formatChatResponses) {
            return " *" + in + "* ";
        }
        return in;
    }

    public static String makeItalic(String in) {
        if (formatChatResponses) {
            return " _" + in + "_ ";
        }
        return in;
    }

    /** handles the different commands */
    private void onCommandReceived(String commandLine) {
        try {
            String command;
            String args;
            if (-1 != commandLine.indexOf(":")) {
                command = commandLine.substring(0, commandLine.indexOf(":"));
                args = commandLine.substring(commandLine.indexOf(":") + 1);
            } else {
                command = commandLine;
                args = "";
            }

            // Not case sensitive commands
            command = command.toLowerCase();

            if (command.equals("sms")) {
                int separatorPos = args.indexOf(":");
                String contact = null;
                String message = null;
                if (-1 != separatorPos) {
                    contact = args.substring(0, separatorPos);
                    setLastRecipient(contact);
                    message = args.substring(separatorPos + 1);
                    sendSMS(message, contact);
                } else if (args.length() > 0) {
                    contact = args;
                    readSMS(contact);
                } else {
                    displayLastRecipient(lastRecipient);
                }
            }
            else if (command.equals("reply")) {
                if (lastRecipient == null) {
                    send("Error: no recipient registered.");
                } else {
                    sendSMS(args, lastRecipient);
                }
            }
            else if (command.equals("geo")) {
                geo(args);
            }
            else if (command.equals("dial")) {
                dial(args);
            }
            else if (command.equals("where")) {
                send("Start locating phone");
                GeoManager.startLocatingPhone();
            }
            else if (command.equals("stop")) {
                send("Stopping ongoing actions");
                GeoManager.stopLocatingPhone();
            }
            else if (command.equals("http")) {
                open("http:" + args);
            }
            else if (command.equals("https")) {
                open("https:" + args);
            }
            else {
                send('"'+ commandLine + '"' + ": unknown command. Send \"?\" for getting help");
            }
        } catch (Exception ex) {
            send("Error : " + ex);
        }
    }

    /** dial the specified contact */
    public void dial(String searchedText) {
        String number = null;
        String contact = null;

        if (Phone.isCellPhoneNumber(searchedText)) {
            number = searchedText;
            contact = ContactsManager.getContactName(number);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(searchedText);
            if (mobilePhones.size() > 1) {
                send("Specify more details:");

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                contact = phone.contactName;
                number = phone.cleanNumber;
            } else {
                send("No match for \"" + searchedText + "\"");
            }
        }

        if( number != null) {
            send("Dial " + contact + " (" + number + ")");
            if(!PhoneManager.Dial(number)) {
                send("Error can't dial.");
            }
        }
    }

    /** sends a SMS to the specified contact */
    public void sendSMS(String message, String contact) {
        if (Phone.isCellPhoneNumber(contact)) {
            send("Sending sms to " + ContactsManager.getContactName(contact));
            SmsMmsManager.sendSMSByPhoneNumber(message, contact);
        } else {
            ArrayList<Phone> mobilePhones = ContactsManager.getMobilePhones(contact);
            if (mobilePhones.size() > 1) {
                send("Specify more details:");

                for (Phone phone : mobilePhones) {
                    send(phone.contactName + " - " + phone.cleanNumber);
                }
            } else if (mobilePhones.size() == 1) {
                Phone phone = mobilePhones.get(0);
                send("Sending sms to " + phone.contactName + " (" + phone.cleanNumber + ")");
                SmsMmsManager.sendSMSByPhoneNumber(message, phone.cleanNumber);
            } else {
                send("No match for \"" + contact + "\"");
            }
        }
    }

    /** reads (count) SMS from all contacts matching pattern */
    public void readSMS(String searchedText) {

        ArrayList<Contact> contacts = ContactsManager.getMatchingContacts(searchedText);
        ArrayList<Sms> sentSms = new ArrayList<Sms>();
        if(displaySentSms) {
            sentSms = SmsMmsManager.getAllSentSms();
        }

        if (contacts.size() > 0) {
            StringBuilder noSms = new StringBuilder();
            Boolean hasMatch = false;
            for (Contact contact : contacts) {
                ArrayList<Sms> smsList = SmsMmsManager.getSms(contact.id, contact.name);
                if(displaySentSms) {
                    smsList.addAll(SmsMmsManager.getSentSms(ContactsManager.getPhones(contact.id),sentSms));
                    Collections.sort(smsList);
                }

                smsList.subList(Math.max(smsList.size() - smsNumber,0), smsList.size());
                if (smsList.size() > 0) {
                    hasMatch = true;
                    StringBuilder smsContact = new StringBuilder();
                    smsContact.append(makeBold(contact.name));
                    for (Sms sms : smsList) {
                        smsContact.append("\r\n" + makeItalic(sms.date.toLocaleString() + " - " + sms.sender));
                        smsContact.append("\r\n" + sms.message);
                    }
                    if (smsList.size() < smsNumber) {
                        smsContact.append("\r\n" + makeItalic("Only got " + smsList.size() + " sms"));
                    }
                    send(smsContact.toString() + "\r\n");
                } else {
                    noSms.append(contact.name + " - No sms found\r\n");
                }
            }
            if (!hasMatch) {
                send(noSms.toString());
            }
        } else {
            send("No match for \"" + searchedText + "\"");
        }
    }


    public void displayLastRecipient(String phoneNumber) {
        if (phoneNumber == null) {
            send("Reply contact is not set");
        } else {
            String contact = ContactsManager.getContactName(phoneNumber);
            if (Phone.isCellPhoneNumber(phoneNumber) && contact.compareTo(phoneNumber) != 0){
                contact += " (" + phoneNumber + ")";
            }
            send("Reply contact is now " + contact);
        }
    }

    /** Open geolocalization application */
    private void geo(String text) {
        List<Address> addresses = GeoManager.geoDecode(text);
        if (addresses != null) {
            if (addresses.size() > 1) {
                send("Specify more details:");
                for (Address address : addresses) {
                    StringBuilder addr = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                        addr.append(address.getAddressLine(i) + "\n");
                    }
                    send(addr.toString());
                }
            } else if (addresses.size() == 1) {
                GeoManager.launchExternal(addresses.get(0).getLatitude() + "," + addresses.get(0).getLongitude());
            }
        } else {
            send("No match for \"" + text + "\"");
            // For emulation testing
            // GeoManager.launchExternal("48.833199,2.362232");
        }
    }

    /** lets the user choose an activity compatible with the url */
    private void open(String url) {
        Intent target = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        Intent intent = Intent.createChooser(target, "TalkMyPhone: choose an activity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


}
