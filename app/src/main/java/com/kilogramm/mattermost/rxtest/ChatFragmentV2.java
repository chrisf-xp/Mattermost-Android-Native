package com.kilogramm.mattermost.rxtest;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.kilogramm.mattermost.MattermostApp;
import com.kilogramm.mattermost.MattermostPreference;
import com.kilogramm.mattermost.R;
import com.kilogramm.mattermost.adapters.AdapterPost;
import com.kilogramm.mattermost.adapters.AttachedFilesAdapter;
import com.kilogramm.mattermost.adapters.command.CommandAdapter;
import com.kilogramm.mattermost.databinding.EditDialogLayoutBinding;
import com.kilogramm.mattermost.databinding.FragmentChatMvpBinding;
import com.kilogramm.mattermost.model.entity.CommandObject;
import com.kilogramm.mattermost.model.entity.Data;
import com.kilogramm.mattermost.model.entity.channel.Channel;
import com.kilogramm.mattermost.model.entity.channel.ChannelRepository;
import com.kilogramm.mattermost.model.entity.filetoattacth.FileToAttach;
import com.kilogramm.mattermost.model.entity.filetoattacth.FileToAttachRepository;
import com.kilogramm.mattermost.model.entity.post.Post;
import com.kilogramm.mattermost.model.entity.post.PostByChannelId;
import com.kilogramm.mattermost.model.entity.post.PostByIdSpecification;
import com.kilogramm.mattermost.model.entity.post.PostEdit;
import com.kilogramm.mattermost.model.entity.post.PostRepository;
import com.kilogramm.mattermost.model.entity.team.Team;
import com.kilogramm.mattermost.model.entity.user.User;
import com.kilogramm.mattermost.model.entity.user.UserRepository;
import com.kilogramm.mattermost.model.fromnet.AutocompleteUsers;
import com.kilogramm.mattermost.model.fromnet.CommandToNet;
import com.kilogramm.mattermost.model.websocket.WebSocketObj;
import com.kilogramm.mattermost.rxtest.autocomplete_list.adapter.UsersDropDownListAdapterV2;
import com.kilogramm.mattermost.service.MattermostService;
import com.kilogramm.mattermost.ui.AttachedFilesLayout;
import com.kilogramm.mattermost.view.BaseActivity;
import com.kilogramm.mattermost.view.channel.AddMembersActivity;
import com.kilogramm.mattermost.view.channel.ChannelActivity;
import com.kilogramm.mattermost.view.chat.OnItemAddedListener;
import com.kilogramm.mattermost.view.chat.OnItemClickListener;
import com.kilogramm.mattermost.view.chat.PostViewHolder;
import com.kilogramm.mattermost.view.fragments.BaseFragment;
import com.kilogramm.mattermost.view.search.SearchMessageActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import icepick.State;
import io.realm.Realm;
import io.realm.RealmResults;
import nucleus.factory.RequiresPresenter;

import static android.view.View.GONE;
import static com.kilogramm.mattermost.view.channel.ChannelActivity.REQUEST_ID;

/**
 * Created by Evgeny on 21.01.2017.
 */
@RequiresPresenter(ChatPresenterV2.class)
public class ChatFragmentV2 extends BaseFragment<ChatPresenterV2> implements OnMoreLoadListener,
        OnItemClickListener<String>, AttachedFilesAdapter.EmptyListListener,
        AttachedFilesLayout.AllUploadedListener, OnItemAddedListener {

    private static final String TAG = "ChatFragmentV2";

    private static final Integer TYPING_DURATION = 5000;

    public static final String START_NORMAL = "start_normal";
    public static final String START_SEARCH = "start_search";
    private static final String START_CODE = "start_code";

    private static final String CHANNEL_ID = "CHANNEL_ID";
    private static final String CHANNEL_NAME = "channel_name";
    private static final String CHANNEL_TYPE = "channel_type";
    private static final String SEARCH_MESSAGE_ID = "search_message_id";

    private static final String REPLY_MESSAGE = "reply_message";
    private static final String EDIT_MESSAGE = "edit_message";

    public static final int SEARCH_CODE = 0;
    private static final int PICK_IMAGE = 1;
    private static final int CAMERA_PIC_REQUEST = 2;
    private static final int FILE_CODE = 3;
    private static final int PICKFILE_REQUEST_CODE = 5;

    private static final int PERMISSION_REQUEST_CODE_WRITE_STORAGE = 6;
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 7;
    private static final int PERMISSION_REQUEST_CODE_GALLERY = 8;
    private static final int PERMISSION_REQUEST_CODE_FILES = 9;

    public static final int ADD_MEMBER_CODE= 10;

    private FragmentChatMvpBinding mBinding;

    public static boolean active = false;

    boolean isSendTyping;

    @State
    String mChannelId;
    @State
    String mChannelName;
    @State
    String mChannelType;
    @State
    String mStartCode;
    @State
    String mSearchMessageId = null;
    @State
    String mTeamId;
    @State
    boolean isFocus = false;
    @State
    int positionItemMessage;
    @State
    boolean isOpenedKeyboard = false;

    @State
    StateFragment mState = StateFragment.STATE_DEFAULT;

    private Uri fileFromCamera;

    private BroadcastReceiver brReceiverTyping;

    private AdapterPost adapter;
    private UsersDropDownListAdapterV2 dropDownListAdapter;
    private CommandAdapter commandAdapter;

    Map<String, String> mapType;

    private Post rootPost;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mChannelId = getArguments().getString(CHANNEL_ID);
        this.mChannelName = getArguments().getString(CHANNEL_NAME);
        this.mChannelType = getArguments().getString(CHANNEL_TYPE);
        this.mStartCode = getArguments().getString(START_CODE);
        this.mTeamId = MattermostPreference.getInstance().getTeamId();
        if (mStartCode.equals(START_SEARCH)) {
            this.mSearchMessageId = getArguments().getString(SEARCH_MESSAGE_ID);
        }
        getPresenter().initPresenter(this.mTeamId, this.mChannelId, this.mChannelType);

        //FIXME click on item notification, not set last channel
        MattermostPreference.getInstance().setLastChannelId(mChannelId);

        checkNeededPermissions();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_mvp, container, false);
        View view = mBinding.getRoot();
        initView();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onPause() {
        super.onPause();
        active = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        active = true;
        setupToolbar("", mChannelName, v -> {
                        if (ChatUtils.isDirectChannel(mChannelType)) {
                String userId = ChatUtils.getDirectUserId(mChannelId);
                if (userId != null) {
                    ProfileRxActivity.start(getActivity(), userId);
                } else {
                    Toast.makeText(getActivity(), "Error load user_id", Toast.LENGTH_SHORT).show();
                }
            } else {
                startChannelActivity();
            }
        }, v -> searchMessage());
        NotificationManager notificationManager = (NotificationManager)
                getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mChannelId.hashCode());
        Log.d(TAG, "onResume: channeld" + mChannelId.hashCode());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (brReceiverTyping != null) {
            getActivity().unregisterReceiver(brReceiverTyping);
        }
    }

    @Override
    public void onTopLoadMore() {
        Log.d(TAG, "onTopLoadMore()");
        getPresenter().requestLoadBefore();
    }

    @Override
    public void onBotLoadMore() {
        Log.d(TAG, "onBotLoadMore()");
        getPresenter().requestLoadAfter();
    }

    @Override
    public void OnItemClick(View view, String item) {
        if (PostRepository.query(new PostByIdSpecification(item)).size() != 0) {
            Post post = new Post(PostRepository.query(new PostByIdSpecification(item)).first());
            switch (view.getId()) {
                case R.id.sendStatusError:
                    showErrorSendMenu(view, post);
                    break;
                case R.id.controlMenu:
                    showPopupMenu(view, post);
                    break;
                case R.id.avatar:
                    ProfileRxActivity.start(getActivity(), post.getUserId());
                    break;
            }
        }
    }

    @Override
    public void onItemAdded() {
        mBinding.rev.smoothScrollToPosition(mBinding.rev.getAdapter().getItemCount() - 1);
    }

    @Override
    public void onEmptyList() {
        hideAttachedFilesLayout();
        if (mBinding.writingMessage.getText().toString().trim().length() > 0) {
            mBinding.btnSend.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else {
            mBinding.btnSend.setTextColor(getResources().getColor(R.color.grey));
        }
    }

    @Override
    public void onAllUploaded() {
        mBinding.btnSend.setTextColor(getResources().getColor(R.color.colorPrimary));
    }

    @Override
    protected void setupTypingText(String text) {
        super.setupTypingText(text);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ArrayList<Uri> pickedFiles = new ArrayList<>();
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_PIC_REQUEST) {
                if (fileFromCamera != null) {
                    pickedFiles.add(fileFromCamera);
                } else {
                    Toast.makeText(getActivity(),
                            getString(R.string.cannot_attach_photo),
                            Toast.LENGTH_SHORT)
                            .show();
                }
            } else if ((requestCode == PICKFILE_REQUEST_CODE || requestCode == PICK_IMAGE)) {
                if (data != null) {
                    if (data.getData() != null) {
                        Uri uri = data.getData();
                        List<Uri> uriList = new ArrayList<>();
                        uriList.add(uri);
                        attachFiles(uriList);
                    } else if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            List<Uri> uriList = new ArrayList<>();
                            uriList.add(clipData.getItemAt(i).getUri());
                            attachFiles(uriList);
                        }
                    }
                }
            } else if (requestCode == ADD_MEMBER_CODE || requestCode == REQUEST_ID){
                initView();
                showList();
            }
        }
        if (pickedFiles.size() > 0) {
            attachFiles(pickedFiles);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_GALLERY:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                }
                break;
            case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent();
                }
                break;
            case PERMISSION_REQUEST_CODE_FILES:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                }
                break;
        }
    }


    private void initView() {
        initListChat();
        initBtnSendOnClickListener();
        initRefreshListener();
        initButtonAddFileOnClickListener();
        initDropDownUserList();
        initCommandList();
        initAttachedFilesLayout();
        initBroadcastReceiver();
        initFab();
        initWritingFieldFocusListener();

        getPresenter().startLoadInfoChannel();

    }

    private void initAttachedFilesLayout() {
        RealmResults<FileToAttach> fileToAttachRealmResults = FileToAttachRepository.getInstance().getFilesForAttach();
        if (fileToAttachRealmResults != null && fileToAttachRealmResults.size() > 0) {
            mBinding.attachedFilesLayout.setVisibility(View.VISIBLE);
        } else {
            mBinding.attachedFilesLayout.setVisibility(GONE);
        }
        mBinding.attachedFilesLayout.setEmptyListListener(this);
        mBinding.attachedFilesLayout.setmAllUploadedListener(this);
    }

    private void initListChat() {
        RealmResults<Post> results = PostRepository.query(new PostByChannelId(mChannelId));
        results.addChangeListener(element -> {
            Log.d(TAG, "initListChat() change listener called");
            if (adapter != null) {
                if (results.size() - 2 == ((LinearLayoutManager) mBinding.rev.getLayoutManager()).findLastCompletelyVisibleItemPosition()) {
                    onItemAdded();
                }
            }
        });
        adapter = new AdapterPost(getActivity(), results, this);
        mBinding.rev.setAdapter(adapter);
        mBinding.rev.setListener(this);
    }

    public void initBtnSendOnClickListener() {
        mBinding.btnSend.setOnClickListener(view -> {
            adapter.updatePostById("");
            if (!mBinding.btnSend.getText().equals("Save"))
                if (mBinding.writingMessage.getText().toString().startsWith("/")) {
                    sendCommand();
                } else {
                    sendMessage();
                }
            else
                editMessage();
        });
    }

    public void setChannelName(String channelName) {
        this.mChannelName = channelName;
    }

    private void initRefreshListener() {
        mBinding.rev.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int bottomRow = (recyclerView == null || recyclerView.getChildCount() == 0)
                        ? 0
                        : recyclerView.getAdapter().getItemCount() - 1;
                int lastVisiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findLastVisibleItemPosition();
                if (bottomRow == lastVisiblePosition) {
                    mBinding.swipeRefreshLayout
                            .setEnabled(true);
                    mBinding.fab.hide();
                } else {
                    mBinding.fab.show();
                    mBinding.swipeRefreshLayout
                            .setEnabled(false);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == 1)
                    BaseActivity.hideKeyboard(getActivity());
            }
        });

        mBinding.swipeRefreshLayout.setOnRefreshListener(direction -> {
            getPresenter().startRequestLoadNormal();
        });
    }

    public void initButtonAddFileOnClickListener() {
        mBinding.buttonAttachFile.setOnClickListener(view -> showDialog());
    }

    private void initDropDownUserList() {
        dropDownListAdapter = new UsersDropDownListAdapterV2(null, getActivity(), name -> {
            addUserLinkMessage(name);
           // mBinding.cardViewDropDown.setVisibility(View.GONE);
        });
        mBinding.idRecUser.setAdapter(dropDownListAdapter);
        mBinding.idRecUser.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.writingMessage.addTextChangedListener(getMassageTextWatcher());
        setListenerToRootView();
        mBinding.writingMessage.setOnClickListener(view ->
                getUserList(((EditText) view).getText().toString()));
    }

    private void initCommandList() {
        commandAdapter = new CommandAdapter(getActivity(), getCommandList(null), command -> {
            Toast.makeText(getActivity(), command.getCommand(), Toast.LENGTH_SHORT).show();
            mBinding.writingMessage.setText(command.getCommand() + " ");
            mBinding.writingMessage.setSelection(mBinding.writingMessage.getText().length());
        });
        mBinding.commandLayout.setAdapter(commandAdapter);
        mBinding.commandLayout.setLayoutManager(new LinearLayoutManager(getActivity()));
        mBinding.writingMessage.addTextChangedListener(getCommandListener());
    }

    private void initBroadcastReceiver() {
        brReceiverTyping = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WebSocketObj obj = intent.getParcelableExtra(MattermostService.BROADCAST_MESSAGE);
                Log.d(TAG, obj.getEvent());
                if (obj.getBroadcast() != null
                        && obj.getBroadcast().getChannel_id().equals(mChannelId)) {
                    if (obj.getEvent().equals(WebSocketObj.EVENT_POSTED)
                            && obj.getUserId() != null
                            && !obj.getUserId().equals(MattermostPreference.getInstance().getMyUserId())) {
                        if (mapType != null)
                            mapType.remove(obj.getUserId());
                        getActivity().runOnUiThread(() -> showTyping(null));
                    } else if (obj.getEvent().equals(WebSocketObj.EVENT_TYPING)) {
                        getActivity().runOnUiThread(() -> showTyping(obj));
                    }else if(obj.getEvent().equals(WebSocketObj.EVENT_POST_EDITED)){
                        if(!TextUtils.isEmpty(obj.getPostId())){
                            getActivity().runOnUiThread(() -> {
                                if(adapter!=null){
                                    adapter.updatePostById(obj.getPostId());
                                }
                            });
                        }
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(WebSocketObj.EVENT_TYPING);
        intentFilter.addAction(WebSocketObj.EVENT_POSTED);
        intentFilter.addAction(WebSocketObj.EVENT_POST_EDITED);
        getActivity().registerReceiver(brReceiverTyping, intentFilter);
    }

    private void initFab() {
        mBinding.fab.hide();
        mBinding.fab.setOnClickListener(v -> {
            mBinding.rev.scrollToPosition(adapter.getItemCount() - 1);
        });
    }

    private void initWritingFieldFocusListener() {
        mBinding.writingMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (v == mBinding.writingMessage && !hasFocus) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    private TextWatcher getCommandListener() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                commandAdapter.updateDate(getCommandList(s.toString()));
                if (commandAdapter.getItemCount() == 0)
                    mBinding.cardViewCommandCardView.setVisibility(View.INVISIBLE);
                else
                    mBinding.cardViewCommandCardView.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

    private List<CommandObject> getCommandList(String commandWrite) {
        if (commandWrite != null && !commandWrite.equals("")) {
            return Stream.of(CommandObject.getCommandList())
                    .filter(value -> value.getCommand().startsWith(commandWrite))
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public TextWatcher getMassageTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (charSequence.toString().trim().length() > 0 ||
                        (FileToAttachRepository.getInstance().haveFilesToAttach() &&
                                !FileToAttachRepository.getInstance().haveUnloadedFiles())) {
                    mBinding.btnSend.setTextColor(getResources().getColor(R.color.colorPrimary));
                    if (!isSendTyping) {
                        isSendTyping = true;
                        MattermostService.Helper.create(getActivity()).sendUserTyping(mChannelId);
                        mBinding.getRoot().postDelayed(() -> isSendTyping = false, TYPING_DURATION);
                    }
                } else {
                    mBinding.btnSend.setTextColor(getResources().getColor(R.color.grey));
                }
                getUserList(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    private void getUserList(String text) {
        Log.d(TAG, "getUserList: true");
        int cursorPos = mBinding.writingMessage.getSelectionStart();
        if (cursorPos > 0 && text.contains("@")) {
            if (text.charAt(cursorPos - 1) == '@') {
                getPresenter().requestGetUsers("", cursorPos);
            } else {
                getPresenter().requestGetUsers(text, cursorPos);
            }
        } else {
            Log.d(TAG, "getUserList: false");
            setDropDownUser(null);
        }
    }

    public void setDropDownUser(AutocompleteUsers autocompleteUsers) {
        if (mBinding.writingMessage.getText().length() > 0) {
            dropDownListAdapter.updateData(autocompleteUsers);
        } else {
            dropDownListAdapter.updateData(null);
        }
        if (dropDownListAdapter.getItemCount() == 0)
            mBinding.cardViewDropDown.setVisibility(View.INVISIBLE);
        else
            mBinding.cardViewDropDown.setVisibility(View.VISIBLE);
    }


    public void setListenerToRootView() {
        final View activityRootView = getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
            if (heightDiff > 100) {
                isOpenedKeyboard = true;
            } else if (isOpenedKeyboard) {
                isOpenedKeyboard = false;
            }
        });
    }

    public void addUserLinkMessage(String s) {
        StringBuffer nameBufferStart = new StringBuffer(mBinding.writingMessage.getText().toString());
        int cursorPos = mBinding.writingMessage.getSelectionStart();

        if (cursorPos != 0 && cursorPos == nameBufferStart.length()
                && nameBufferStart.charAt(cursorPos - 1) == '@') {
            mBinding.writingMessage.append(String.format("%s ", s));
            mBinding.writingMessage.setSelection(mBinding.writingMessage.getText().length());
            return;
        }
        if (cursorPos < nameBufferStart.length())
            nameBufferStart.delete(cursorPos, nameBufferStart.length());
        if (nameBufferStart.charAt(cursorPos - 1) == '@') {
            nameBufferStart.append(String.format("%s ", s));
        } else {
            String[] username = nameBufferStart.toString().split("@");
            nameBufferStart = new StringBuffer();
            int count = 1;
            if (username.length == 0) {
                nameBufferStart.append(String.format("@%s ", s));
            }
            for (String element : username) {
                if (count == username.length)
                    nameBufferStart.append(String.format("%s ", s));
                else
                    nameBufferStart.append(String.format("%s@", element));
                count++;
            }
        }
        StringBuffer nameBufferEnd = new StringBuffer(mBinding.writingMessage.getText());
        if (cursorPos < nameBufferStart.length())
            nameBufferEnd.delete(0, cursorPos);

        mBinding.writingMessage.setText(nameBufferStart.toString() + nameBufferEnd.toString());
        mBinding.writingMessage.setSelection(nameBufferStart.length());
    }

    private void attachFiles(List<Uri> uriList) {
        Log.d(TAG, "try to attach file");
        mBinding.attachedFilesLayout.setVisibility(View.VISIBLE);
        mBinding.attachedFilesLayout.addItems(uriList, mChannelId);
        mBinding.btnSend.setTextColor(getResources().getColor(R.color.grey));
    }

    private void searchMessage() {
        SearchMessageActivity.startForResult(getActivity(), mTeamId, SEARCH_CODE);
    }

    public static ChatFragmentV2 createFragment(String startCode, String channelId,
                                                String channelName, String channelType,
                                                String searchId) {
        Log.d(TAG, "createFragment() called with: startCode = [" + startCode + "], channelId = [" + channelId + "], channelName = [" + channelName + "], channelType = [" + channelType + "], searchId = [" + searchId + "]");
        ChatFragmentV2 chatFragment = new ChatFragmentV2();
        Bundle bundle = new Bundle();
        bundle.putString(CHANNEL_ID, channelId);
        bundle.putString(CHANNEL_NAME, channelName);
        bundle.putString(CHANNEL_TYPE, channelType);
        bundle.putString(START_CODE, startCode);
        if (startCode.equals(START_SEARCH) && searchId != null && !searchId.equals("")) {
            bundle.putString(SEARCH_MESSAGE_ID, searchId);
        }
        chatFragment.setArguments(bundle);
        return chatFragment;
    }

    public void showTyping(WebSocketObj obj) {
        String typing = getStringTyping(obj);
        if (typing != null) {
            setupTypingText(typing);

            mBinding.getRoot().postDelayed(() -> {
                if (mapType != null && obj != null) mapType.remove(obj.getUserId());
                showTyping(null);
            }, TYPING_DURATION);
        } else {
            if (getPresenter() != null) getPresenter().showInfoDefault();
            sendUsersStatus(null);
        }
    }

    public String getStringTyping(WebSocketObj obj) {
        StringBuffer result = new StringBuffer();
        if (mChannelType != null) {
            if (mChannelType.equals("D")) {
                if (obj != null) {
                    return getString(R.string.typing);
                } else return null;
            } else {
                if (mapType == null) {
                    mapType = new HashMap<>();
                }
                if (obj != null) {
                    RealmResults<User> resultUser = UserRepository
                            .query(new UserRepository.UserByIdSpecification(obj.getUserId()));

                    if (resultUser != null && resultUser.size() == 0) {
                        MattermostService.Helper.
                                create(MattermostApp.getSingleton()).
                                startLoadUser(obj.getUserId());
                    } else {
                        mapType.put(obj.getUserId(),
                                resultUser.first()
                                        .getUsername());
                    }

                }
                int count = 0;
                if (mapType.size() == 1) {
                    for (Map.Entry<String, String> item : mapType.entrySet()) {
                        result.append(item.getValue() + " " + getString(R.string.typing));
                    }
                    return result.toString();
                }

                if (mapType.size() == 2) {
                    for (Map.Entry<String, String> item : mapType.entrySet()) {
                        count++;
                        if (count == 2)
                            result.append(item.getValue() + " " + getString(R.string.typing));
                        else
                            result.append(item.getValue() + " and ");
                    }
                    return result.toString();
                }
                if (mapType.size() > 2)
                    for (Map.Entry<String, String> item : mapType.entrySet()) {
                        count++;
                        if (count == 1)
                            result.append(item.getValue() + ", ");
                        else if (count == 2) {
                            result.append(String.format("%s and %d %s",
                                    item.getValue(),
                                    mapType.size() - 2,
                                    getString(R.string.typing)));
                            return result.toString();
                        }
                    }
            }
        }
        return null;
    }

    private void sendUsersStatus(WebSocketObj obj) {
        if (mChannelType != null) {
            if (!mChannelType.equals("D")) {
                if (obj != null)
                    mapType.remove(obj.getUserId());
            }
        } else setupTypingText("");
    }

    private void checkNeededPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_WRITE_STORAGE);
            }
        }
    }

    private void sendCommand() {
        getPresenter().requestSendCommand(new CommandToNet(this.mChannelId,
                mBinding.writingMessage.getText().toString(),
                Boolean.FALSE.toString()));
    }

    private void sendMessage() {
        Post post = new Post();
        post.setChannelId(mChannelId);
        post.setCreateAt(getTimePost());
        post.setMessage(getMessage());
        if (rootPost != null) {
            post.setRootId(rootPost.getId());
            closeEditView();
        }
        post.setUserId(MattermostPreference.getInstance().getMyUserId());
        post.setFilenames(mBinding.attachedFilesLayout.getAttachedFiles());
        post.setPendingPostId(String.format("%s:%s", post.getUserId(), post.getCreateAt()));
        String message = post.getMessage().trim();
        if (
                message.length() != 0 && FileToAttachRepository.getInstance().getFilesForAttach().isEmpty() ||
                        message.length() == 0 && !FileToAttachRepository.getInstance().getFilesForAttach().isEmpty() && !FileToAttachRepository.getInstance().haveUnloadedFiles() ||
                        message.length() != 0 && !FileToAttachRepository.getInstance().getFilesForAttach().isEmpty() && !FileToAttachRepository.getInstance().haveUnloadedFiles()
                ) {
            getPresenter().requestSendToServer(post);
            hideAttachedFilesLayout();
        } else {
            if (!FileToAttachRepository.getInstance().getFilesForAttach().isEmpty()) {
                Toast.makeText(getActivity(), getString(R.string.wait_files), Toast.LENGTH_SHORT).show();
            } else if (message.length() <= 0) {
                Toast.makeText(getActivity(), getString(R.string.message_empty), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void editMessage() {
        PostEdit postEdit = new PostEdit();
        postEdit.setId(rootPost.getId());
        postEdit.setChannelId(mChannelId);
        postEdit.setMessage(getMessage());
        closeEditView();
        if (postEdit.getMessage().length() != 0) {
            setMessage("");
            getPresenter().requestEditPost(postEdit);
        } else {
            Toast.makeText(getActivity(), getString(R.string.message_empty), Toast.LENGTH_SHORT).show();
        }
    }

    public String getMessage() {
        return mBinding.writingMessage.getText().toString();
    }

    public void setMessage(String s) {
        mBinding.writingMessage.setText(s);
    }

    private Long getTimePost() {
        Long currentTime = Calendar.getInstance().getTimeInMillis();
        if (adapter.getLastItem() == null) {
            return currentTime;
        }
        Long lastTime = ((Post) adapter.getLastItem()).getCreateAt();
        if (currentTime > lastTime)
            return currentTime;
        else
            return lastTime + 1;
    }


    private void closeEditView() {
        mBinding.editReplyMessageLayout.editableText.setText(null);
        mBinding.editReplyMessageLayout.getRoot().setVisibility(GONE);
        rootPost = null;
        mBinding.btnSend.setText(getString(R.string.send));
    }

    public void hideAttachedFilesLayout() {
        mBinding.btnSend.setTextColor(getResources().getColor(R.color.colorPrimary));
        mBinding.attachedFilesLayout.setVisibility(GONE);
    }

    public void startLoad() {
        if (mStartCode.equals(START_SEARCH)) {
            getPresenter().startRequestLoadSearch(mSearchMessageId);
        } else if (mStartCode.equals(START_NORMAL)) {
            getPresenter().startRequestLoadNormal();
        }
    }


    private void showDialog() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_buttom_sheet, null);

        final Dialog mBottomSheetDialog = new Dialog(getActivity(), R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(view);
        mBottomSheetDialog.setCancelable(true);
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        view.findViewById(R.id.layCamera).setOnClickListener(v -> {
            makePhoto();
            mBottomSheetDialog.cancel();
        });
        view.findViewById(R.id.layGallery).setOnClickListener(v -> {
            openGallery();
            mBottomSheetDialog.cancel();
        });
        view.findViewById(R.id.layFile).setOnClickListener(v -> {
            pickFile();
            mBottomSheetDialog.cancel();
        });
    }

    private void makePhoto() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_CAMERA);
            } else {
                dispatchTakePictureIntent();
            }
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            final File root = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES + "/Mattermost");
            root.mkdir();
            final String fname = "img_" + System.currentTimeMillis() + ".jpg";
            final File sdImageMainDirectory = new File(root, fname);

            fileFromCamera = Uri.fromFile(sdImageMainDirectory);
            Log.d(TAG, fileFromCamera.toString());
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileFromCamera);
            startActivityForResult(takePictureIntent, CAMERA_PIC_REQUEST);
        }
    }

    private void openGallery() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_GALLERY);
            } else {
                openFile(getActivity(), "image/*", PICK_IMAGE);
            }
        } else {
            openFile(getActivity(), "image/*", PICK_IMAGE);
        }
    }

    private void pickFile() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE_FILES);
            } else {
                openFilePicker();
            }
        } else {
            openFilePicker();
        }
    }

    private void openFilePicker() {
        openFile(getActivity(), "*/*", PICKFILE_REQUEST_CODE);
    }

    private void openFile(Context context, String minmeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(minmeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        // if you want any file type, you can skip next line
        sIntent.putExtra("CONTENT_TYPE", minmeType);
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (context.getPackageManager().resolveActivity(sIntent, 0) != null) {
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intent});
        } else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try {
            startActivityForResult(chooserIntent, requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }

    public void setVisibleProgressBar(Boolean aBoolean) {
        mBinding.progressBar.setVisibility((aBoolean) ? View.VISIBLE : GONE);
    }

    public void setRefreshing(boolean b) {
        mBinding.swipeRefreshLayout.setRefreshing(b);
        //enableAllPagination();
    }

    public void enableAllPagination() {
        mBinding.rev.setCanPagination(true);
        mBinding.rev.setCanPaginationTop(true);
        mBinding.rev.setCanPaginationBot(true);
        disableShowLoadMoreTop();
        disableShowLoadMoreBot();
    }

    public void enableTopPagination() {
        mBinding.rev.setCanPagination(true);
        mBinding.rev.setCanPaginationTop(true);
        mBinding.rev.setCanPaginationBot(false);
        disableShowLoadMoreTop();
        disableShowLoadMoreBot();
    }

    public void enableBotPagination() {
        mBinding.rev.setCanPagination(true);
        mBinding.rev.setCanPaginationTop(false);
        mBinding.rev.setCanPaginationBot(true);
        disableShowLoadMoreTop();
        disableShowLoadMoreBot();
    }

    public void disablePagination() {
        mBinding.rev.setCanPagination(false);
        mBinding.rev.setCanPaginationTop(false);
        mBinding.rev.setCanPaginationBot(false);
        disableShowLoadMoreTop();
        disableShowLoadMoreBot();
    }

    public void slideToMessageById() {
        if (adapter != null) {
            adapter.setmHighlightedPostId(mSearchMessageId);
            positionItemMessage = adapter.getPositionById(mSearchMessageId);
        }

        isFocus = false;
        try {
            mBinding.rev.scrollToPosition(positionItemMessage);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "slideToMessageById() called");
        }
        mBinding.rev.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int pFirst = layoutManager.findFirstVisibleItemPosition();
                int pLast = layoutManager.findLastVisibleItemPosition();

                if (adapter != null && adapter.getmHighlightedPostId() != null && pFirst < positionItemMessage && positionItemMessage < pLast && !isFocus) {
                    isFocus = true;
                }

                if (isFocus && pFirst > positionItemMessage || positionItemMessage > pLast) {
                    if (adapter != null) adapter.setmHighlightedPostId(null);
                    adapter.notifyDataSetChanged();
                    isFocus = false;
                    mBinding.rev.removeOnScrollListener(this);
                }
            }
        });
    }

    public void showList() {
        mBinding.rev.setVisibility(View.VISIBLE);
        mBinding.emptyList.setVisibility(View.GONE);
        mBinding.newMessageLayout.setVisibility(View.VISIBLE);
    }

    public void setMessageLayout(int visible) {
//        binding.bottomToolbar.bottomToolbarLayout.setVisibility(visible);
        mBinding.sendingMessageContainer.setVisibility(visible);
        mBinding.line.setVisibility(visible);
    }

    public void enablePaginationTopAndBot() {
        mBinding.rev.setCanPaginationTop(true);
        mBinding.rev.setCanPaginationBot(true);
        mBinding.rev.setCanPagination(true);
    }

    public void setStateFragment(StateFragment stateFragment) {
        this.mState = stateFragment;
    }

    public void showErrorLayout() {
        Toast.makeText(getActivity(), "ErrorLayout", Toast.LENGTH_SHORT).show();
    }

    public void setCanPaginationTop(Boolean aBoolean) {
        mBinding.rev.setCanPaginationTop(aBoolean);
        disableShowLoadMoreTop();
    }

    public void setCanPaginationBot(Boolean aBoolean) {
        mBinding.rev.setCanPaginationBot(aBoolean);
        disableShowLoadMoreBot();
    }

    public void disableShowLoadMoreTop() {
        Log.d(TAG, "disableShowLoadMoreTop()");
        mBinding.rev.disableShowLoadMoreTop();
    }

    public void disableShowLoadMoreBot() {
        Log.d(TAG, "disableShowLoadMoreBot()");
        mBinding.rev.disableShowLoadMoreBot();
    }

    public void showEmptyList(String channelId) {
        mBinding.progressBar.setVisibility(GONE);
        try {
            Channel channel = ChannelRepository.query(
                    new ChannelRepository.ChannelByIdSpecification(channelId)).last();

            User user = Realm.getDefaultInstance().where(User.class)
                    .equalTo("id", ChatUtils.getDirectUserId(mChannelId)).findFirst();

            String createAtDate = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH)
                    .format(new Date(channel.getCreateAt()));

            mBinding.emptyListTitle.setVisibility(View.VISIBLE);
            mBinding.emptyListMessage.setVisibility(View.VISIBLE);
            if (channel.getType().equals(Channel.DIRECT)) {
                mBinding.emptyListTitle.setText(user.getUsername());
                mBinding.emptyListMessage.setText(String.format(
                        getResources().getString(R.string.empty_dialog_direct_message), user.getUsername()));
            } else {
                mBinding.emptyListTitle.setText(String.format(
                        getResources().getString(R.string.empty_dialog_title), channel.getDisplayName()));

                String emptyListMessage = String.format(
                        getResources().getString(R.string.empty_dialog_beginning_message),
                        channel.getDisplayName(), createAtDate);

                if (channel.getType().equals(Channel.OPEN)) {
                    mBinding.emptyListMessage.setText(new StringBuilder(emptyListMessage
                            + " " + getResources().getString(R.string.empty_dialog_group_message)));
                } else {
                    mBinding.emptyListMessage.setText(new StringBuilder(emptyListMessage
                            + " " + getResources().getString(R.string.empty_dialog_private_message)));
                }
                mBinding.emptyListInviteOthers.setText(getResources().getString(R.string.empty_dialog_invite));
                mBinding.emptyListInviteOthers.setOnClickListener(
//                        v -> AddMembersActivity.start(getActivity(), channel.getId()));
                        v -> startAddMembersActivity());
                mBinding.emptyListInviteOthers.setVisibility(View.VISIBLE);
            }
            mBinding.emptyList.setVisibility(View.VISIBLE);
            mBinding.newMessageLayout.setVisibility(View.VISIBLE);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "showEmptyList: ", e);
        }
    }

    private void startChannelActivity(){
        Intent starter = new Intent(getActivity(), ChannelActivity.class);
        starter.putExtra(CHANNEL_ID, mChannelId);
        startActivityForResult(starter, REQUEST_ID);
    }

    private void startAddMembersActivity(){
        Intent starter = new Intent(getActivity(), AddMembersActivity.class);
        starter.putExtra(CHANNEL_ID, mChannelId);
        startActivityForResult(starter, ADD_MEMBER_CODE);
    }

    public void invalidateAdapter() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void showErrorSendMenu(View view, Post post) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), view, Gravity.BOTTOM);
        popupMenu.inflate(R.menu.error_send_item_popupmenu);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.try_again:
                    Post p = new Post(post);
                    getPresenter().requestSendToServerError(p);
                    break;
                case R.id.delete:
                    PostRepository.remove(post);
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    private void showPopupMenu(View view, Post post) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), view, Gravity.BOTTOM);

        if (post.getUserId().equals(MattermostPreference.getInstance().getMyUserId())) {
            popupMenu.inflate(R.menu.my_chat_item_popupmenu);
        } else {
            popupMenu.inflate(R.menu.foreign_chat_item_popupmenu);
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            EditDialogLayoutBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(),
                    R.layout.edit_dialog_layout, null, false);

            switch (menuItem.getItemId()) {
                case R.id.edit:
                    rootPost = post;
                    showEditView(Html.fromHtml(post.getMessage()).toString(), EDIT_MESSAGE);
                    break;
                case R.id.delete:
                    new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                            .setTitle(getString(R.string.confirm_post_delete))
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                            .setPositiveButton(R.string.delete, (dialogInterface, i) -> getPresenter().requestDeletePost(post))
                            .show();
                    break;
                case R.id.permalink:
                    binding.edit.setText(getMessageLink(post.getId()));
                    new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                            .setTitle(getString(R.string.copy_permalink))
                            .setView(binding.getRoot())
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                            .setPositiveButton(R.string.copy_link, (dialogInterface1, i1) -> copyLink(binding.edit.getText().toString()))
                            .show();
                    break;
                case R.id.copy:
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(PostViewHolder.getMarkdownPost(post.getMessage(), getActivity()));
                    Toast.makeText(getActivity(), "Сopied to the clipboard", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.reply:
                    rootPost = post;
                    showReplayView(Html.fromHtml(post.getMessage()).toString(), REPLY_MESSAGE);
                    break;
            }
            return true;
        });
        popupMenu.show();
    }

    private void showEditView(String message, String type) {
        showView(message, type);
        mBinding.editReplyMessageLayout.close.setOnClickListener(view -> {
            mBinding.writingMessage.setText(null);
            closeEditView();
        });
        mBinding.writingMessage.setText(rootPost.getMessage());
        mBinding.writingMessage.setSelection(rootPost.getMessage().length());
    }

    private void showReplayView(String message, String type) {
        showView(message, type);
        mBinding.editReplyMessageLayout.close.setOnClickListener(view -> closeEditView());
    }

    private void showView(String message, String type) {
        Animation upAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.edit_card_up);

        if (type.equals(REPLY_MESSAGE))
            mBinding.editReplyMessageLayout.title.setText(getResources().getString(R.string.reply_message));
        else {
            mBinding.editReplyMessageLayout.title.setText(getResources().getString(R.string.edit_message));
            mBinding.btnSend.setText(R.string.save);
        }

        mBinding.editReplyMessageLayout.editableText.setText(message);
        mBinding.editReplyMessageLayout.root.startAnimation(upAnim);
        //binding.editMessageLayout.card.startAnimation(fallingAnimation);
        mBinding.editReplyMessageLayout.getRoot().setVisibility(View.VISIBLE);
    }

    private String getMessageLink(String postId) {
        Realm realm = Realm.getDefaultInstance();
        return "https://"
                + MattermostPreference.getInstance().getBaseUrl()
                + "/"
                + realm.where(Team.class).findFirst().getName()
                + "/pl/"
                + postId;
    }

    public void copyLink(String link) {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(link);
        Toast.makeText(getActivity(), "link copied", Toast.LENGTH_SHORT).show();
    }

    public enum StateFragment {
        STATE_NORMAL_LOADING,
        STATE_SEARCH_LOADING,
        STATE_NORMAL,
        STATE_DEFAULT
    }

}