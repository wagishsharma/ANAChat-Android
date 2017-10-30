package com.nowfloats.chatsdk.internal;

import android.content.Context;

import com.nowfloats.chatsdk.MessageListener;
import com.nowfloats.chatsdk.NfChatSDKConfig;
import com.nowfloats.chatsdk.internal.database.MessageRepository;
import com.nowfloats.chatsdk.internal.model.Message;
import com.nowfloats.chatsdk.internal.model.MessageResponse;
import com.nowfloats.chatsdk.internal.network.ApiCalls;
import com.nowfloats.chatsdk.internal.utils.ConnectionDetector;
import com.nowfloats.chatsdk.internal.utils.ListenerManager;
import com.nowfloats.chatsdk.internal.utils.NFChatSDK;
import com.nowfloats.chatsdk.internal.utils.NFChatUtils;
import com.nowfloats.chatsdk.internal.utils.concurrent.ApiExecutor;
import com.nowfloats.chatsdk.internal.utils.concurrent.ApiExecutorFactory;
import com.nowfloats.chatsdk.internal.utils.constants.Constants;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MessengerCoreMethods {
    private static final String TAG = "MessengerCoreMethods";
    private Context mContext;
    private NfChatSDKConfig NfChatSDKConfig;
    private MessageListener mListener;
    private ConnectionDetector cd;

    public MessengerCoreMethods(NfChatSDKConfig NfChatSDKConfig, MessageListener
            listener) throws IllegalArgumentException {

        this.NfChatSDKConfig = NfChatSDKConfig;
        this.mListener = listener;
        this.mContext = this.NfChatSDKConfig.getContext();
        cd = new ConnectionDetector(mContext);
        if (this.mListener == null) {
            throw new IllegalArgumentException("Messenger listener cannot be null.");
        }
        NFChatSDK.instance().initialize(mContext);
        ListenerManager.getInstance().addMessageChangeListener(mListener);
        getAllMessages();
        syncMessages();
    }

    private void getAllMessages() {
        ApiExecutor apiExecutor = ApiExecutorFactory.getHandlerExecutor();
        apiExecutor.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Message> messages
                            = MessageRepository.getInstance(mContext).getMessages();
                    ApiExecutor executor = ApiExecutorFactory.getHandlerExecutor();
                    executor.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mListener.onConversationUpdate(messages);
                        }
                    });
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleMessagePush(Message message) {
        MessageResponse messageResponse = null;
        if (message.getMessageType() == Constants.MessageType.INPUT) {
            messageResponse
                    = new MessageResponse.
                    MessageResponseBuilder(mContext).buildFromMessage(message).build();
        } else if (message.getMessageType() == Constants.MessageType.CAROUSEL) {
            messageResponse = new MessageResponse.
                    MessageResponseBuilder(mContext).buildCarousel(message).build();
        }
        if (messageResponse != null)
            ApiCalls.sendMessage(mContext.getApplicationContext(), messageResponse);
    }

    public void syncMessages() {
        if (!NFChatUtils.isNetworkConnected(mContext)) return;
        ApiExecutor apiExecutor = ApiExecutorFactory.getHandlerExecutor();
        apiExecutor.runSync(new Runnable() {
            @Override
            public void run() {
                try {
                    final List<Message> messages
                            = MessageRepository.getInstance(mContext).getUnSentMessages();
                    for (final Message message : messages) {
                        handleMessagePush(message);
                    }
                } catch (SQLException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}