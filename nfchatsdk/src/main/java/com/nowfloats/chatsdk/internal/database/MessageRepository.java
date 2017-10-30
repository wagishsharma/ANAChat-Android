package com.nowfloats.chatsdk.internal.database;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.nowfloats.chatsdk.internal.model.InputTypeAddress;
import com.nowfloats.chatsdk.internal.model.InputTypeDate;
import com.nowfloats.chatsdk.internal.model.InputTypeEmail;
import com.nowfloats.chatsdk.internal.model.InputTypeLocation;
import com.nowfloats.chatsdk.internal.model.InputTypeNumeric;
import com.nowfloats.chatsdk.internal.model.InputTypePhone;
import com.nowfloats.chatsdk.internal.model.InputTypeText;
import com.nowfloats.chatsdk.internal.model.InputTypeTime;
import com.nowfloats.chatsdk.internal.model.Item;
import com.nowfloats.chatsdk.internal.model.Message;
import com.nowfloats.chatsdk.internal.model.MessageCarousel;
import com.nowfloats.chatsdk.internal.model.MessageInput;
import com.nowfloats.chatsdk.internal.model.MessageResponse;
import com.nowfloats.chatsdk.internal.model.MessageSimple;
import com.nowfloats.chatsdk.internal.model.Option;
import com.nowfloats.chatsdk.internal.model.inputdata.InputTypeMedia;
import com.nowfloats.chatsdk.internal.network.ApiCalls;
import com.nowfloats.chatsdk.internal.utils.ListenerManager;
import com.nowfloats.chatsdk.internal.utils.constants.Constants;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by lookup on 06/09/17.
 */

public class MessageRepository {

    private static MessageRepository instance = null;
    private Context context;

    public static MessageRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (MessageRepository.class) {
                instance = new MessageRepository(context);
            }
        }
        return instance;
    }

    private DatabaseHelper mHelper;

    private MessageRepository(Context context) {
        this.context = context;
        if (PreferencesManager.getsInstance(context).getFirstLaunch()) {
            context.deleteDatabase("nfchat.db");
            PreferencesManager.getsInstance(context).setFirstLaunch(false);

        }
        mHelper = OpenHelperManager.getHelper(this.context,
                DatabaseHelper.class);
    }

    public void handleMessageResponse(MessageResponse messageResponse) {
        Log.d(TAG, "handleMessageResponse with API!" + messageResponse.getMessage().getMessageType());
        switch (messageResponse.getMessage().getMessageType()) {
            case Constants.MessageType.CAROUSEL:
                setCarousalContent(messageResponse);
                break;
            case Constants.MessageType.INPUT:
                setInputMessage(messageResponse);
                break;
            case Constants.MessageType.SIMPLE:
                setSimpleMessage(messageResponse);
                break;
            case Constants.MessageType.EXTERNAL:
                setExternalMessage(messageResponse);
                break;
            default:
                break;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        OpenHelperManager.releaseHelper();
    }

    public void setCarousalContent(MessageResponse messageResponse) {
        MessageCarousel messageCarousel = new MessageCarousel();
        try {
            if (!isMessageExist(messageResponse.getMessage().getTimestamp())) {
                if (messageResponse.getData().getContent().getInput() != null)
                    messageCarousel.setInput(messageResponse.getData().getContent().getInput());
                messageCarousel.setMandatory(messageResponse.getData().getContent().getMandatory());
                messageCarousel.setItems(messageResponse.getData().getContent().getItems());
                if (messageResponse.getData().getContent().getItems() != null &&
                        !isMessageExist(messageResponse.getMessage().getTimestamp())) {
                    for (Item item : messageCarousel.getItems()) {
                        item.setOptionsForeignCollection(item.getOptions());
                        for (Option option : item.getOptions()) {
                            item.setMessageCarousel(messageCarousel);
                            option.setItem(item);
                            mHelper.getOptionsDao().create(option);
                        }
                    }
                }
            }
            messageResponse.getMessage().setMessageCarousel(messageCarousel);
            writeMessage(messageResponse);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setSimpleMessage(MessageResponse messageResponse) {
        try {
            MessageSimple messageSimple = new MessageSimple();
            messageSimple.setMandatory(messageResponse.getData().getContent().getMandatory());
            if (messageResponse.getData().getContent().getMedia() != null)
                messageSimple.setMedia(messageResponse.getData().getContent().getMedia());
            if (messageResponse.getData().getContent().getText() != null)
                messageSimple.setText(messageResponse.getData().getContent().getText());
            messageResponse.getMessage().setMessageSimple(messageSimple);
            writeMessage(messageResponse);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void setExternalMessage(MessageResponse messageResponse) {
        try {
            messageResponse.getMessage().setExternalMessage(new
                    Gson().toJson(messageResponse.getData().getContent()));
            writeMessage(messageResponse);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setInputMessage(MessageResponse messageResponse) {
        MessageInput messageInput = new MessageInput();
        messageInput.setInputType(messageResponse.getData().getContent().getInputType());
        messageInput.setMandatory(messageResponse.getData().getContent().getMandatory());
        try {
            switch (messageInput.getInputType()) {
                case Constants.InputType.LOCATION:
                    InputTypeLocation inputTypeLocation
                            = new InputTypeLocation();
                    inputTypeLocation.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeLocation.setDefaultLocation((messageResponse.getData().
                            getContent().getDefaultLocation()));
                    messageInput.setInputTypeLocation(inputTypeLocation);
                    break;
                case Constants.InputType.DATE:
                    InputTypeDate inputTypeDate
                            = new InputTypeDate();
                    inputTypeDate.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeDate.setDateRange((messageResponse.getData().
                            getContent().getDateRange()));
                    messageInput.setInputTypeDate(inputTypeDate);
                    break;
                case Constants.InputType.NUMERIC:
                    InputTypeNumeric inputTypeNumeric
                            = new InputTypeNumeric();
                    inputTypeNumeric.setInput(messageResponse.getData().getContent().getInput());
                    messageInput.setInputTypeNumeric(inputTypeNumeric);
                    break;
                case Constants.InputType.PHONE:
                    InputTypePhone inputTypePhone
                            = new InputTypePhone();
                    inputTypePhone.setInput(messageResponse.getData().getContent().getInput());
                    messageInput.setInputTypePhone(inputTypePhone);
                    break;
                case Constants.InputType.ADDRESS:
                    InputTypeAddress inputTypeAddress
                            = new InputTypeAddress();
                    inputTypeAddress.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeAddress.setRequiredFields(messageResponse.getData().getContent().
                            getRequiredFields().toArray(new String
                            [messageResponse.getData().getContent().getRequiredFields().size()]));
                    messageInput.setInputTypeAddress(inputTypeAddress);
                    break;
                case Constants.InputType.MEDIA:
                    InputTypeMedia inputTypeMedia
                            = new InputTypeMedia();
                    inputTypeMedia.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeMedia.setMediaType(messageResponse.getData().getContent().getMediaType());
                    messageInput.setInputTypeMedia(inputTypeMedia);
                    messageResponse.setFileUpload(true);
                    break;
                case Constants.InputType.TEXT:
                    InputTypeText inputTypeText = new InputTypeText();
                    inputTypeText.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeText.setText(messageResponse.getData().getContent().getText());
                    inputTypeText.setTextInputAttr(messageResponse.getData().
                            getContent().getTextInputAttr());
                    messageInput.setInputTypeText(inputTypeText);
                    break;
                case Constants.InputType.TIME:
                    InputTypeTime inputTypeTime
                            = new InputTypeTime();
                    inputTypeTime.setInput(messageResponse.getData().getContent().getInput());
                    inputTypeTime.setTimeRange((messageResponse.getData().
                            getContent().getTimeRange()));
                    messageInput.setInputTypeTime(inputTypeTime);
                    break;
                case Constants.InputType.EMAIL:
                    InputTypeEmail inputTypeEmail
                            = new InputTypeEmail();
                    inputTypeEmail.setInput(messageResponse.getData().getContent().getInput());
                    messageInput.setInputTypeEmail(inputTypeEmail);
                    break;
                case Constants.InputType.OPTIONS:
                    if (!messageResponse.isOnlyUpdate()) {
                        messageInput.setInputForOptions(messageResponse.
                                getData().getContent().getInput());
                        messageInput.setOptionsForeignCollection(messageResponse.getData()
                                .getContent().getOptions());
                        messageResponse.getMessage().setMessageInput(messageInput);
                        if (!isMessageExist(messageResponse.getMessage().getTimestamp()))
                            for (Option option : messageInput.getOptionsForeignCollection()) {
                                option.setMessageInput(messageInput);
                                mHelper.getOptionsDao().create(option);
                            }
                    }
                    break;
                case Constants.InputType.LIST:
                    return;
            }
            messageResponse.getMessage().setMessageInput(messageInput);
            writeMessage(messageResponse);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param timestamp based on message will be searched
     * @return true if exist
     * @throws SQLException
     */
    private Boolean isMessageExist(long timestamp) throws SQLException {
        QueryBuilder<Message, Integer> builder = mHelper.getMessageDao().queryBuilder();
        builder.where().eq("timestamp", timestamp);
        builder.limit(1L);
        List<Message> messages = mHelper.getMessageDao().query(builder.prepare());
        return messages != null && messages.size() > 0;
    }

    private void writeMessage(MessageResponse messageResponse) throws SQLException {
        if (messageResponse.isOnlyUpdate()) {
            updateMessage(messageResponse.getMessage(), messageResponse.getTimestampToUpdate());
            ListenerManager.getInstance().notifyMessageUpdate(messageResponse.getMessage());
            return;
        }
        Message resultMessage = null;
        try {
            resultMessage = mHelper.getMessageDao()
                    .createIfNotExists(messageResponse.getMessage());
            if (!resultMessage.getSyncWithServer()) ApiCalls.sendMessage(context, messageResponse);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ListenerManager.getInstance().notifyNewMessage(resultMessage);
    }

    public List<Message> getMessages() throws SQLException, IOException {
        QueryBuilder<Message, Integer> builder = mHelper.getMessageDao().queryBuilder();
        builder.orderBy("timestamp", false);  // true for ascending, false for descending
        builder.limit(50L);
        return mHelper.getMessageDao().query(builder.prepare());  // returns list of 50 items
    }

    public List<Message> getLastMessage() throws SQLException, IOException {
        QueryBuilder<Message, Integer> builder = mHelper.getMessageDao().queryBuilder();
        builder.limit(1L);
        builder.orderBy("timestamp", false);  // true for ascending, false for descending
        return mHelper.getMessageDao().query(builder.prepare());  // returns list of 50 items
    }

    public List<Message> getUnSentMessages() throws SQLException, IOException {
        QueryBuilder<Message, Integer> builder = mHelper.getMessageDao().queryBuilder();
        builder.where().eq("sync_with_server", false);
        builder.orderBy("timestamp", true);
        return mHelper.getMessageDao().query(builder.prepare());
    }

    private int updateMessage(Message message, long time) throws SQLException {
        UpdateBuilder<Message, Integer> updateBuilder
                = mHelper.getMessageDao().updateBuilder();
        updateBuilder.where().eq("timestamp", time);
        updateBuilder.updateColumnValue("sync_with_server", message.getSyncWithServer());
        updateBuilder.updateColumnValue("messageId", message.getMessageId());
        updateBuilder.updateColumnValue("timestamp", message.getTimestamp());
        return updateBuilder.update();
    }

    public int updateCarouselMessage(MessageCarousel message) throws SQLException {
        UpdateBuilder<MessageCarousel, Integer> updateBuilder
                = mHelper.getMessageCarouselDao().updateBuilder();
        updateBuilder.where().eq("id", message.getId());
        updateBuilder.updateColumnValue("is_enable",
                message.getEnabled());
        return updateBuilder.update();
    }

}